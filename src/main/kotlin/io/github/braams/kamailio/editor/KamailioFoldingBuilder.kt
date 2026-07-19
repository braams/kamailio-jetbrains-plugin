package io.github.braams.kamailio.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioBlock
import io.github.braams.kamailio.psi.KamailioPpDirective
import io.github.braams.kamailio.psi.KamailioRouteDef
import io.github.braams.kamailio.psi.KamailioTypes
import io.github.braams.kamailio.psi.impl.KamailioRouteDefMixin

class KamailioFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        for (routeDef in PsiTreeUtil.findChildrenOfType(root, KamailioRouteDef::class.java)) {
            val block = PsiTreeUtil.getChildOfType(routeDef, KamailioBlock::class.java) ?: continue
            if (block.textLength > 2) {
                descriptors += FoldingDescriptor(block.node, block.textRange)
            }
        }

        PsiTreeUtil.processElements(root) { el ->
            if (el.node.elementType == KamailioTypes.BLOCK_COMMENT && el.textContains('\n')) {
                descriptors += FoldingDescriptor(el.node, el.textRange)
            }
            true
        }

        // #!ifdef ... #!endif regions (flat directives matched with a stack)
        val stack = ArrayDeque<KamailioPpDirective>()
        for (dir in PsiTreeUtil.findChildrenOfType(root, KamailioPpDirective::class.java)) {
            when (dir.node.firstChildNode?.firstChildNode?.elementType) {
                KamailioTypes.PP_IFDEF_KW, KamailioTypes.PP_IFNDEF_KW, KamailioTypes.PP_IFEXP_KW ->
                    stack.addLast(dir)
                KamailioTypes.PP_ENDIF_KW -> {
                    val start = stack.removeLastOrNull() ?: continue
                    val range = TextRange(start.textRange.startOffset, dir.textRange.endOffset)
                    if (document.getLineNumber(range.startOffset) < document.getLineNumber(range.endOffset)) {
                        descriptors += FoldingDescriptor(start.node, range)
                    }
                }
                else -> {}
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        val psi = node.psi
        return when {
            psi is KamailioBlock -> "{...}"
            node.elementType == KamailioTypes.BLOCK_COMMENT -> "/*...*/"
            psi is KamailioPpDirective -> psi.text.lineSequence().first().trim() + " ..."
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
