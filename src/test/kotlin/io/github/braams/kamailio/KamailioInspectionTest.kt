package io.github.braams.kamailio

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.braams.kamailio.inspections.DuplicateRouteInspection
import io.github.braams.kamailio.inspections.ModparamWithoutLoadmoduleInspection
import io.github.braams.kamailio.inspections.UnbalancedIfdefInspection
import io.github.braams.kamailio.inspections.UnresolvedRouteInspection

class KamailioInspectionTest : BasePlatformTestCase() {

    fun testUnresolvedRoute() {
        myFixture.enableInspections(UnresolvedRouteInspection())
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            request_route {
                route(<warning descr="Route 'MISSING' is not defined">MISSING</warning>);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testUnbalancedIfdef() {
        myFixture.enableInspections(UnbalancedIfdefInspection())
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            <error descr="Unclosed #!ifdef WITH_NAT">#!ifdef WITH_NAT</error>
            debug=3
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDuplicateRoute() {
        myFixture.enableInspections(DuplicateRouteInspection())
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            route[RELAY] {
                t_relay();
            }
            route[<warning descr="Duplicate route[RELAY] (already defined in kamailio.cfg)">RELAY</warning>] {
                exit;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testModparamWithoutLoadmodule() {
        myFixture.enableInspections(ModparamWithoutLoadmoduleInspection())
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            loadmodule "tm.so"
            modparam("tm", "fr_timer", 30000)
            modparam(<warning descr="Module 'usrloc' is not loaded with loadmodule">"usrloc"</warning>, "db_url", "url")
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true)
    }
}
