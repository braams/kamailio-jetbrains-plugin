package io.github.braams.kamailio.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import io.github.braams.kamailio.KamailioFileType
import io.github.braams.kamailio.KamailioLanguage

class KamailioFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, KamailioLanguage) {
    override fun getFileType(): FileType = KamailioFileType.INSTANCE
    override fun toString(): String = "Kamailio config file"
}
