package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioPvKey
import io.github.braams.kamailio.psi.KamailioPvPlain

open class KamailioPvMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    private val outerPvPlain: KamailioPvPlain?
        // findChildOfType is depth-first, so for both $var(x) and $(ru{s.len}) the outermost pv_plain wins
        get() = PsiTreeUtil.findChildOfType(this, KamailioPvPlain::class.java)

    /** "ru" for $ru, "var" for $var(x), "hdr" for $hdr(CSeq), "ru" for $(ru{s.len}) */
    val pvName: String?
        get() = outerPvPlain?.identifier?.text

    val pvNameElement: PsiElement?
        get() = outerPvPlain?.identifier

    /** "x" for $var(x), "ipban=>$si" for $sht(ipban=>$si); null for $ru */
    val keyText: String?
        get() = PsiTreeUtil.findChildOfType(this, KamailioPvKey::class.java)?.text
}
