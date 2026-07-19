package io.github.braams.kamailio.formatting

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import io.github.braams.kamailio.psi.KamailioFile
import io.github.braams.kamailio.psi.KamailioTypes

class KamailioBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        // recovery output (error elements, Grammar-Kit dummy blocks — e.g. raw SQL inside an
        // inactive #!ifdef) is opaque: reformatting must not restructure text the parser
        // could not understand
        if (isRecoveryNode(myNode)) return emptyList()
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE && child.textLength > 0) {
                blocks += KamailioBlock(child, null, null, spacingBuilder)
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent {
        val type = myNode.elementType
        val parentType = myNode.treeParent?.elementType ?: return Indent.getNoneIndent()

        // Preprocessor directives are indented like regular statements here;
        // KamailioPostFormatProcessor pins their lines to column 0 afterwards
        // (absolute indents derail the indent calculation of the following siblings).
        return when {
            parentType == KamailioTypes.BLOCK &&
                type != KamailioTypes.LBRACE && type != KamailioTypes.RBRACE ->
                Indent.getNormalIndent()

            parentType == KamailioTypes.SWITCH_STMT && type == KamailioTypes.SWITCH_CASE ->
                Indent.getNormalIndent()

            (parentType == KamailioTypes.CASE_CLAUSE || parentType == KamailioTypes.DEFAULT_CLAUSE) &&
                (type == KamailioTypes.STATEMENT ||
                    type == KamailioTypes.LINE_COMMENT || type == KamailioTypes.BLOCK_COMMENT) ->
                Indent.getNormalIndent()

            // wrapped parts of an implicit string concatenation (multi-line modparam values)
            parentType == KamailioTypes.STRING_VALUE ->
                Indent.getContinuationIndent()

            else -> Indent.getNoneIndent()
        }
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        // parser-recovery garbage is not our code to restyle — freeze the whitespace around it
        if (isRecoveryBlock(child1) || isRecoveryBlock(child2)) return Spacing.getReadOnlySpacing()
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    private fun isRecoveryBlock(block: Block?): Boolean {
        val node = (block as? KamailioBlock)?.node ?: return false
        return isRecoveryNode(node)
    }

    private fun isRecoveryNode(node: ASTNode): Boolean {
        if (node.psi is PsiErrorElement) return true
        if (node.elementType == GeneratedParserUtilBase.DUMMY_BLOCK) return true
        // raw token leaves directly under the file (only recovery produces those)
        val parent = node.treeParent ?: return false
        return parent.psi is KamailioFile && node.firstChildNode == null &&
            node.elementType != KamailioTypes.LINE_COMMENT && node.elementType != KamailioTypes.BLOCK_COMMENT
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = when (myNode.elementType) {
        KamailioTypes.BLOCK, KamailioTypes.SWITCH_STMT,
        KamailioTypes.CASE_CLAUSE, KamailioTypes.DEFAULT_CLAUSE ->
            ChildAttributes(Indent.getNormalIndent(), null)
        else -> ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null || isRecoveryNode(myNode)
}
