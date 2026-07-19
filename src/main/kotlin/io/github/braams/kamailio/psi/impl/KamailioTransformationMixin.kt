package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioTransName

open class KamailioTransformationMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    val transNameElement: PsiElement?
        get() = PsiTreeUtil.getChildOfType(this, KamailioTransName::class.java)

    /** Full dotted name, e.g. "s.len" */
    val transformationName: String?
        get() = transNameElement?.text
}
