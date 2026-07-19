package io.github.braams.kamailio.highlighting

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioPpDirective
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin
import io.github.braams.kamailio.resolve.KamailioIncludeGraph

/**
 * Regions guarded by `#!ifdef SYMBOL` where SYMBOL is not #!define'd anywhere in the include graph.
 * Such code is never compiled by Kamailio (the stock config keeps raw SQL in one), so parse errors
 * inside them are suppressed.
 */
object KamailioInactiveRegions {

    fun isInInactiveRegion(element: PsiElement): Boolean {
        val offset = element.textRange.startOffset
        return regions(element.containingFile).any { it.contains(offset) }
    }

    private fun regions(file: PsiFile): List<TextRange> =
        CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(computeRegions(file), PsiModificationTracker.MODIFICATION_COUNT)
        }

    private fun computeRegions(file: PsiFile): List<TextRange> {
        val definedNames = KamailioIncludeGraph.defines(file)
            .mapNotNullTo(HashSet()) { (it as KamailioPpDefineMixin).name }

        val result = mutableListOf<TextRange>()
        // stack of (start offset after the opening directive, inactive?)
        val stack = ArrayDeque<Pair<Int, Boolean>>()
        for (dir in PsiTreeUtil.findChildrenOfType(file, KamailioPpDirective::class.java)) {
            val head = dir.node.firstChildNode ?: continue
            when (head.firstChildNode?.elementType) {
                KamailioTypes.PP_IFDEF_KW, KamailioTypes.PP_IFNDEF_KW -> {
                    val symbol = head.findChildByType(KamailioTypes.IDENT)?.text
                    val negated = head.firstChildNode?.elementType == KamailioTypes.PP_IFNDEF_KW
                    val inactive = symbol != null && (symbol in definedNames) == negated
                    stack.addLast(dir.textRange.endOffset to inactive)
                }
                KamailioTypes.PP_IFEXP_KW -> stack.addLast(dir.textRange.endOffset to false)
                KamailioTypes.PP_ELSE_KW -> {
                    val top = stack.removeLastOrNull() ?: continue
                    if (top.second) result += TextRange(top.first, dir.textRange.startOffset)
                    // after #!else the branch activity flips; be conservative: treat else-branch as active
                    stack.addLast(dir.textRange.endOffset to false)
                }
                KamailioTypes.PP_ENDIF_KW -> {
                    val top = stack.removeLastOrNull() ?: continue
                    if (top.second) result += TextRange(top.first, dir.textRange.startOffset)
                }
                else -> {}
            }
        }
        return result
    }
}
