package io.github.braams.kamailio.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import io.github.braams.kamailio.psi.KamailioLoadmoduleStmt
import io.github.braams.kamailio.psi.KamailioPpDefine
import io.github.braams.kamailio.psi.KamailioRouteDef

class KamailioStructureViewModel(editor: Editor?, psiFile: PsiFile) :
    StructureViewModelBase(psiFile, editor, KamailioStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {

    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = element.value is PsiFile

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element.value is KamailioRouteDef || element.value is KamailioPpDefine || element.value is KamailioLoadmoduleStmt
}
