package io.github.braams.kamailio.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin

/** Soft reference from a bare identifier to a matching #!define / #!trydef; unresolved is not an error. */
class KamailioDefineReference(
    element: PsiElement,
    range: TextRange,
    private val constName: String
) : PsiReferenceBase<PsiElement>(element, range, true) {

    override fun resolve(): PsiElement? =
        KamailioIncludeGraph.defines(element.containingFile)
            .firstOrNull { it is KamailioPpDefineMixin && it.name == constName }

    override fun getVariants(): Array<Any> =
        KamailioIncludeGraph.defines(element.containingFile)
            .mapNotNull { (it as KamailioPpDefineMixin).name }
            .distinct()
            .toTypedArray()
}
