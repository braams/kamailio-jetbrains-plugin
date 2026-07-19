package io.github.braams.kamailio

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.braams.kamailio.psi.RouteKind
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin

class KamailioResolveTest : BasePlatformTestCase() {

    fun testRouteCallResolvesToRouteDef() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            request_route {
                route(REL<caret>AY);
            }
            route[RELAY] {
                t_relay();
            }
            """.trimIndent()
        )
        val ref = myFixture.file.findReferenceAt(myFixture.caretOffset)
        val target = ref?.resolve()
        assertInstanceOf(target, KamailioRouteDefMixin::class.java)
        assertEquals("RELAY", (target as KamailioRouteDefMixin).name)
        assertEquals(RouteKind.ROUTE, target.kind)
    }

    fun testTOnBranchResolvesToBranchRoute() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            request_route {
                t_on_branch("MANAGE<caret>_BRANCH");
            }
            branch_route[MANAGE_BRANCH] {
                xlog("branch");
            }
            """.trimIndent()
        )
        val ref = myFixture.file.findReferenceAt(myFixture.caretOffset)
        val target = ref?.resolve()
        assertInstanceOf(target, KamailioRouteDefMixin::class.java)
        assertEquals(RouteKind.BRANCH, (target as KamailioRouteDefMixin).kind)
    }

    fun testDefineUsageResolves() {
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            #!define DBGLEVEL 3
            debug=DBG<caret>LEVEL
            """.trimIndent()
        )
        val ref = myFixture.file.findReferenceAt(myFixture.caretOffset)
        val target = ref?.resolve()
        assertInstanceOf(target, KamailioPpDefineMixin::class.java)
        assertEquals("DBGLEVEL", (target as KamailioPpDefineMixin).name)
    }

    fun testCrossFileResolveThroughInclude() {
        myFixture.addFileToProject(
            "kamailio-local.cfg",
            """
            #!KAMAILIO
            route[FROM_INCLUDED] {
                t_relay();
            }
            """.trimIndent()
        )
        myFixture.configureByText(
            "kamailio.cfg",
            """
            #!KAMAILIO
            include_file "kamailio-local.cfg"
            request_route {
                route(FROM_IN<caret>CLUDED);
            }
            """.trimIndent()
        )
        val ref = myFixture.file.findReferenceAt(myFixture.caretOffset)
        val target = ref?.resolve()
        assertInstanceOf(target, KamailioRouteDefMixin::class.java)
        assertEquals("kamailio-local.cfg", (target as KamailioRouteDefMixin).containingFile.name)
    }
}
