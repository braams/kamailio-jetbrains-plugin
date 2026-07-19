package io.github.braams.kamailio

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class KamailioFileType : LanguageFileType(KamailioLanguage) {
    override fun getName(): String = "Kamailio Config"
    override fun getDescription(): String = "Kamailio SIP server configuration"
    override fun getDefaultExtension(): String = "cfg"
    override fun getIcon(): Icon = KamailioIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = KamailioFileType()
    }
}
