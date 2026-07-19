package io.github.braams.kamailio

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KamailioCompletionTest : BasePlatformTestCase() {

    private fun completions(text: String): List<String> {
        myFixture.configureByText("kamailio.cfg", text)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings ?: emptyList()
    }

    fun testGlobalParamsAtTopLevel() {
        val items = completions("#!KAMAILIO\nchil<caret>\n")
        assertContainsElements(items, "children")
        assertDoesntContain(items, "t_relay")
    }

    fun testTopLevelKeywords() {
        val items = completions("#!KAMAILIO\nloadm<caret>\n")
        assertContainsElements(items, "loadmodule")
    }

    fun testFunctionsInsideRoute() {
        val items = completions("#!KAMAILIO\nrequest_route {\n    t_rel<caret>\n}\n")
        assertContainsElements(items, "t_relay")
        assertDoesntContain(items, "children")
    }

    fun testKeywordsInsideCondition() {
        val items = completions("#!KAMAILIO\nrequest_route {\n    if (mysel<caret>) { }\n}\n")
        assertContainsElements(items, "myself")
    }

    fun testModuleNamesInLoadmodule() {
        val items = completions("#!KAMAILIO\nloadmodule \"t<caret>\"\n")
        assertContainsElements(items, "tm.so", "tls.so")
    }

    fun testModuleNamesInModparamFirstArg() {
        val items = completions("#!KAMAILIO\nmodparam(\"t<caret>\", \"x\", 1)\n")
        assertContainsElements(items, "tm")
    }

    fun testModparamNamesFilteredByModule() {
        val items = completions("#!KAMAILIO\nmodparam(\"tm\", \"fr_<caret>\", 30000)\n")
        assertContainsElements(items, "fr_timer")
        assertDoesntContain(items, "early_media") // acc parameter
    }

    fun testPseudovarInCode() {
        val items = completions("#!KAMAILIO\nrequest_route {\n    \$var(x) = \$r<caret>;\n}\n")
        assertContainsElements(items, "rU", "ru")
    }

    fun testPseudovarInString() {
        val items = completions("#!KAMAILIO\nrequest_route {\n    xlog(\"src=\$s<caret>\");\n}\n")
        assertContainsElements(items, "si", "sp")
    }

    fun testTransformation() {
        val items = completions("#!KAMAILIO\nrequest_route {\n    \$var(x) = \$(rU{s.<caret>});\n}\n")
        assertContainsElements(items, "s.len", "s.int")
    }

    /** Chooses the first item when a lookup pops up; a unique match is auto-inserted by the fixture. */
    private fun completeFirst(text: String) {
        myFixture.configureByText("kamailio.cfg", text)
        if (myFixture.completeBasic() != null) myFixture.type('\n')
    }

    fun testRouteNamesViaReference() {
        completeFirst("#!KAMAILIO\nrequest_route {\n    route(REL<caret>);\n}\nroute[RELAY] {\n    exit;\n}\n")
        assertTrue(myFixture.file.text.contains("route(RELAY);"))
    }

    fun testFunctionInsertsParens() {
        completeFirst("#!KAMAILIO\nrequest_route {\n    t_check_tran<caret>\n}\n")
        assertTrue(myFixture.file.text.contains("t_check_trans()"))
    }

    fun testGlobalParamInsertsAssign() {
        completeFirst("#!KAMAILIO\nchildre<caret>\n")
        assertTrue(myFixture.file.text.contains("children="))
    }
}
