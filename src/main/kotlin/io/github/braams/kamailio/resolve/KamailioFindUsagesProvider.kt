package io.github.braams.kamailio.resolve

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import io.github.braams.kamailio.lexer.KamailioLexerAdapter
import io.github.braams.kamailio.psi.KamailioNamedElement
import io.github.braams.kamailio.psi.KamailioTokenSets
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.psi.impl.KamailioPpDefineMixin
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin

class KamailioFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        KamailioLexerAdapter(),
        TokenSet.create(KamailioTypes.IDENT, KamailioTypes.EVENT_NAME),
        KamailioTokenSets.COMMENTS,
        KamailioTokenSets.STRINGS
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement is KamailioNamedElement

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is KamailioRouteDefMixin -> "route"
        is KamailioPpDefineMixin -> "define"
        else -> "element"
    }

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? KamailioNamedElement)?.name ?: element.text.take(30)

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = getDescriptiveName(element)
}
