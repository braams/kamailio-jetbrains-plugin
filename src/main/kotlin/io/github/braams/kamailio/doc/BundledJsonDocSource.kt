package io.github.braams.kamailio.doc

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Reads the bundled documentation database generated from the official Kamailio docs:
 * - `/docs/core.json` — `{"core": {overview, global_parameters, functions, keywords, pseudovariables,
 *   transformations}}`;
 * - `/docs/modules.json` — module name -> `{overview, parameters, functions, pseudovariables,
 *   transformations}`.
 *
 * Every entry is an object `{"doc": <markdown>, "aliases": [...], "type": ..., "variants": [...]}`;
 * pseudo-variables and transformations live in the module that exports them, so module attribution
 * is explicit. The first `###` heading of the Markdown (usually the signature / value type) becomes
 * the [DocEntry.syntax] line. Aliases are registered as separate keys pointing to a renamed copy of
 * the entry.
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

        readJson("/docs/core.json")?.getAsJsonObject("core")?.let { core ->
            core.section("global_parameters") { name, o ->
                putWithAliases(db.globalParams, KamailioDocCategory.GLOBAL_PARAM, name, null, o)
            }
            core.section("functions") { name, o ->
                putWithAliases(db.functions, KamailioDocCategory.FUNCTION, name, null, o)
            }
            core.section("keywords") { name, o ->
                putWithAliases(db.keywords, KamailioDocCategory.KEYWORD, name, null, o)
            }
            core.section("pseudovariables") { name, o ->
                putWithAliases(db.pseudovars, KamailioDocCategory.PSEUDOVAR, name.removePrefix("$"), null, o)
            }
            core.section("transformations") { name, o ->
                putWithAliases(db.transformations, KamailioDocCategory.TRANSFORMATION, name, null, o)
            }
        }

        readJson("/docs/modules.json")?.let { mods ->
            for ((moduleName, moduleEl) in mods.entrySet()) {
                val module = moduleEl as? JsonObject ?: continue
                val overview = module.get("overview")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                db.modules[moduleName] = entry(KamailioDocCategory.MODULE, moduleName, null, overview)
                module.section("parameters") { name, o ->
                    val e = entryOf(KamailioDocCategory.MODPARAM, name, moduleName, o)
                    db.modparams[modparamKey(moduleName, name)] = e
                    db.modparamsByName.putIfAbsent(name, e)
                }
                // core wins name clashes, then the first module (rare, e.g. tls vs tls_wolfssl)
                module.section("functions") { name, o ->
                    putWithAliases(db.functions, KamailioDocCategory.FUNCTION, name, moduleName, o)
                }
                module.section("pseudovariables") { name, o ->
                    putWithAliases(db.pseudovars, KamailioDocCategory.PSEUDOVAR, name.removePrefix("$"), moduleName, o)
                }
                module.section("transformations") { name, o ->
                    putWithAliases(db.transformations, KamailioDocCategory.TRANSFORMATION, name, moduleName, o)
                }
            }
        }
        return db
    }

    /** Registers the entry under its name and, as renamed copies, under each of its aliases. */
    private fun putWithAliases(
        map: MutableMap<String, DocEntry>,
        category: KamailioDocCategory,
        name: String,
        module: String?,
        o: JsonObject
    ) {
        val e = entryOf(category, name, module, o)
        map.putIfAbsent(name, e)
        o.getAsJsonArray("aliases")?.forEach { alias ->
            if (alias.isJsonPrimitive) map.putIfAbsent(alias.asString, e.copy(name = alias.asString))
        }
    }

    private fun readJson(path: String): JsonObject? {
        val stream = javaClass.getResourceAsStream(path) ?: return null
        return stream.reader().use { JsonParser.parseReader(it) as? JsonObject }
    }

    private inline fun JsonObject.section(name: String, handle: (String, JsonObject) -> Unit) {
        val obj = get(name) as? JsonObject ?: return
        for ((key, value) in obj.entrySet()) {
            (value as? JsonObject)?.let { handle(key, it) }
        }
    }

    private fun modparamKey(module: String, name: String) = "$module $name"

    /** Name-only entries (blank doc) are kept: completion needs the names; hover skips them. */
    private fun entryOf(category: KamailioDocCategory, name: String, module: String?, o: JsonObject): DocEntry {
        val doc = o.get("doc")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        return entry(category, name, module, doc)
    }

    private fun entry(category: KamailioDocCategory, name: String, module: String?, md: String): DocEntry {
        val text = md.replace("\u200B", "").trim()
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        if (!firstLine.startsWith("#")) return DocEntry(category, name, module, null, text, markdown = true)
        val syntax = firstLine.trimStart('#').trim().replace("`", "")
        val body = text.substringAfter('\n', "").trim()
        return DocEntry(category, name, module, syntax.ifEmpty { null }, body, markdown = true)
    }
}
