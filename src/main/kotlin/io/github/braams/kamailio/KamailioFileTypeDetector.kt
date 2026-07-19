package io.github.braams.kamailio

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile

/**
 * Claims `*.cfg` files whose content starts with a Kamailio shebang (`#!KAMAILIO` etc.).
 * Files named `kamailio*.cfg` are matched by the fileType pattern instead and never reach this detector.
 */
class KamailioFileTypeDetector : FileTypeRegistry.FileTypeDetector {

    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? {
        if (!file.name.endsWith(".cfg", ignoreCase = true)) return null
        val text = firstCharsIfText ?: return null
        return if (SHEBANGS.any { text.startsWith(it) }) KamailioFileType.INSTANCE else null
    }

    override fun getDesiredContentPrefixLength(): Int = 16

    private companion object {
        val SHEBANGS = listOf("#!KAMAILIO", "#!OPENSER", "#!SER", "#!MAXCOMPAT", "#!ALL")
    }
}
