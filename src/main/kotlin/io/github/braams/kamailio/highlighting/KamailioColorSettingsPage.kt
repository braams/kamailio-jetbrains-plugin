package io.github.braams.kamailio.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import io.github.braams.kamailio.KamailioIcons
import javax.swing.Icon

class KamailioColorSettingsPage : ColorSettingsPage {

    override fun getDisplayName(): String = "Kamailio"
    override fun getIcon(): Icon = KamailioIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = KamailioSyntaxHighlighter()
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getDemoText(): String = """
        #!KAMAILIO
        # line comment
        /* block
           comment */
        #!define WITH_NAT
        #!trydef DBURL "mysql://kamailio:kamailiorw@localhost/kamailio"

        debug=2
        log_prefix="{${'$'}mt ${'$'}hdr(CSeq) ${'$'}ci} "

        loadmodule "tm.so"
        modparam("usrloc", "db_url", DBURL)

        request_route {
            if (!mf_process_maxfwd_header("10")) {
                sl_send_reply("483", "Too Many Hops");
                exit;
            }
            ${'$'}var(rc) = ${'$'}rU + "@" + ${'$'}(ru{s.len});
            route(RELAY);
        }

        route[RELAY] {
            if (!t_relay()) {
                sl_reply_error();
            }
        }

        event_route[xhttp:request] {
            xhttp_reply("200", "OK", "text/html", "<html>ok ${'$'}si</html>");
        }
    """.trimIndent()

    private companion object {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", KamailioColors.KEYWORD),
            AttributesDescriptor("Preprocessor directive", KamailioColors.PP_DIRECTIVE),
            AttributesDescriptor("Line comment", KamailioColors.LINE_COMMENT),
            AttributesDescriptor("Block comment", KamailioColors.BLOCK_COMMENT),
            AttributesDescriptor("String", KamailioColors.STRING),
            AttributesDescriptor("Pseudo-variable in string", KamailioColors.STRING_INTERP),
            AttributesDescriptor("Number", KamailioColors.NUMBER),
            AttributesDescriptor("Identifier", KamailioColors.IDENTIFIER),
            AttributesDescriptor("Pseudo-variable", KamailioColors.PSEUDO_VAR),
            AttributesDescriptor("Operator", KamailioColors.OPERATOR),
            AttributesDescriptor("Parentheses", KamailioColors.PARENTHESES),
            AttributesDescriptor("Braces", KamailioColors.BRACES),
            AttributesDescriptor("Brackets", KamailioColors.BRACKETS),
            AttributesDescriptor("Semicolon", KamailioColors.SEMICOLON),
            AttributesDescriptor("Comma", KamailioColors.COMMA),
            AttributesDescriptor("Route name", KamailioColors.ROUTE_NAME),
            AttributesDescriptor("Function call", KamailioColors.FUNCTION_CALL),
            AttributesDescriptor("Constant (define)", KamailioColors.CONSTANT),
            AttributesDescriptor("Bad character", KamailioColors.BAD_CHARACTER)
        )
    }
}
