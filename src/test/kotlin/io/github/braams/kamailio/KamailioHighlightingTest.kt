package io.github.braams.kamailio

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.braams.kamailio.highlighting.KamailioColors
import io.github.braams.kamailio.psi.KamailioPvKey
import io.github.braams.kamailio.psi.KamailioRouteCall
import io.github.braams.kamailio.psi.KamailioTransArg
import io.github.braams.kamailio.psi.KamailioTypes

/**
 * `kw_as_name_` (Kamailio.bnf) lets keyword tokens act as plain names inside a pv key or a
 * transformation argument. `KamailioParserUtil.asName` remaps the token to IDENT in the *parsed*
 * tree (fixing hover, hover/reference-adjacent tooling), but the base syntax highlighter
 * (`KamailioSyntaxHighlighter`) re-lexes the document independently of parsing and keeps reporting
 * the original keyword token type for that text — so `KamailioAnnotator` must additionally force
 * identifier coloring for these positions. Plain lexer-based coloring never shows up in
 * `myFixture.doHighlighting()` output (only annotator/inspection-produced `HighlightInfo` does),
 * so the annotator override is what we can actually assert here.
 */
class KamailioHighlightingTest : BasePlatformTestCase() {

    private fun annotationsAt(text: String, marker: String): List<HighlightInfo> {
        myFixture.configureByText("kamailio.cfg", text)
        // marker's *last* occurrence locates the target ("route" also appears in "request_route")
        val offset = text.lastIndexOf(marker) + marker.length / 2
        check(offset >= marker.length / 2) { "marker not found: $marker" }
        return myFixture.doHighlighting(HighlightSeverity.INFORMATION)
            .filter { it.startOffset <= offset && offset < it.endOffset }
    }

    fun testRouteAsPvKeyIsRemappedToIdent() {
        myFixture.configureByText("kamailio.cfg", "#!KAMAILIO\nrequest_route {\n    \$var(route) = 1;\n}\n")
        val key = PsiTreeUtil.findChildOfType(myFixture.file, KamailioPvKey::class.java)!!
        assertEquals("route", key.text)
        assertEquals(KamailioTypes.IDENT, key.firstChild.node.elementType)
    }

    fun testRouteAsTransformationArgIsRemappedToIdent() {
        myFixture.configureByText(
            "kamailio.cfg", "#!KAMAILIO\nrequest_route {\n    \$var(x) = \$(rU{s.select,route});\n}\n"
        )
        val arg = PsiTreeUtil.findChildOfType(myFixture.file, KamailioTransArg::class.java)!!
        assertEquals("route", arg.text)
        assertEquals(KamailioTypes.IDENT, arg.firstChild.node.elementType)
    }

    fun testRouteKeywordInRouteCallIsNotRemapped() {
        myFixture.configureByText("kamailio.cfg", "#!KAMAILIO\nrequest_route {\n    route(RELAY);\n}\n")
        val call = PsiTreeUtil.findChildOfType(myFixture.file, KamailioRouteCall::class.java)!!
        assertEquals(KamailioTypes.ROUTE_KW, call.firstChild.node.elementType)
    }

    fun testRouteAsPvKeyIsRecoloredAsIdentifier() {
        val infos = annotationsAt("#!KAMAILIO\nrequest_route {\n    \$var(route) = 1;\n}\n", "route")
        val expected = EditorColorsManager.getInstance().globalScheme.getAttributes(KamailioColors.IDENTIFIER)
        assertTrue(infos.any { it.forcedTextAttributes == expected })
    }

    fun testRouteAsTransformationArgIsRecoloredAsIdentifier() {
        val infos = annotationsAt(
            "#!KAMAILIO\nrequest_route {\n    \$var(x) = \$(rU{s.select,route});\n}\n", "route"
        )
        val expected = EditorColorsManager.getInstance().globalScheme.getAttributes(KamailioColors.IDENTIFIER)
        assertTrue(infos.any { it.forcedTextAttributes == expected })
    }

    fun testRouteKeywordInRouteCallIsNotRecolored() {
        // the "route" in route(RELAY) is a real keyword usage; the annotator must not touch it
        val infos = annotationsAt("#!KAMAILIO\nrequest_route {\n    route(RELAY);\n}\n", "route(")
        assertEmpty(infos.filter { it.forcedTextAttributes != null || it.forcedTextAttributesKey != null })
    }
}
