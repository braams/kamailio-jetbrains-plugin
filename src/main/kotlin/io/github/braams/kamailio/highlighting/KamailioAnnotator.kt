package io.github.braams.kamailio.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import io.github.braams.kamailio.psi.KamailioPpIfdef
import io.github.braams.kamailio.psi.KamailioPpIfndef
import io.github.braams.kamailio.psi.KamailioRouteName
import io.github.braams.kamailio.psi.KamailioRouteRef
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.resolve.KamailioDefineReference
import io.github.braams.kamailio.psi.impl.KamailioCallExprMixin
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin
import io.github.braams.kamailio.psi.impl.KamailioPvMixin

class KamailioAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            element is KamailioRouteName || element is KamailioRouteRef ->
                color(holder, element, KamailioColors.ROUTE_NAME)

            element is KamailioCallExprMixin ->
                element.functionNameElement?.let { color(holder, it, KamailioColors.FUNCTION_CALL) }

            element is KamailioPvMixin ->
                element.pvNameElement?.let { color(holder, it, KamailioColors.PSEUDO_VAR) }

            element is KamailioPpDefineMixin ->
                element.nameIdentifier?.let { color(holder, it, KamailioColors.CONSTANT) }

            element is KamailioPpIfdef || element is KamailioPpIfndef ->
                element.node.findChildByType(KamailioTypes.IDENT)?.let {
                    color(holder, it.psi, KamailioColors.CONSTANT)
                }

            else ->
                // usages of #!define constants (FLT_ACC in setflag(...), DBURL in modparam(...), ...)
                for (ref in element.references) {
                    if (ref is KamailioDefineReference && ref.resolve() != null) {
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(ref.rangeInElement.shiftRight(element.textRange.startOffset))
                            .textAttributes(KamailioColors.CONSTANT)
                            .create()
                    }
                }
        }
    }

    private fun color(holder: AnnotationHolder, target: PsiElement, key: com.intellij.openapi.editor.colors.TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(target.textRange)
            .textAttributes(key)
            .create()
    }
}
