package io.github.braams.kamailio.doc

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Source of Kamailio documentation entries. The external hints database plugs in later through the
 * `io.github.braams.kamailio.docSource` extension point; [BundledJsonDocSource] is the fallback.
 */
interface KamailioDocSource {

    fun lookup(category: KamailioDocCategory, name: String, module: String? = null): DocEntry?

    /** All entries of a category, for completion. */
    fun entries(category: KamailioDocCategory): Collection<DocEntry> = emptyList()

    companion object {
        val EP_NAME: ExtensionPointName<KamailioDocSource> =
            ExtensionPointName.create("io.github.braams.kamailio.docSource")
    }
}
