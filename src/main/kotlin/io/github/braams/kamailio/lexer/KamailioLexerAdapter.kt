package io.github.braams.kamailio.lexer

import com.intellij.lexer.FlexAdapter

class KamailioLexerAdapter : FlexAdapter(_KamailioLexer()) {
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        (flex as _KamailioLexer).clearStates()
        super.start(buffer, startOffset, endOffset, initialState)
    }
}
