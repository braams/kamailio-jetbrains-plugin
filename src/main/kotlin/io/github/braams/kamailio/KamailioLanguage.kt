package io.github.braams.kamailio

import com.intellij.lang.Language

object KamailioLanguage : Language("Kamailio") {
    private fun readResolve(): Any = KamailioLanguage
    override fun getDisplayName(): String = "Kamailio"
}
