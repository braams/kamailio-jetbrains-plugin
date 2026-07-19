package io.github.braams.kamailio

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import io.github.braams.kamailio.parser.KamailioParserDefinition
import java.io.File

/**
 * Integration check: the stock kamailio.cfg (v6.2 default) must parse without errors,
 * except inside the ACCDB_COMMENT ifdef region which intentionally contains raw SQL.
 */
class KamailioDefaultConfigTest : ParsingTestCase("", "cfg", KamailioParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    fun testDefaultConfigParsesClean() {
        val text = File(testDataPath, "kamailio.cfg").readText()
        val file = createPsiFile("kamailio", text)
        ensureParsed(file)

        // The ACCDB_COMMENT region holds raw SQL that is not valid Kamailio syntax by design
        val sqlRegionStart = text.indexOf("#!ifdef ACCDB_COMMENT")
        val sqlRegionEnd = text.indexOf("#!endif", sqlRegionStart)
        check(sqlRegionStart >= 0 && sqlRegionEnd > sqlRegionStart) { "ACCDB_COMMENT region not found" }

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
            .filter { it.textOffset < sqlRegionStart || it.textOffset > sqlRegionEnd }

        if (errors.isNotEmpty()) {
            val details = errors.joinToString("\n") { err ->
                val line = StringUtil.offsetToLineNumber(text, err.textOffset) + 1
                val context = text.lines().getOrNull(line - 1)?.trim() ?: ""
                "line $line: ${err.errorDescription} | $context"
            }
            fail("${errors.size} parse errors outside the ACCDB_COMMENT region:\n$details")
        }

        // guard against a vacuous pass: the structure AFTER the SQL region must really be parsed
        val routes = PsiTreeUtil.findChildrenOfType(file, io.github.braams.kamailio.psi.KamailioRouteDef::class.java)
            .filterIsInstance<io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin>()
        assertTrue("request_route not parsed", routes.any { it.kind == io.github.braams.kamailio.psi.RouteKind.REQUEST })
        assertTrue("route[RELAY] not parsed", routes.any { it.name == "RELAY" })
        assertTrue("failure_route[MANAGE_FAILURE] not parsed", routes.any { it.name == "MANAGE_FAILURE" })
    }
}
