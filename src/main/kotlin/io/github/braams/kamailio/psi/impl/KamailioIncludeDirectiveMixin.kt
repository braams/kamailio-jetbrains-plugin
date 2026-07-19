package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioPsiUtil
import io.github.braams.kamailio.psi.KamailioStringLiteral

open class KamailioIncludeDirectiveMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    val pathText: String?
        get() = PsiTreeUtil.getChildOfType(this, KamailioStringLiteral::class.java)
            ?.let { KamailioPsiUtil.stringLiteralContent(it) }
            ?.takeIf { it.isNotBlank() }

    /** Resolves the included file relative to the directory of the containing file. */
    fun resolveIncludedFile(): PsiFile? {
        val path = pathText ?: return null
        val baseDir = containingFile?.originalFile?.virtualFile?.parent ?: return null
        val target = baseDir.findFileByRelativePath(path) ?: return null
        return PsiManager.getInstance(project).findFile(target)
    }
}
