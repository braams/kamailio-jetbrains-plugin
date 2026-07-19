package io.github.braams.kamailio.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.github.braams.kamailio.lexer.KamailioLexerAdapter
import io.github.braams.kamailio.psi.KamailioTokenSets
import io.github.braams.kamailio.psi.KamailioTypes

class KamailioSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = KamailioLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when {
        tokenType == null -> emptyArray()
        KamailioTokenSets.KEYWORDS.contains(tokenType) -> pack(KamailioColors.KEYWORD)
        KamailioTokenSets.PP_DIRECTIVES.contains(tokenType) -> pack(KamailioColors.PP_DIRECTIVE)
        tokenType == KamailioTypes.PP_TEXT -> pack(KamailioColors.PP_DIRECTIVE)
        tokenType == KamailioTypes.LINE_COMMENT -> pack(KamailioColors.LINE_COMMENT)
        tokenType == KamailioTypes.BLOCK_COMMENT -> pack(KamailioColors.BLOCK_COMMENT)
        tokenType == KamailioTypes.STRING_INTERP -> pack(KamailioColors.STRING_INTERP)
        KamailioTokenSets.STRINGS.contains(tokenType) -> pack(KamailioColors.STRING)
        tokenType == KamailioTypes.NUMBER || tokenType == KamailioTypes.IP_LITERAL -> pack(KamailioColors.NUMBER)
        tokenType == KamailioTypes.DOLLAR -> pack(KamailioColors.PSEUDO_VAR)
        tokenType == KamailioTypes.EVENT_NAME -> pack(KamailioColors.ROUTE_NAME)
        tokenType == KamailioTypes.IDENT || tokenType == KamailioTypes.DESC_KW -> pack(KamailioColors.IDENTIFIER)
        KamailioTokenSets.OPERATORS.contains(tokenType) -> pack(KamailioColors.OPERATOR)
        tokenType == KamailioTypes.LPAREN || tokenType == KamailioTypes.RPAREN -> pack(KamailioColors.PARENTHESES)
        tokenType == KamailioTypes.LBRACE || tokenType == KamailioTypes.RBRACE -> pack(KamailioColors.BRACES)
        tokenType == KamailioTypes.LBRACK || tokenType == KamailioTypes.RBRACK -> pack(KamailioColors.BRACKETS)
        tokenType == KamailioTypes.SEMICOLON -> pack(KamailioColors.SEMICOLON)
        tokenType == KamailioTypes.COMMA -> pack(KamailioColors.COMMA)
        tokenType == TokenType.BAD_CHARACTER -> pack(KamailioColors.BAD_CHARACTER)
        else -> emptyArray()
    }
}
