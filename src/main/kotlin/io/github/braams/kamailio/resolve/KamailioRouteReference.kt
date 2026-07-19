package io.github.braams.kamailio.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import io.github.braams.kamailio.psi.RouteKind
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin

class KamailioRouteReference(
    element: PsiElement,
    range: TextRange,
    private val routeName: String,
    private val kinds: Set<RouteKind>
) : PsiPolyVariantReferenceBase<PsiElement>(element, range) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        KamailioIncludeGraph.routeDefs(element.containingFile)
            .filter { def ->
                def is KamailioRouteDefMixin &&
                    def.kind in kinds &&
                    def.nameIdentifier?.text == routeName
            }
            .map { PsiElementResolveResult(it) }
            .toTypedArray()

    override fun getVariants(): Array<Any> =
        KamailioIncludeGraph.routeDefs(element.containingFile)
            .filter { it is KamailioRouteDefMixin && it.kind in kinds }
            .mapNotNull { it.nameIdentifier?.text }
            .distinct()
            .toTypedArray()
}
