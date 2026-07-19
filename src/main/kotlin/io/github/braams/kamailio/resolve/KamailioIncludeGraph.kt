package io.github.braams.kamailio.resolve

import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioIncludeDirective
import io.github.braams.kamailio.psi.KamailioLoadmoduleStmt
import io.github.braams.kamailio.psi.KamailioPpDefine
import io.github.braams.kamailio.psi.KamailioRouteDef
import io.github.braams.kamailio.psi.impl.KamailioIncludeDirectiveMixin

/**
 * Kamailio includes are textual, so the resolve scope of a file is the file itself plus everything
 * reachable through include_file / import_file. v1 limitation: definitions in the *including* file
 * are not visible from the included one.
 */
object KamailioIncludeGraph {

    fun scope(origin: PsiFile): List<KamailioFile> =
        CachedValuesManager.getCachedValue(origin) {
            val result = LinkedHashSet<KamailioFile>()
            collect(origin, result)
            CachedValueProvider.Result.create(result.toList(), PsiModificationTracker.MODIFICATION_COUNT)
        }

    /** true when some include_file path cannot be resolved — inspections should go quiet then */
    fun hasUnresolvedIncludes(origin: PsiFile): Boolean =
        scope(origin).any { file ->
            PsiTreeUtil.findChildrenOfType(file, KamailioIncludeDirective::class.java)
                .any { (it as KamailioIncludeDirectiveMixin).resolveIncludedFile() == null }
        }

    fun routeDefs(origin: PsiFile): List<KamailioRouteDef> =
        scope(origin).flatMap { PsiTreeUtil.findChildrenOfType(it, KamailioRouteDef::class.java) }

    fun defines(origin: PsiFile): List<KamailioPpDefine> =
        scope(origin).flatMap { PsiTreeUtil.findChildrenOfType(it, KamailioPpDefine::class.java) }

    fun loadmodules(origin: PsiFile): List<KamailioLoadmoduleStmt> =
        scope(origin).flatMap { PsiTreeUtil.findChildrenOfType(it, KamailioLoadmoduleStmt::class.java) }

    private fun collect(file: PsiFile, acc: MutableSet<KamailioFile>) {
        if (file !is KamailioFile || !acc.add(file)) return
        for (include in PsiTreeUtil.findChildrenOfType(file, KamailioIncludeDirective::class.java)) {
            (include as KamailioIncludeDirectiveMixin).resolveIncludedFile()?.let { collect(it, acc) }
        }
    }
}
