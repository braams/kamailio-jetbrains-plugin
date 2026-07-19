package io.github.braams.kamailio.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import io.github.braams.kamailio.psi.KamailioArgList
import io.github.braams.kamailio.psi.KamailioCaseValue
import io.github.braams.kamailio.psi.KamailioGlobalValue
import io.github.braams.kamailio.psi.KamailioIdentifierRef
import io.github.braams.kamailio.psi.KamailioIncludeDirective
import io.github.braams.kamailio.psi.KamailioModparamValue
import io.github.braams.kamailio.psi.KamailioPpIfdef
import io.github.braams.kamailio.psi.KamailioPpIfndef
import io.github.braams.kamailio.psi.KamailioPpUndef
import io.github.braams.kamailio.psi.KamailioPpValue
import io.github.braams.kamailio.psi.KamailioPsiUtil
import io.github.braams.kamailio.psi.KamailioRouteRef
import io.github.braams.kamailio.psi.KamailioStringLiteral
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.psi.RouteKind
import io.github.braams.kamailio.psi.impl.KamailioCallExprMixin

class KamailioReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(psiElement(KamailioRouteRef::class.java), RouteRefProvider)
        registrar.registerReferenceProvider(psiElement(KamailioStringLiteral::class.java), StringLiteralProvider)

        val defineHosts = listOf(
            KamailioIdentifierRef::class.java, KamailioModparamValue::class.java, KamailioGlobalValue::class.java,
            KamailioCaseValue::class.java, KamailioPpValue::class.java,
            KamailioPpIfdef::class.java, KamailioPpIfndef::class.java, KamailioPpUndef::class.java
        )
        for (host in defineHosts) {
            registrar.registerReferenceProvider(psiElement(host), DefineRefProvider)
        }
    }

    /** route(NAME) -> route[NAME] */
    private object RouteRefProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val leaf = element.node.findChildByType(KamailioTypes.IDENT)
                ?: element.node.findChildByType(KamailioTypes.NUMBER)
                ?: return PsiReference.EMPTY_ARRAY
            val range = TextRange.from(leaf.startOffset - element.textRange.startOffset, leaf.textLength)
            return arrayOf(KamailioRouteReference(element, range, leaf.text, setOf(RouteKind.ROUTE)))
        }
    }

    /** t_on_branch("X") -> branch_route[X], include_file "path" -> file */
    private object StringLiteralProvider : PsiReferenceProvider() {
        private val T_ON_KINDS = mapOf(
            "t_on_branch" to RouteKind.BRANCH,
            "t_on_reply" to RouteKind.ONREPLY,
            "t_on_failure" to RouteKind.FAILURE,
            "t_on_branch_failure" to RouteKind.EVENT
        )

        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val parent = element.parent ?: return PsiReference.EMPTY_ARRAY

            if (parent is KamailioIncludeDirective) {
                val content = KamailioPsiUtil.stringLiteralContent(element)
                if (content.isEmpty()) return PsiReference.EMPTY_ARRAY
                return arrayOf(*FileReferenceSet(content, element, 1, this, true).allReferences)
            }

            val argList = PsiTreeUtil.getParentOfType(element, KamailioArgList::class.java)
            if (argList != null) {
                val call = argList.parent as? KamailioCallExprMixin ?: return PsiReference.EMPTY_ARRAY
                val kind = T_ON_KINDS[call.functionName] ?: return PsiReference.EMPTY_ARRAY
                val content = KamailioPsiUtil.stringLiteralContent(element)
                if (content.isEmpty()) return PsiReference.EMPTY_ARRAY
                val range = TextRange.from(1, content.length)
                return arrayOf(KamailioRouteReference(element, range, content, setOf(kind)))
            }

            return PsiReference.EMPTY_ARRAY
        }
    }

    /** Bare identifiers (FLT_ACC, DBURL, WITH_NAT ...) -> #!define */
    private object DefineRefProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val base = element.textRange.startOffset
            val refs = mutableListOf<PsiReference>()
            PsiTreeUtil.processElements(element) { el ->
                if (el.node.elementType == KamailioTypes.IDENT && el.firstChild == null) {
                    val range = TextRange.from(el.textRange.startOffset - base, el.textLength)
                    refs += KamailioDefineReference(element, range, el.text)
                }
                true
            }
            return refs.toTypedArray()
        }
    }
}
