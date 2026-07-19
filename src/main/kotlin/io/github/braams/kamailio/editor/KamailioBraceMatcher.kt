package io.github.braams.kamailio.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import io.github.braams.kamailio.psi.KamailioTypes

class KamailioBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(KamailioTypes.LBRACE, KamailioTypes.RBRACE, true),
            BracePair(KamailioTypes.LPAREN, KamailioTypes.RPAREN, false),
            BracePair(KamailioTypes.LBRACK, KamailioTypes.RBRACK, false)
        )
    }
}
