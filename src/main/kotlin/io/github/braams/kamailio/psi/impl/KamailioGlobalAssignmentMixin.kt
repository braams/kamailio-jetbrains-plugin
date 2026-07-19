package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioParamName

open class KamailioGlobalAssignmentMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    val paramNameElement: PsiElement?
        get() = PsiTreeUtil.getChildOfType(this, KamailioParamName::class.java)

    val paramNameText: String?
        get() = paramNameElement?.text
}
