package io.github.braams.kamailio.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object KamailioPsiUtil {

    /** Content of a string_literal (between the quotes) or SQ_STRING (quotes stripped). */
    fun stringLiteralContent(element: PsiElement): String {
        if (element.node.elementType == KamailioTypes.SQ_STRING) {
            return element.text.removeSurrounding("'")
        }
        val sb = StringBuilder()
        var child = element.firstChild
        while (child != null) {
            val type = child.node.elementType
            if (type == KamailioTypes.STRING_TEXT || type == KamailioTypes.STRING_INTERP) {
                sb.append(child.text)
            }
            child = child.nextSibling
        }
        return sb.toString()
    }

    /** Concatenated content of a string_value (handles implicit adjacent-string concatenation). */
    fun stringValueContent(stringValue: PsiElement): String {
        val sb = StringBuilder()
        var child = stringValue.firstChild
        while (child != null) {
            val type = child.node.elementType
            if (type == KamailioTypes.STRING_LITERAL || type == KamailioTypes.SQ_STRING) {
                sb.append(stringLiteralContent(child))
            }
            child = child.nextSibling
        }
        return sb.toString()
    }

    inline fun <reified T : PsiElement> childOfType(element: PsiElement): T? =
        PsiTreeUtil.getChildOfType(element, T::class.java)
}
