package io.github.braams.kamailio.doc

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.model.Pointer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.tree.TokenSet
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.impl.KamailioCallExprMixin
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.psi.impl.KamailioGlobalAssignmentMixin
import io.github.braams.kamailio.psi.impl.KamailioLoadmoduleMixin
import io.github.braams.kamailio.psi.impl.KamailioModparamMixin
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin
import io.github.braams.kamailio.psi.impl.KamailioPvMixin
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin
import io.github.braams.kamailio.psi.impl.KamailioTransformationMixin
import io.github.braams.kamailio.resolve.KamailioDefineReference
import io.github.braams.kamailio.resolve.KamailioRouteReference

class KamailioDocumentationTargetProvider : DocumentationTargetProvider {

    private companion object {
        // tokens whose text can name a core keyword entry (myself, src_ip, request_route, ...)
        val KEYWORD_TOKENS = TokenSet.create(
            KamailioTypes.IDENT,
            KamailioTypes.ROUTE_KW,
            KamailioTypes.REQUEST_ROUTE_KW,
            KamailioTypes.REPLY_ROUTE_KW,
            KamailioTypes.ONREPLY_ROUTE_KW,
            KamailioTypes.FAILURE_ROUTE_KW,
            KamailioTypes.BRANCH_ROUTE_KW,
            KamailioTypes.ONSEND_ROUTE_KW,
            KamailioTypes.EVENT_ROUTE_KW
        )
    }

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        if (file !is KamailioFile) return emptyList()
        val leaf = file.findElementAt(offset) ?: return emptyList()

        fromStringInterp(leaf, offset)?.let { return listOf(it) }
        fromDocDb(leaf, offset)?.let { return listOf(it) }
        fromLocalDefinition(leaf, offset)?.let { return listOf(it) }
        fromKeyword(leaf)?.let { return listOf(it) }
        return emptyList()
    }

    /** Category+name lookup in the docSource EP / bundled JSON. */
    private fun fromDocDb(leaf: PsiElement, offset: Int): DocumentationTarget? {
        var el: PsiElement? = leaf
        while (el != null && el !is PsiFile) {
            when (el) {
                is KamailioTransformationMixin ->
                    if (contains(el.transNameElement, leaf)) {
                        val name = el.transformationName ?: return null
                        return target(leaf, KamailioDocCategory.TRANSFORMATION, name, null)
                    }
                is KamailioPvMixin ->
                    if (contains(el.pvNameElement, leaf)) {
                        val name = el.pvName ?: return null
                        return target(leaf, KamailioDocCategory.PSEUDOVAR, name, null)
                    }
                is KamailioCallExprMixin ->
                    if (contains(el.functionNameElement, leaf)) {
                        val name = el.functionName ?: return null
                        return target(leaf, KamailioDocCategory.FUNCTION, name, null)
                    }
                is KamailioModparamMixin ->
                    if (contains(el.paramNameElement, leaf)) {
                        val name = el.paramName ?: return null
                        return target(leaf, KamailioDocCategory.MODPARAM, name, el.moduleNames.firstOrNull())
                    } else if (contains(el.moduleNameElement, leaf)) {
                        val name = moduleNameAt(el, offset) ?: return null
                        return target(leaf, KamailioDocCategory.MODULE, name, null)
                    }
                is KamailioGlobalAssignmentMixin ->
                    if (contains(el.paramNameElement, leaf)) {
                        val name = el.paramNameText ?: return null
                        return target(leaf, KamailioDocCategory.GLOBAL_PARAM, name, null)
                    }
                is KamailioLoadmoduleMixin ->
                    if (contains(el.modulePathElement, leaf)) {
                        val name = el.moduleName ?: return null
                        return target(leaf, KamailioDocCategory.MODULE, name, null)
                    }
                else -> {}
            }
            el = el.parent
        }
        return null
    }

    /** `modparam("nathelper|registrar", ...)` — pick the `|`-separated segment the caret is on. */
    private fun moduleNameAt(el: KamailioModparamMixin, offset: Int): String? {
        val names = el.moduleNames
        if (names.size <= 1) return names.firstOrNull()
        val strEl = el.moduleNameElement ?: return null
        val rel = offset - strEl.textRange.startOffset
        var start = 0
        for (seg in strEl.text.split('|')) {
            val end = start + seg.length
            if (rel in start..end) return seg.trim('"', ' ').ifEmpty { null }
            start = end + 1
        }
        return names.firstOrNull()
    }

    private fun target(anchor: PsiElement, category: KamailioDocCategory, name: String, module: String?): DocumentationTarget? {
        val entry = KamailioDocService.lookup(category, name, module) ?: return null
        return KamailioDocTarget(anchor, entryHtml(anchor, entry), entry.name)
    }

    /**
     * A pseudo-variable inside a double-quoted string is one flat STRING_INTERP token
     * (`$fu`, `$var(x)`, `$(ru{s.len})`) with no pv PSI inside — parse the name out of the token text.
     */
    private fun fromStringInterp(leaf: PsiElement, offset: Int): DocumentationTarget? {
        if (leaf.node.elementType != KamailioTypes.STRING_INTERP) return null
        val text = leaf.text
        val rel = offset - leaf.textRange.startOffset
        // caret inside a {transformation} of the $(name{...}) form shows the transformation doc
        var open = text.indexOf('{')
        while (open in 0 until rel) {
            val close = text.indexOf('}', open).let { if (it == -1) text.length else it }
            if (rel <= close) {
                val trans = text.substring(open + 1, close).substringBefore(',').trim()
                target(leaf, KamailioDocCategory.TRANSFORMATION, trans, null)?.let { return it }
                break
            }
            open = text.indexOf('{', close)
        }
        val name = text.removePrefix("$").removePrefix("(").takeWhile { it.isLetterOrDigit() || it == '_' }
        if (name.isEmpty()) return null
        return target(leaf, KamailioDocCategory.PSEUDOVAR, name, null)
    }

    /** Core keywords (`myself`, `src_ip`, `request_route`, ...) are plain tokens, not PSI structure. */
    private fun fromKeyword(leaf: PsiElement): DocumentationTarget? {
        if (leaf.node.elementType !in KEYWORD_TOKENS) return null
        return target(leaf, KamailioDocCategory.KEYWORD, leaf.text, null)
    }

    /** Hover on route(X) / define usages shows the local definition. */
    private fun fromLocalDefinition(leaf: PsiElement, offset: Int): DocumentationTarget? {
        var el: PsiElement? = leaf
        while (el != null && el !is PsiFile) {
            for (ref in el.references) {
                val absRange = ref.rangeInElement.shiftRight(el.textRange.startOffset)
                if (!absRange.containsOffset(offset)) continue
                when (ref) {
                    is KamailioRouteReference -> {
                        val def = ref.multiResolve(false).firstOrNull()?.element as? KamailioRouteDefMixin ?: continue
                        return KamailioDocTarget(el, routeHtml(def), def.name ?: "route")
                    }
                    is KamailioDefineReference -> {
                        val def = ref.resolve() as? KamailioPpDefineMixin ?: continue
                        return KamailioDocTarget(el, defineHtml(def), def.name ?: "define")
                    }
                }
            }
            el = el.parent
        }
        // hover directly on a definition
        val routeDef = leaf.parentOfTypeSelf<KamailioRouteDefMixin>()
        if (routeDef != null && contains(routeDef.nameIdentifier, leaf)) {
            return KamailioDocTarget(leaf, routeHtml(routeDef), routeDef.name ?: "route")
        }
        val defineDef = leaf.parentOfTypeSelf<KamailioPpDefineMixin>()
        if (defineDef != null && contains(defineDef.nameIdentifier, leaf)) {
            return KamailioDocTarget(leaf, defineHtml(defineDef), defineDef.name ?: "define")
        }
        return null
    }

    private inline fun <reified T : PsiElement> PsiElement.parentOfTypeSelf(): T? {
        var el: PsiElement? = this
        while (el != null && el !is PsiFile) {
            if (el is T) return el
            el = el.parent
        }
        return null
    }

    private fun contains(container: PsiElement?, leaf: PsiElement): Boolean =
        container != null && container.textRange.contains(leaf.textRange)

    private fun entryHtml(anchor: PsiElement, entry: DocEntry): String {
        val body = if (entry.markdown) DocMarkdownToHtmlConverter.convert(anchor.project, entry.doc) else entry.doc
        val sb = StringBuilder()
        sb.append("<div class='definition'><pre>")
        sb.append(StringUtil.escapeXmlEntities(entry.syntax ?: entry.name))
        sb.append("</pre></div>")
        sb.append("<div class='content'>").append(body).append("</div>")
        val kind = entry.category.name.lowercase().replace('_', ' ')
        val origin = if (entry.module != null) "$kind, module ${entry.module}" else kind
        sb.append("<div class='bottom'><i>").append(StringUtil.escapeXmlEntities(origin)).append("</i></div>")
        return sb.toString()
    }

    private fun routeHtml(def: KamailioRouteDefMixin): String {
        val kind = def.kind?.keyword ?: "route"
        val name = def.nameIdentifier?.text
        val header = if (name != null) "$kind[$name]" else kind
        return "<div class='definition'><pre>${StringUtil.escapeXmlEntities(header)}</pre></div>" +
            "<div class='content'>Defined in ${StringUtil.escapeXmlEntities(def.containingFile.name)}</div>"
    }

    private fun defineHtml(def: KamailioPpDefineMixin): String {
        val text = def.text.lineSequence().first().trim()
        return "<div class='definition'><pre>${StringUtil.escapeXmlEntities(text)}</pre></div>" +
            "<div class='content'>Defined in ${StringUtil.escapeXmlEntities(def.containingFile.name)}</div>"
    }

    private class KamailioDocTarget(
        anchor: PsiElement,
        private val html: String,
        private val title: String
    ) : DocumentationTarget {

        private val pointer = SmartPointerManager.createPointer(anchor)

        override fun computePresentation(): TargetPresentation =
            TargetPresentation.builder(title).presentation()

        override fun createPointer(): Pointer<out DocumentationTarget> {
            val h = html
            val t = title
            return Pointer {
                pointer.element?.let { KamailioDocTarget(it, h, t) }
            }
        }

        override fun computeDocumentation(): DocumentationResult = DocumentationResult.documentation(html)
    }
}
