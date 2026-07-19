package io.github.braams.kamailio.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import io.github.braams.kamailio.psi.KamailioPsiUtil
import io.github.braams.kamailio.psi.KamailioStringValue

open class KamailioLoadmoduleMixin(node: ASTNode) : KamailioPsiElementBase(node) {

    val modulePathElement: KamailioStringValue?
        get() = PsiTreeUtil.getChildOfType(this, KamailioStringValue::class.java)

    val modulePath: String?
        get() = modulePathElement?.let { KamailioPsiUtil.stringValueContent(it) }

    /** "db_mysql.so" / "/usr/lib/kamailio/modules/tm.so" -> "db_mysql" / "tm" */
    val moduleName: String?
        get() = modulePath?.substringAfterLast('/')?.removeSuffix(".so")?.takeIf { it.isNotBlank() }
}
