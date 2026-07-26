package io.github.braams.kamailio

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.braams.kamailio.psi.KamailioPvKey
import io.github.braams.kamailio.psi.impl.KamailioPvMixin

/**
 * Regressions for the lexer/parser split: the lexer tokenizes some words as hard keywords
 * regardless of context, but real configs use them as ordinary names in places the lexer
 * cannot tell apart (pv keys, transformation arguments). See `kw_as_name_` in Kamailio.bnf.
 */
class KamailioParserTest : BasePlatformTestCase() {

    private fun assertNoParseErrors(text: String) {
        myFixture.configureByText("kamailio.cfg", text)
        val errors = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java)
        assertTrue(
            "unexpected parse errors: ${errors.joinToString { it.errorDescription }}",
            errors.isEmpty()
        )
    }

    fun testRouteKeywordAsVarKey() {
        assertNoParseErrors(
            """
            #!KAMAILIO
            request_route {
                ${'$'}var(route) = ${'$'}sht(routes=>${'$'}rU);
            }
            """.trimIndent()
        )
    }

    fun testRouteKeywordAsVarKeyResolvesToPlainKeyText() {
        myFixture.configureByText(
            "kamailio.cfg",
            "#!KAMAILIO\nrequest_route {\n    \$var(route) = 1;\n}\n"
        )
        val pv = PsiTreeUtil.findChildOfType(myFixture.file, KamailioPvMixin::class.java)!!
        assertEquals("var", pv.pvName)
        assertEquals("route", pv.keyText)
        assertNotNull(PsiTreeUtil.findChildOfType(pv, KamailioPvKey::class.java))
    }

    fun testOtherHardKeywordsAsAvpAndHtableKeys() {
        assertNoParseErrors(
            """
            #!KAMAILIO
            request_route {
                ${'$'}avp(if) = 1;
                ${'$'}avp(while) = 1;
                ${'$'}sht(h=>case) = 1;
                ${'$'}sht(h=>modparam) = 1;
                ${'$'}var(desc) = 1;
            }
            """.trimIndent()
        )
    }

    fun testRouteKeywordStillWorksAsRouteCall() {
        assertNoParseErrors(
            """
            #!KAMAILIO
            request_route {
                route(RELAY);
            }
            route[RELAY] {
                exit;
            }
            """.trimIndent()
        )
    }

    fun testKeywordAsNameInsideTransformationArg() {
        assertNoParseErrors(
            """
            #!KAMAILIO
            request_route {
                ${'$'}var(x) = ${'$'}(rU{s.select,route});
            }
            """.trimIndent()
        )
    }
}
