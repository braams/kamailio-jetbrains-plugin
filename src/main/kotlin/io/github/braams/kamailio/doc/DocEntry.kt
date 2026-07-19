package io.github.braams.kamailio.doc

/**
 * One documentation entry. [doc] is HTML unless [markdown] is set, in which case it is Markdown and
 * gets converted to HTML at render time (see `KamailioDocumentationTargetProvider`).
 */
data class DocEntry(
    val category: KamailioDocCategory,
    val name: String,
    val module: String? = null,
    val syntax: String? = null,
    val doc: String = "",
    val markdown: Boolean = false
)
