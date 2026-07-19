package io.github.braams.kamailio.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object KamailioColors {
    @JvmField
    val KEYWORD = TextAttributesKey.createTextAttributesKey("KAMAILIO_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    @JvmField
    val PP_DIRECTIVE = TextAttributesKey.createTextAttributesKey("KAMAILIO_PP_DIRECTIVE", DefaultLanguageHighlighterColors.METADATA)

    @JvmField
    val LINE_COMMENT = TextAttributesKey.createTextAttributesKey("KAMAILIO_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)

    @JvmField
    val BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey("KAMAILIO_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)

    @JvmField
    val STRING = TextAttributesKey.createTextAttributesKey("KAMAILIO_STRING", DefaultLanguageHighlighterColors.STRING)

    @JvmField
    val STRING_INTERP = TextAttributesKey.createTextAttributesKey("KAMAILIO_STRING_INTERP", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)

    @JvmField
    val NUMBER = TextAttributesKey.createTextAttributesKey("KAMAILIO_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

    @JvmField
    val IDENTIFIER = TextAttributesKey.createTextAttributesKey("KAMAILIO_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)

    @JvmField
    val PSEUDO_VAR = TextAttributesKey.createTextAttributesKey("KAMAILIO_PSEUDO_VAR", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

    @JvmField
    val OPERATOR = TextAttributesKey.createTextAttributesKey("KAMAILIO_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)

    @JvmField
    val PARENTHESES = TextAttributesKey.createTextAttributesKey("KAMAILIO_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)

    @JvmField
    val BRACES = TextAttributesKey.createTextAttributesKey("KAMAILIO_BRACES", DefaultLanguageHighlighterColors.BRACES)

    @JvmField
    val BRACKETS = TextAttributesKey.createTextAttributesKey("KAMAILIO_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)

    @JvmField
    val SEMICOLON = TextAttributesKey.createTextAttributesKey("KAMAILIO_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)

    @JvmField
    val COMMA = TextAttributesKey.createTextAttributesKey("KAMAILIO_COMMA", DefaultLanguageHighlighterColors.COMMA)

    @JvmField
    val ROUTE_NAME = TextAttributesKey.createTextAttributesKey("KAMAILIO_ROUTE_NAME", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)

    @JvmField
    val FUNCTION_CALL = TextAttributesKey.createTextAttributesKey("KAMAILIO_FUNCTION_CALL", DefaultLanguageHighlighterColors.FUNCTION_CALL)

    @JvmField
    val CONSTANT = TextAttributesKey.createTextAttributesKey("KAMAILIO_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT)

    @JvmField
    val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("KAMAILIO_BAD_CHARACTER", com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER)
}
