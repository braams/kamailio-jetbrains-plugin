package io.github.braams.kamailio.doc

/** Queries docSource extensions first (the external hints DB), then the bundled JSON. */
object KamailioDocService {

    private val bundled by lazy { BundledJsonDocSource() }

    fun lookup(category: KamailioDocCategory, name: String, module: String? = null): DocEntry? {
        for (source in KamailioDocSource.EP_NAME.extensionList) {
            source.lookup(category, name, module)?.let { return it }
        }
        return bundled.lookup(category, name, module)
    }
}
