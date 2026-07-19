package io.github.braams.kamailio.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import io.github.braams.kamailio.resolve.KamailioIncludeGraph
import io.github.braams.kamailio.resolve.KamailioRouteReference

class UnresolvedRouteInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                for (ref in element.references) {
                    if (ref is KamailioRouteReference && ref.multiResolve(false).isEmpty()) {
                        if (KamailioIncludeGraph.hasUnresolvedIncludes(element.containingFile)) return
                        holder.registerProblem(
                            element,
                            "Route '${ref.canonicalText}' is not defined",
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                            ref.rangeInElement
                        )
                    }
                }
            }
        }
}
