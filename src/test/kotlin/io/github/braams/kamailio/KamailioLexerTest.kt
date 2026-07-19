package io.github.braams.kamailio

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase
import io.github.braams.kamailio.lexer.KamailioLexerAdapter

class KamailioLexerTest : LexerTestCase() {

    override fun createLexer(): Lexer = KamailioLexerAdapter()
    override fun getDirPath(): String = ""

    fun testHashCommentVsDirective() {
        doTest(
            "# comment\n#!define WITH_NAT\n#comment2\n#\n",
            """
            LINE_COMMENT ('# comment')
            WHITE_SPACE ('\n')
            #!define ('#!define')
            WHITE_SPACE (' ')
            IDENT ('WITH_NAT')
            WHITE_SPACE ('\n')
            LINE_COMMENT ('#comment2')
            WHITE_SPACE ('\n')
            LINE_COMMENT ('#')
            WHITE_SPACE ('\n')
            """.trimIndent()
        )
    }

    fun testShebang() {
        doTest(
            "#!KAMAILIO\n",
            """
            #!KAMAILIO ('#!KAMAILIO')
            WHITE_SPACE ('\n')
            """.trimIndent()
        )
    }

    fun testStringInterpolation() {
        doTest(
            "\"blocked - \$rm from \$fu (IP:\$si:\$sp)\\n\"",
            """
            " ('"')
            STRING_TEXT ('blocked - ')
            STRING_INTERP ('${'$'}rm')
            STRING_TEXT (' from ')
            STRING_INTERP ('${'$'}fu')
            STRING_TEXT (' (IP:')
            STRING_INTERP ('${'$'}si')
            STRING_TEXT (':')
            STRING_INTERP ('${'$'}sp')
            STRING_TEXT (')\n')
            " ('"')
            """.trimIndent()
        )
    }

    fun testShtNestedPv() {
        doTest(
            "\$sht(ipban=>\$si)",
            """
            ${'$'} ('${'$'}')
            IDENT ('sht')
            ( ('(')
            IDENT ('ipban')
            => ('=>')
            ${'$'} ('${'$'}')
            IDENT ('si')
            ) (')')
            """.trimIndent()
        )
    }

    fun testPvTransformation() {
        doTest(
            "\$(ru{s.len})",
            """
            ${'$'} ('${'$'}')
            ( ('(')
            IDENT ('ru')
            { ('{')
            IDENT ('s')
            . ('.')
            IDENT ('len')
            } ('}')
            ) (')')
            """.trimIndent()
        )
    }

    fun testEventRouteName() {
        doTest(
            "event_route[xhttp:request] {",
            """
            event_route ('event_route')
            [ ('[')
            EVENT_NAME ('xhttp:request')
            ] (']')
            WHITE_SPACE (' ')
            { ('{')
            """.trimIndent()
        )
    }

    fun testIpLiteralAndNumbers() {
        doTest(
            "src_ip!=127.0.0.1 && \$var(x)==-1",
            """
            IDENT ('src_ip')
            != ('!=')
            IP_LITERAL ('127.0.0.1')
            WHITE_SPACE (' ')
            && ('&&')
            WHITE_SPACE (' ')
            ${'$'} ('${'$'}')
            IDENT ('var')
            ( ('(')
            IDENT ('x')
            ) (')')
            == ('==')
            - ('-')
            NUMBER ('1')
            """.trimIndent()
        )
    }

    fun testComments() {
        doTest(
            "// slash comment\n/* block\ncomment */",
            """
            LINE_COMMENT ('// slash comment')
            WHITE_SPACE ('\n')
            BLOCK_COMMENT ('/* block\ncomment */')
            """.trimIndent()
        )
    }

    fun testDefineWithStringValue() {
        doTest(
            "#!trydef DBURL \"mysql://kamailio@localhost/kamailio\"\n",
            """
            #!define ('#!trydef')
            WHITE_SPACE (' ')
            IDENT ('DBURL')
            WHITE_SPACE (' ')
            " ('"')
            STRING_TEXT ('mysql://kamailio@localhost/kamailio')
            " ('"')
            WHITE_SPACE ('\n')
            """.trimIndent()
        )
    }
}
