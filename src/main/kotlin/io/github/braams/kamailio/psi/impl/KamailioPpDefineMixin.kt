package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import io.github.braams.kamailio.psi.KamailioNamedElement
import io.github.braams.kamailio.psi.KamailioPpValue
import io.github.braams.kamailio.psi.KamailioTypes

open class KamailioPpDefineMixin(node: ASTNode) : KamailioPsiElementBase(node), KamailioNamedElement {

    /** "#!define", "#!trydef", "#!redef", ... */
    val directiveText: String
        get() = node.firstChildNode?.text ?: ""

    val valueText: String?
        get() = PsiTreeUtil.getChildOfType(this, KamailioPpValue::class.java)?.text

    override fun getNameIdentifier(): PsiElement? = node.findChildByType(KamailioTypes.IDENT)?.psi

    override fun getName(): String? = nameIdentifier?.text

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("Renaming defines is not supported yet")

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
