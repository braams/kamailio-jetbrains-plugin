package io.github.braams.kamailio.formatting

import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import io.github.braams.kamailio.KamailioLanguage

class KamailioLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = KamailioLanguage

    override fun getIndentOptionsEditor(): SmartIndentOptionsEditor = SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions
    ) {
        // the stock kamailio.cfg is tab-indented — follow that convention by default
        indentOptions.USE_TAB_CHARACTER = true
        indentOptions.TAB_SIZE = 4
        indentOptions.INDENT_SIZE = 4
        indentOptions.CONTINUATION_INDENT_SIZE = 4   // one tab, like the wrapped modparam strings in the stock config

        // spacing defaults follow the dominant style of the stock kamailio.cfg
        commonSettings.SPACE_AFTER_COMMA = true
        commonSettings.SPACE_BEFORE_COMMA = false
        commonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true   // $var(rc) = $rc;
        commonSettings.SPACE_AROUND_EQUALITY_OPERATORS = false    // $rU==$null, uri=~"..."
        commonSettings.SPACE_AROUND_RELATIONAL_OPERATORS = false
        commonSettings.SPACE_AROUND_LOGICAL_OPERATORS = true      // a && b
        commonSettings.SPACE_AROUND_ADDITIVE_OPERATORS = true     // "sip:" + $rU
        commonSettings.SPACE_BEFORE_IF_PARENTHESES = true         // if (...)
        commonSettings.SPACE_BEFORE_SWITCH_PARENTHESES = true
        commonSettings.SPACE_BEFORE_WHILE_PARENTHESES = true
        commonSettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES = false // t_relay(), modparam(
        commonSettings.SPACE_WITHIN_PARENTHESES = false
        commonSettings.SPACE_BEFORE_ELSE_KEYWORD = true           // } else
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        when (settingsType) {
            SettingsType.INDENT_SETTINGS ->
                consumer.showStandardOptions(
                    "INDENT_SIZE", "TAB_SIZE", "USE_TAB_CHARACTER", "KEEP_INDENTS_ON_EMPTY_LINES"
                )
            SettingsType.SPACING_SETTINGS ->
                consumer.showStandardOptions(
                    "SPACE_AFTER_COMMA", "SPACE_BEFORE_COMMA",
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS", "SPACE_AROUND_EQUALITY_OPERATORS",
                    "SPACE_AROUND_RELATIONAL_OPERATORS", "SPACE_AROUND_LOGICAL_OPERATORS",
                    "SPACE_AROUND_ADDITIVE_OPERATORS",
                    "SPACE_BEFORE_IF_PARENTHESES", "SPACE_BEFORE_SWITCH_PARENTHESES",
                    "SPACE_BEFORE_WHILE_PARENTHESES", "SPACE_BEFORE_METHOD_CALL_PARENTHESES",
                    "SPACE_WITHIN_PARENTHESES", "SPACE_BEFORE_ELSE_KEYWORD"
                )
            else -> {}
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = """
        #!KAMAILIO
        #!define WITH_NAT

        debug=2
        loadmodule "tm.so"
        modparam("tm", "fr_timer", 30000)

        request_route {
            if (!mf_process_maxfwd_header("10")) {
                sl_send_reply("483", "Too Many Hops");
                exit;
            }
        #!ifdef WITH_NAT
            force_rport();
        #!endif
            switch(${'$'}rU) {
                case "test":
                    xlog("test call\n");
                    break;
                default:
                    route(RELAY);
            }
        }

        route[RELAY] {
            if (!t_relay()) {
                sl_reply_error();
            }
        }
    """.trimIndent()
}
