package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import io.github.braams.kamailio.psi.KamailioTypes

open class KamailioPpSubstMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    val isSubstdef: Boolean
        get() = node.firstChildNode?.text?.contains("substdef") == true

    /**
     * For `#!substdef "/NAME/value/"` extracts NAME — the constant such a directive defines.
     */
    val definedName: String?
        get() {
            if (!isSubstdef) return null
            val text = node.findChildByType(KamailioTypes.PP_TEXT)?.text?.trim()?.removeSurrounding("\"") ?: return null
            if (text.length < 2) return null
            val delimiter = text[0]
            val end = text.indexOf(delimiter, 1)
            if (end <= 1) return null
            return text.substring(1, end).takeIf { name -> name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' } }
        }
}
