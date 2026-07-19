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

    fun entries(category: KamailioDocCategory): Collection<DocEntry> {
        val merged = LinkedHashMap<String, DocEntry>()
        for (source in KamailioDocSource.EP_NAME.extensionList) {
            for (e in source.entries(category)) merged.putIfAbsent("${e.module} ${e.name}", e)
        }
        for (e in bundled.entries(category)) merged.putIfAbsent("${e.module} ${e.name}", e)
        return merged.values
    }
}
