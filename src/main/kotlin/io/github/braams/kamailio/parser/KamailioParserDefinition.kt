package io.github.braams.kamailio.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.github.braams.kamailio.KamailioLanguage
import io.github.braams.kamailio.lexer.KamailioLexerAdapter
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioTokenSets
import io.github.braams.kamailio.psi.KamailioTypes

class KamailioParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = KamailioLexerAdapter()
    override fun createParser(project: Project?): PsiParser = KamailioParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = KamailioTokenSets.COMMENTS
    override fun getStringLiteralElements(): TokenSet = KamailioTokenSets.STRINGS
    override fun createElement(node: ASTNode): PsiElement = KamailioTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = KamailioFile(viewProvider)

    companion object {
        @JvmField
        val FILE = IFileElementType(KamailioLanguage)
    }
}
