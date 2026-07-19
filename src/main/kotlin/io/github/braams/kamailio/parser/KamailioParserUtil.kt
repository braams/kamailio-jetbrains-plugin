package io.github.braams.kamailio.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType

object KamailioParserUtil : GeneratedParserUtilBase() {

    /**
     * True when the next token is on the same line as the previous non-whitespace token.
     * A backslash-newline (line continuation inside a directive) counts as the same line.
     * Used to stop preprocessor-directive values at the end of the line, since the lexer
     * treats newlines as plain whitespace.
     */
    @JvmStatic
    fun sameLine(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
        if (builder.eof()) return false
        var step = -1
        while (true) {
            val type = builder.rawLookup(step) ?: return false
            if (type != TokenType.WHITE_SPACE) return true
            val start = builder.rawTokenTypeStart(step)
            val end = builder.rawTokenTypeStart(step + 1)
            val text = builder.originalText.subSequence(start, end)
            if (text.contains('\n') && !text.contains('\\')) return false
            step--
        }
    }
}
