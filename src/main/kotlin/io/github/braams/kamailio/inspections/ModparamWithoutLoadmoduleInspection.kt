package io.github.braams.kamailio.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import io.github.braams.kamailio.psi.impl.KamailioLoadmoduleMixin
import io.github.braams.kamailio.psi.impl.KamailioModparamMixin
import io.github.braams.kamailio.resolve.KamailioIncludeGraph

class ModparamWithoutLoadmoduleInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is KamailioModparamMixin) return
                val file = element.containingFile
                if (KamailioIncludeGraph.hasUnresolvedIncludes(file)) return
                // #!ifdef conditions are deliberately ignored: a loadmodule inside any branch counts
                val loaded = KamailioIncludeGraph.loadmodules(file)
                    .mapNotNullTo(HashSet()) { (it as KamailioLoadmoduleMixin).moduleName }
                if (loaded.isEmpty()) return
                for (module in element.moduleNames) {
                    if (module !in loaded) {
                        holder.registerProblem(
                            element.moduleNameElement ?: element,
                            "Module '$module' is not loaded with loadmodule"
                        )
                    }
                }
            }
        }
}
