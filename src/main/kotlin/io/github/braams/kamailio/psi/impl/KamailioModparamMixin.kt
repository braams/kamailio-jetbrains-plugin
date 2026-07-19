package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioModparamValue
import io.github.braams.kamailio.psi.KamailioPsiUtil
import io.github.braams.kamailio.psi.KamailioStringValue

open class KamailioModparamMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    private val stringArgs: List<KamailioStringValue>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, KamailioStringValue::class.java)

    /** `modparam("nathelper|registrar", ...)` declares the param for several modules at once. */
    val moduleNames: List<String>
        get() = stringArgs.getOrNull(0)
            ?.let { KamailioPsiUtil.stringValueContent(it) }
            ?.split('|')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    val paramName: String?
        get() = stringArgs.getOrNull(1)?.let { KamailioPsiUtil.stringValueContent(it) }?.takeIf { it.isNotBlank() }

    val valueElement: KamailioModparamValue?
        get() = PsiTreeUtil.getChildOfType(this, KamailioModparamValue::class.java)

    val moduleNameElement: KamailioStringValue?
        get() = stringArgs.getOrNull(0)

    val paramNameElement: KamailioStringValue?
        get() = stringArgs.getOrNull(1)
}
