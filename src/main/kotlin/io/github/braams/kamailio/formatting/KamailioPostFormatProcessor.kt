package io.github.braams.kamailio.formatting

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioPpDirective

/**
 * After regular formatting, pins `#!` preprocessor directive lines to column 0
 * (C-preprocessor convention).
 */
class KamailioPostFormatProcessor : PostFormatProcessor {

    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        val file = source.containingFile
        if (file is KamailioFile) {
            processText(file, source.textRange, settings)
        }
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source !is KamailioFile) return rangeToReformat
        val document = PsiDocumentManager.getInstance(source.project).getDocument(source)
            ?: return rangeToReformat
        PsiDocumentManager.getInstance(source.project).doPostponedOperationsAndUnblockDocument(document)

        var removed = 0
        val directives = PsiTreeUtil.findChildrenOfType(source, KamailioPpDirective::class.java)
            .filter { rangeToReformat.contains(it.textRange.startOffset) }
            .sortedByDescending { it.textRange.startOffset }
        for (dir in directives) {
            val start = dir.textRange.startOffset
            val lineStart = document.getLineStartOffset(document.getLineNumber(start))
            if (lineStart < start && document.getText(TextRange(lineStart, start)).isBlank()) {
                document.deleteString(lineStart, start)
                removed += start - lineStart
            }
        }
        if (removed > 0) {
            PsiDocumentManager.getInstance(source.project).commitDocument(document)
        }
        return TextRange(rangeToReformat.startOffset, (rangeToReformat.endOffset - removed).coerceAtLeast(rangeToReformat.startOffset))
    }
}
