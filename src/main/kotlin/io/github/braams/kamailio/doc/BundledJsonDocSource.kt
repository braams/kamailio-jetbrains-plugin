package io.github.braams.kamailio.doc

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Reads the bundled documentation database generated from the official Kamailio docs:
 * - `/docs/core.json` — sections `parameters`, `functions`, `keywords`, `pseudovariables`, `transformations`;
 *   each section maps name -> Markdown text.
 * - `/docs/modules.json` — module name -> `{overview, parameters: {...}, functions: {...}}`, values in Markdown.
 *
 * The first `###` heading of an entry (it usually carries the signature / value type) becomes the
 * [DocEntry.syntax] line; the rest is the Markdown body.
 */
class BundledJsonDocSource : KamailioDocSource {

    private class Db {
        val globalParams = HashMap<String, DocEntry>()
        val keywords = HashMap<String, DocEntry>()
        val pseudovars = HashMap<String, DocEntry>()
        val transformations = HashMap<String, DocEntry>()
        val functions = HashMap<String, DocEntry>()

        /** Keyed "module param"; [modparamsByName] is the fallback when the module is unresolved. */
        val modparams = HashMap<String, DocEntry>()
        val modparamsByName = HashMap<String, DocEntry>()
        val modules = HashMap<String, DocEntry>()
    }

    private val db: Db by lazy { load() }

    override fun lookup(category: KamailioDocCategory, name: String, module: String?): DocEntry? = when (category) {
        KamailioDocCategory.GLOBAL_PARAM -> db.globalParams[name]
        KamailioDocCategory.KEYWORD -> db.keywords[name]
        KamailioDocCategory.PSEUDOVAR -> db.pseudovars[name]
        KamailioDocCategory.TRANSFORMATION -> db.transformations[name]
        KamailioDocCategory.MODULE -> db.modules[name]
        KamailioDocCategory.FUNCTION ->
            db.functions[name]?.takeIf { module == null || it.module == null || it.module == module }
        KamailioDocCategory.MODPARAM ->
            if (module != null) db.modparams[modparamKey(module, name)] else db.modparamsByName[name]
    }

    override fun entries(category: KamailioDocCategory): Collection<DocEntry> = when (category) {
        KamailioDocCategory.GLOBAL_PARAM -> db.globalParams.values
        KamailioDocCategory.KEYWORD -> db.keywords.values
        KamailioDocCategory.PSEUDOVAR -> db.pseudovars.values
        KamailioDocCategory.TRANSFORMATION -> db.transformations.values
        KamailioDocCategory.MODULE -> db.modules.values
        KamailioDocCategory.FUNCTION -> db.functions.values
        KamailioDocCategory.MODPARAM -> db.modparams.values
    }

    private fun load(): Db {
        val db = Db()

        readJson("/docs/core.json")?.let { core ->
            core.section("parameters") { name, md ->
                db.globalParams[name] = entry(KamailioDocCategory.GLOBAL_PARAM, name, null, md)
            }
            core.section("functions") { name, md ->
                db.functions[name] = entry(KamailioDocCategory.FUNCTION, name, null, md)
            }
            core.section("keywords") { name, md ->
                db.keywords[name] = entry(KamailioDocCategory.KEYWORD, name, null, md)
            }
            // pv -> exporting module, recovered from the cookbook's per-module sections (null = core)
            val pvModules = readJson("/docs/pv-modules.json")?.entrySet()
                ?.filter { it.value.isJsonPrimitive }
                ?.associate { it.key to it.value.asString }
                .orEmpty()
            core.section("pseudovariables") { name, md ->
                // keys are mostly bare ("ru"), a few carry the sigil ("$_s") — pvName never includes it
                val bare = name.removePrefix("$")
                db.pseudovars[bare] = entry(KamailioDocCategory.PSEUDOVAR, bare, pvModules[bare], md)
            }
            core.section("transformations") { name, md ->
                db.transformations[name] = entry(KamailioDocCategory.TRANSFORMATION, name, null, md)
            }
        }

        readJson("/docs/modules.json")?.let { mods ->
            for ((moduleName, moduleEl) in mods.entrySet()) {
                val module = moduleEl as? JsonObject ?: continue
                module.get("overview")?.takeIf { it.isJsonPrimitive }?.asString?.let { overview ->
                    db.modules[moduleName] = entry(KamailioDocCategory.MODULE, moduleName, null, overview)
                }
                module.section("parameters") { name, md ->
                    val e = entry(KamailioDocCategory.MODPARAM, name, moduleName, md)
                    db.modparams[modparamKey(moduleName, name)] = e
                    db.modparamsByName.putIfAbsent(name, e)
                }
                module.section("functions") { name, md ->
                    // core functions win, and the first module wins the rare cross-module name clash
                    db.functions.putIfAbsent(name, entry(KamailioDocCategory.FUNCTION, name, moduleName, md))
                }
            }
        }
        return db
    }

    private fun readJson(path: String): JsonObject? {
        val stream = javaClass.getResourceAsStream(path) ?: return null
        return stream.reader().use { JsonParser.parseReader(it) as? JsonObject }
    }

    private inline fun JsonObject.section(name: String, put: (String, String) -> Unit) {
        val obj = get(name) as? JsonObject ?: return
        for ((key, value) in obj.entrySet()) {
            if (value.isJsonPrimitive) put(key, value.asString)
        }
    }

    private fun modparamKey(module: String, name: String) = "$module $name"

    private fun entry(category: KamailioDocCategory, name: String, module: String?, md: String): DocEntry {
        val text = md.replace("\u200B", "").trim()
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        if (!firstLine.startsWith("#")) return DocEntry(category, name, module, null, text, markdown = true)
        val syntax = firstLine.trimStart('#').trim().replace("`", "")
        val body = text.substringAfter('\n', "").trim()
        return DocEntry(category, name, module, syntax.ifEmpty { null }, body, markdown = true)
    }
}
