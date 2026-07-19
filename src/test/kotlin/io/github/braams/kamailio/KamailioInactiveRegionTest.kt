package io.github.braams.kamailio

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Raw SQL inside `#!ifdef ACCDB_COMMENT` (never defined) must not be highlighted as errors —
 * KamailioInactiveRegionErrorFilter suppresses PsiErrorElement highlighting there.
 */
class KamailioInactiveRegionTest : BasePlatformTestCase() {

    fun testSqlInsideInactiveIfdefIsNotHighlighted() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            #!ifdef ACCDB_COMMENT
              ALTER TABLE acc ADD COLUMN src_user VARCHAR(64) NOT NULL DEFAULT '';
              ALTER TABLE missed_calls ADD COLUMN src_domain VARCHAR(128) NOT NULL DEFAULT '';
            #!endif
            request_route {
                exit;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorsInActiveCodeStillHighlighted() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            #!define WITH_FOO
            #!ifdef WITH_FOO
            ALTER TABLE acc;
            #!endif
            """.trimIndent()
        )
        // just assert that at least one error IS reported in an active region
        val errors = myFixture.doHighlighting().filter { it.severity.name == "ERROR" }
        assertTrue("errors in active regions must stay visible", errors.isNotEmpty())
    }
}
