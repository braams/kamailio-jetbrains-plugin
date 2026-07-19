package io.github.braams.kamailio.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.doc.DocEntry
import io.github.braams.kamailio.doc.KamailioDocCategory
import io.github.braams.kamailio.doc.KamailioDocService
import io.github.braams.kamailio.psi.KamailioBlock
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioModparamValue
import io.github.braams.kamailio.psi.KamailioPpDefine
import io.github.braams.kamailio.psi.KamailioPvKey
import io.github.braams.kamailio.psi.KamailioPvPlain
import io.github.braams.kamailio.psi.KamailioRouteName
import io.github.braams.kamailio.psi.KamailioRouteRef
import io.github.braams.kamailio.psi.KamailioTransName
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.psi.impl.KamailioLoadmoduleMixin
import io.github.braams.kamailio.psi.impl.KamailioModparamMixin

/**
 * Completion from the documentation database: global parameters, functions, module names,
 * modparam parameter names, pseudo-variables (also inside double-quoted strings) and transformations.
 * Route names and #!define constants are completed by the references' variants, not here.
 */
class KamailioCompletionContributor : CompletionContributor() {

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if (context.file is KamailioFile) {
            context.dummyIdentifier = CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
        }
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        if (position.containingFile !is KamailioFile) return

        // comments never match: the position leaf there is a comment token, not IDENT/string
        when (position.node.elementType) {
            KamailioTypes.STRING_TEXT, KamailioTypes.STRING_INTERP ->
                inString(position, parameters, result)
            KamailioTypes.IDENT, KamailioTypes.DESC_KW ->
                inCode(position, parameters, result)
        }
    }

    private fun inString(position: PsiElement, parameters: CompletionParameters, result: CompletionResultSet) {
        // "$fu" inside a double-quoted string is one flat STRING_INTERP token
        if (position.node.elementType == KamailioTypes.STRING_INTERP) {
            val text = position.text
            val rel = (parameters.offset - position.textRange.startOffset).coerceIn(0, text.length)
            val nameStart = text.take(rel).indexOfLast { it == '$' || it == '(' || it == '{' } + 1
            val prefixed = result.withPrefixMatcher(text.substring(nameStart, rel))
            for (e in KamailioDocService.entries(KamailioDocCategory.PSEUDOVAR)) {
                prefixed.addElement(pvElement(e))
            }
            return
        }

        val loadmodule = PsiTreeUtil.getParentOfType(position, KamailioLoadmoduleMixin::class.java)
        if (loadmodule != null) {
            for (e in KamailioDocService.entries(KamailioDocCategory.MODULE)) {
                result.addElement(
                    LookupElementBuilder.create("${e.name}.so")
                        .withIcon(AllIcons.Nodes.Plugin)
                        .withTypeText("module")
                )
            }
            return
        }

        val modparam = PsiTreeUtil.getParentOfType(position, KamailioModparamMixin::class.java) ?: return
        when {
            contains(modparam.moduleNameElement, position) ->
                for (e in KamailioDocService.entries(KamailioDocCategory.MODULE)) {
                    result.addElement(
                        LookupElementBuilder.create(e.name)
                            .withIcon(AllIcons.Nodes.Plugin)
                            .withTypeText("module")
                    )
                }
            contains(modparam.paramNameElement, position) -> {
                val modules = modparam.moduleNames.toSet()
                for (e in KamailioDocService.entries(KamailioDocCategory.MODPARAM)) {
                    if (modules.isNotEmpty() && e.module !in modules) continue
                    result.addElement(
                        LookupElementBuilder.create(e.name)
                            .withIcon(AllIcons.Nodes.Property)
                            .withTypeText(e.module ?: "modparam")
                    )
                }
            }
        }
    }

    private fun inCode(position: PsiElement, parameters: CompletionParameters, result: CompletionResultSet) {
        // route(X), #!ifdef X, modparam value: completed by reference variants; $var(x) keys: nothing to offer
        if (hasParent<KamailioRouteRef>(position) || hasParent<KamailioRouteName>(position) ||
            hasParent<KamailioPpDefine>(position) || hasParent<KamailioPvKey>(position) ||
            hasParent<KamailioModparamValue>(position)
        ) return

        val transName = PsiTreeUtil.getParentOfType(position, KamailioTransName::class.java)
        if (transName != null) {
            // dotted names ("s.len"): match against the whole trans_name typed so far
            val prefix = transName.text
                .substring(0, (parameters.offset - transName.textRange.startOffset).coerceAtLeast(0))
            val prefixed = result.withPrefixMatcher(prefix)
            for (e in KamailioDocService.entries(KamailioDocCategory.TRANSFORMATION)) {
                prefixed.addElement(
                    LookupElementBuilder.create(e.name)
                        .withIcon(AllIcons.Nodes.Lambda)
                        .withTypeText("transformation")
                )
            }
            return
        }

        if (hasParent<KamailioPvPlain>(position)) {
            for (e in KamailioDocService.entries(KamailioDocCategory.PSEUDOVAR)) {
                result.addElement(pvElement(e))
            }
            return
        }

        if (PsiTreeUtil.getParentOfType(position, KamailioBlock::class.java) != null) {
            for (e in KamailioDocService.entries(KamailioDocCategory.FUNCTION)) {
                val noArgs = e.syntax?.contains("()") == true
                result.addElement(
                    LookupElementBuilder.create(e.name)
                        .withIcon(AllIcons.Nodes.Function)
                        .withTailText(if (noArgs) "()" else "(...)", true)
                        .withTypeText(e.module ?: "core")
                        .withInsertHandler(ParenthesesInsertHandler.getInstance(!noArgs))
                )
            }
            for (e in KamailioDocService.entries(KamailioDocCategory.KEYWORD)) {
                result.addElement(
                    LookupElementBuilder.create(e.name).withTypeText("keyword")
                )
            }
        } else {
            for (e in KamailioDocService.entries(KamailioDocCategory.GLOBAL_PARAM)) {
                result.addElement(
                    LookupElementBuilder.create(e.name)
                        .withIcon(AllIcons.Nodes.Property)
                        .withTypeText("core parameter")
                        .withInsertHandler(ASSIGN_INSERT)
                )
            }
            for (kw in TOP_LEVEL_KEYWORDS) {
                result.addElement(LookupElementBuilder.create(kw))
            }
        }
    }

    private fun pvElement(e: DocEntry): LookupElement =
        LookupElementBuilder.create(e.name)
            .withIcon(AllIcons.Nodes.Variable)
            .withTypeText(e.module ?: "core")

    private inline fun <reified T : PsiElement> hasParent(el: PsiElement): Boolean =
        PsiTreeUtil.getParentOfType(el, T::class.java) != null

    private fun contains(container: PsiElement?, el: PsiElement): Boolean =
        container != null && container.textRange.contains(el.textRange)

    private companion object {
        val TOP_LEVEL_KEYWORDS = listOf(
            "loadmodule", "loadpath", "modparam", "include_file", "import_file",
            "request_route", "route", "reply_route", "onreply_route", "failure_route",
            "branch_route", "onsend_route", "event_route"
        )

        /** `debug` -> `debug=` with the caret after `=` (stock config style is tight). */
        val ASSIGN_INSERT = InsertHandler<LookupElement> { context, _ ->
            val at = context.tailOffset
            val doc = context.document
            if (at >= doc.textLength || doc.charsSequence[at] != '=') {
                doc.insertString(at, "=")
            }
            context.editor.caretModel.moveToOffset(at + 1)
        }
    }
}
