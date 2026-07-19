package io.github.braams.kamailio

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.braams.kamailio.doc.KamailioDocCategory
import io.github.braams.kamailio.doc.KamailioDocService
import io.github.braams.kamailio.doc.KamailioDocumentationTargetProvider

class KamailioDocumentationTest : BasePlatformTestCase() {

    fun testBundledDocServiceLookup() {
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.FUNCTION, "t_relay"))
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.FUNCTION, "force_rport")) // core function
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.GLOBAL_PARAM, "debug"))
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.MODPARAM, "fr_timer", "tm"))
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.MODPARAM, "db_url", null)) // unresolved module
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.PSEUDOVAR, "ru"))
        assertNull(KamailioDocService.lookup(KamailioDocCategory.PSEUDOVAR, "ru")!!.module) // core pv
        assertEquals("htable", KamailioDocService.lookup(KamailioDocCategory.PSEUDOVAR, "sht")!!.module)
        assertEquals("dialog", KamailioDocService.lookup(KamailioDocCategory.PSEUDOVAR, "dlg")!!.module)
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.TRANSFORMATION, "s.len"))
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.KEYWORD, "myself"))
        assertNotNull(KamailioDocService.lookup(KamailioDocCategory.MODULE, "tm"))
        assertNull(KamailioDocService.lookup(KamailioDocCategory.FUNCTION, "no_such_function"))
        assertNull(KamailioDocService.lookup(KamailioDocCategory.MODPARAM, "no_such_param", "tm"))
    }

    fun testHeadingBecomesSyntaxLine() {
        val entry = KamailioDocService.lookup(KamailioDocCategory.FUNCTION, "t_relay")!!
        assertTrue(entry.markdown)
        assertEquals("t_relay([host, port])", entry.syntax)
        assertFalse("body must not repeat the heading", entry.doc.startsWith("#"))
    }

    fun testHoverTargets() {
        val provider = KamailioDocumentationTargetProvider()

        fun targetsAt(text: String): Boolean {
            myFixture.configureByText("kamailio.cfg", text)
            return provider.documentationTargets(myFixture.file, myFixture.caretOffset).isNotEmpty()
        }

        assertTrue("function doc", targetsAt("#!KAMAILIO\nrequest_route {\n    t_re<caret>lay();\n}\n"))
        assertTrue("global param doc", targetsAt("#!KAMAILIO\nde<caret>bug=3\n"))
        assertTrue("modparam doc", targetsAt("#!KAMAILIO\nloadmodule \"tm.so\"\nmodparam(\"tm\", \"fr_<caret>timer\", 30000)\n"))
        assertTrue("pseudovar doc", targetsAt("#!KAMAILIO\nrequest_route {\n    \$var(x) = \$r<caret>U;\n}\n"))
        assertTrue(
            "transformation doc",
            targetsAt("#!KAMAILIO\nrequest_route {\n    \$var(x) = \$(rU{s.l<caret>en});\n}\n")
        )
        assertTrue(
            "pv in double-quoted string",
            targetsAt("#!KAMAILIO\nrequest_route {\n    dlgs_init(\"\$f<caret>u\", \"\$tu\", \"srcip=\$si\");\n}\n")
        )
        assertTrue(
            "pv mid-string",
            targetsAt("#!KAMAILIO\nrequest_route {\n    dlgs_init(\"\$fu\", \"\$tu\", \"srcip=\$s<caret>i\");\n}\n")
        )
        assertTrue(
            "pv with key in string",
            targetsAt("#!KAMAILIO\nrequest_route {\n    xlog(\"v=\$va<caret>r(x)\");\n}\n")
        )
        assertTrue(
            "transformation in string",
            targetsAt("#!KAMAILIO\nrequest_route {\n    xlog(\"len=\$(ru{s.l<caret>en})\");\n}\n")
        )
        assertTrue("module overview doc", targetsAt("#!KAMAILIO\nloadmodule \"t<caret>m.so\"\n"))
        assertTrue(
            "module name in modparam",
            targetsAt("#!KAMAILIO\nloadmodule \"tm.so\"\nmodparam(\"t<caret>m\", \"fr_timer\", 30000)\n")
        )
        myFixture.configureByText(
            "kamailio.cfg",
            "#!KAMAILIO\nmodparam(\"nathelper|regis<caret>trar\", \"received_avp\", \"\$avp(RECEIVED)\")\n"
        )
        val multi = provider.documentationTargets(myFixture.file, myFixture.caretOffset).single()
        assertEquals("second module of multi-module modparam", "registrar", multi.computePresentation().presentableText)
        assertTrue(
            "keyword doc",
            targetsAt("#!KAMAILIO\nrequest_route {\n    if (uri == mys<caret>elf) {\n        exit;\n    }\n}\n")
        )
        assertTrue(
            "route local doc",
            targetsAt("#!KAMAILIO\nrequest_route {\n    route(REL<caret>AY);\n}\nroute[RELAY] {\n    exit;\n}\n")
        )
        assertTrue(
            "define local doc",
            targetsAt("#!KAMAILIO\n#!define DBGLEVEL 3\ndebug=DBGLE<caret>VEL\n")
        )
    }
}
