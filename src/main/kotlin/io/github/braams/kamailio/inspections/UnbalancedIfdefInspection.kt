package io.github.braams.kamailio.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioPpDirective
import io.github.braams.kamailio.psi.KamailioTypes

class UnbalancedIfdefInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file !is KamailioFile) return
                val stack = ArrayDeque<KamailioPpDirective>()
                for (dir in PsiTreeUtil.findChildrenOfType(file, KamailioPpDirective::class.java)) {
                    when (dir.node.firstChildNode?.firstChildNode?.elementType) {
                        KamailioTypes.PP_IFDEF_KW, KamailioTypes.PP_IFNDEF_KW, KamailioTypes.PP_IFEXP_KW ->
                            stack.addLast(dir)
                        KamailioTypes.PP_ELSE_KW ->
                            if (stack.isEmpty()) {
                                holder.registerProblem(dir, "#!else without matching #!ifdef")
                            }
                        KamailioTypes.PP_ENDIF_KW ->
                            if (stack.removeLastOrNull() == null) {
                                holder.registerProblem(dir, "#!endif without matching #!ifdef")
                            }
                        else -> {}
                    }
                }
                for (unclosed in stack) {
                    holder.registerProblem(unclosed, "Unclosed ${unclosed.text.trim().lineSequence().first()}")
                }
            }
        }
}
