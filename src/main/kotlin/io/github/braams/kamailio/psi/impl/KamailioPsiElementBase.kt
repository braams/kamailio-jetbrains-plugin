package io.github.braams.kamailio.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry

/**
 * Base class of all generated PSI elements. Overrides [getReferences] to pick up references
 * contributed via PsiReferenceContributor — ASTWrapperPsiElement does not do that by itself.
 */
open class KamailioPsiElementBase(node: ASTNode) : ASTWrapperPsiElement(node) {
    override fun getReferences(): Array<PsiReference> =
        ReferenceProvidersRegistry.getReferencesFromProviders(this)
}
