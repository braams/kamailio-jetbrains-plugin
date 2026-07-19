package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import io.github.braams.kamailio.psi.KamailioNamedElement
import io.github.braams.kamailio.psi.KamailioRouteKind
import io.github.braams.kamailio.psi.KamailioRouteNameDecl
import io.github.braams.kamailio.psi.RouteKind

open class KamailioRouteDefMixin(node: ASTNode) : KamailioPsiElementBase(node), KamailioNamedElement {

    val kind: RouteKind?
        get() {
            val kindElement = PsiTreeUtil.getChildOfType(this, KamailioRouteKind::class.java) ?: return null
            return RouteKind.fromToken(kindElement.node.firstChildNode?.elementType)
        }

    override fun getNameIdentifier(): PsiElement? =
        PsiTreeUtil.getChildOfType(this, KamailioRouteNameDecl::class.java)?.routeName

    override fun getName(): String? = nameIdentifier?.text ?: kind?.keyword

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("Renaming routes is not supported yet")

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
