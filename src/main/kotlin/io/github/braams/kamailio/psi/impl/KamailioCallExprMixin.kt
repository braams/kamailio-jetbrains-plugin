package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioIdentifier

open class KamailioCallExprMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    val functionNameElement: PsiElement?
        get() = PsiTreeUtil.getChildOfType(this, KamailioIdentifier::class.java)

    val functionName: String?
        get() = functionNameElement?.text
}
