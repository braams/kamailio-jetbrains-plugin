package io.github.braams.kamailio.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioLoadmoduleStmt
import io.github.braams.kamailio.psi.KamailioPpDefine
import io.github.braams.kamailio.psi.KamailioRouteDef
import io.github.braams.kamailio.psi.impl.KamailioLoadmoduleMixin
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin

class KamailioStructureViewElement(private val element: NavigatablePsiElement) :
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element
    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)
    override fun canNavigate(): Boolean = element.canNavigate()
    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()

    override fun getAlphaSortKey(): String = presentableText()

    override fun getPresentation(): ItemPresentation {
        val icon = when (element) {
            is KamailioRouteDef -> PlatformIcons.METHOD_ICON
            is KamailioPpDefine -> PlatformIcons.FIELD_ICON
            is KamailioLoadmoduleStmt -> PlatformIcons.LIBRARY_ICON
            else -> element.presentation?.getIcon(false)
        }
        return PresentationData(presentableText(), locationText(), icon, null)
    }

    override fun getChildren(): Array<TreeElement> {
        if (element !is KamailioFile) return TreeElement.EMPTY_ARRAY
        val children = mutableListOf<TreeElement>()
        // Top level and inside #!ifdef regions only — defines/loadmodules/routes are never nested in each other
        PsiTreeUtil.findChildrenOfAnyType(
            element, KamailioPpDefine::class.java, KamailioLoadmoduleStmt::class.java, KamailioRouteDef::class.java
        ).forEach { children += KamailioStructureViewElement(it as NavigatablePsiElement) }
        return children.toTypedArray()
    }

    private fun presentableText(): String = when (element) {
        is KamailioFile -> element.name
        is KamailioRouteDefMixin -> {
            val kind = element.kind?.keyword ?: "route"
            val name = element.nameIdentifier?.text
            if (name != null) "$kind[$name]" else kind
        }
        is KamailioPpDefineMixin -> element.name ?: "#!define"
        is KamailioLoadmoduleMixin -> element.moduleName ?: "loadmodule"
        else -> element.name ?: element.text.take(30)
    }

    private fun locationText(): String? = when (element) {
        is KamailioPpDefineMixin -> element.valueText?.take(40)
        else -> null
    }
}
