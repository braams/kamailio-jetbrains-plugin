package io.github.braams.kamailio

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KamailioFormatterTest : BasePlatformTestCase() {

    fun testIndentationAndDirectivesAtColumnZero() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            request_route {
            xlog("x");
            if (1) {
            xlog("y");
            }
            #!ifdef WITH_X
            xlog("z");
            #!endif
            switch(${'$'}rU) {
            case "a":
            xlog("a");
            break;
            default:
            exit;
            }
            }
            """.trimIndent()
        )
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        // default Kamailio code style uses tab indentation (like the stock kamailio.cfg)
        myFixture.checkResult(
            """
            #!KAMAILIO
            request_route {
            >xlog("x");
            >if (1) {
            >>xlog("y");
            >}
            #!ifdef WITH_X
            >xlog("z");
            #!endif
            >switch (${'$'}rU) {
            >>case "a":
            >>>xlog("a");
            >>>break;
            >>default:
            >>>exit;
            >}
            }
            """.trimIndent().replace(">", "\t")
        )
    }

    fun testSpacingFollowsStockStyle() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            debug = 3
            loadmodule    "tm.so"
            modparam("tm","fr_timer",30000)
            request_route {
            >if(!t_relay()){
            >>sl_send_reply("500" ,"err");
            >}else{
            >>${'$'}var(x)=${'$'}rU+"@"+${'$'}rd;
            >}
            >if (${'$'}var(x) == 1 && ${'$'}si!=127.0.0.1) {
            >>exit;
            >}
            }
            """.trimIndent().replace(">", "\t")
        )
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        myFixture.checkResult(
            """
            #!KAMAILIO
            debug=3
            loadmodule "tm.so"
            modparam("tm", "fr_timer", 30000)
            request_route {
            >if (!t_relay()) {
            >>sl_send_reply("500", "err");
            >} else {
            >>${'$'}var(x) = ${'$'}rU + "@" + ${'$'}rd;
            >}
            >if (${'$'}var(x)==1 && ${'$'}si!=127.0.0.1) {
            >>exit;
            >}
            }
            """.trimIndent().replace(">", "\t")
        )
    }

    fun testPseudoVariableInternalsUntouched() {
        val text = "#!KAMAILIO\nrequest_route {\n\t\$var(x) = \$sht(ipban=>\$si) + \$(rU{s.substr,0,4});\n}\n"
        myFixture.configureByText("kamailio.cfg", text)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        myFixture.checkResult(text)
    }

    fun testReformatOfStockConfigIsIdempotent() {
        val stock = java.io.File("src/test/testData/kamailio.cfg").readText()
        myFixture.configureByText("kamailio.cfg", stock)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        val once = myFixture.file.text
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertEquals("reformat must be idempotent", once, myFixture.file.text)
    }
}
