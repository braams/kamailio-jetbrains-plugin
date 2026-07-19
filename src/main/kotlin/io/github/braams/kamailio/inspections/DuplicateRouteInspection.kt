package io.github.braams.kamailio.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin
import io.github.braams.kamailio.resolve.KamailioIncludeGraph

class DuplicateRouteInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file !is KamailioFile) return
                val seen = HashMap<Pair<Any?, String>, KamailioRouteDefMixin>()
                for (def in KamailioIncludeGraph.routeDefs(file)) {
                    if (def !is KamailioRouteDefMixin) continue
                    val name = def.nameIdentifier?.text ?: continue
                    val key = def.kind to name
                    val first = seen.putIfAbsent(key, def)
                    if (first != null && def.containingFile == file) {
                        holder.registerProblem(
                            def.nameIdentifier ?: def,
                            "Duplicate ${def.kind?.keyword ?: "route"}[$name] (already defined in ${first.containingFile.name})"
                        )
                    }
                }
            }
        }
}
