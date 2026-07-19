#   CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

An IntelliJ Platform plugin (Kotlin) for Kamailio SIP server configuration files: syntax highlighting, structure view, go to definition (routes, `#!define` constants, cross-file via `include_file`), formatter, semantic inspections, and hover documentation. `kamailio.cfg` at the repo root is a real Kamailio v6.2 default config used as the reference input; a copy in `src/test/testData/` is the integration-test fixture (must parse with zero errors outside the raw-SQL `ACCDB_COMMENT` ifdef region).

## Build and Run

Uses the IntelliJ Platform Gradle Plugin 2.x (`org.jetbrains.intellij.platform`) plus `org.jetbrains.grammarkit`. Targets IntelliJ IDEA Community 2025.1 (`sinceBuild = 251`), JVM target 21, Kotlin 2.1.

- `./gradlew buildPlugin` — build the plugin distribution (into `build/distributions/`)
- `./gradlew runIde` — launch a sandboxed IDE with the plugin installed (also the "Run Plugin" run configuration; sandbox log at `build/idea-sandbox/system/log/idea.log`)
- `./gradlew test` — run all tests; single class: `./gradlew test --tests "io.github.braams.kamailio.KamailioLexerTest"`
- `./gradlew generateLexer generateParser` — regenerate lexer/parser from `src/main/grammar/` into `src/main/gen/` (gitignored; compile tasks depend on generation automatically)
- `./gradlew verifyPlugin` — run JetBrains plugin verification

Gradle configuration cache and build cache are enabled. The Kotlin stdlib is deliberately not bundled (`kotlin.stdlib.default.dependency=false`) — the IDE provides it; don't add a stdlib dependency.

## Architecture

Package root: `io.github.braams.kamailio`; plugin ID `io.github.braams.kamailio` (must never change once published to Marketplace); Gradle project name `kamailio-jetbrains-plugin`. Language pipeline: `src/main/grammar/Kamailio.flex` (JFlex lexer) + `Kamailio.bnf` (Grammar-Kit) generate parser/PSI into `src/main/gen/`; hand-written PSI mixins live in `psi/impl/` and are wired via `mixin=` attributes in the BNF. Reference grammars for the language: Kamailio's own `src/core/cfg.lex`/`cfg.y` and the tree-sitter-kamailio-cfg project.

Non-obvious constraints, learned the hard way:
- **Lexer states carry the hard parts**: `#` vs `#!` disambiguation, `PP_LINE`/`PP_RAW` for directives, `IN_DQ` for pseudo-variable interpolation inside strings, `EVRT` for `event_route[x:y]` names. The `KamailioLexerAdapter` clears the state stack on `start()` — keep that when touching the lexer.
- **No EOL token.** Newlines are ordinary whitespace everywhere; preprocessor directives are line-bounded by the `<<sameLine>>` predicate in `KamailioParserUtil`. An EOL token containing `\n` breaks the formatting engine in non-obvious ways — don't reintroduce one.
- **References only work because `KamailioPsiElementBase.getReferences()` calls `ReferenceProvidersRegistry`** — the generated PSI must keep extending it (BNF `extends=` attribute).
- **Directives-at-column-0 is done by `KamailioPostFormatProcessor`** after normal formatting; giving them `Indent.getAbsoluteNoneIndent()` in `KamailioBlock` derails sibling indents.
- Mixin property names must not collide with Grammar-Kit-generated child accessors (e.g. `paramNameText` vs generated `getParamName()`).
- In `item_recover`, bare `IDENT` must NOT be a stop token — recovery could make zero progress on identifier-led garbage (the SQL in `ACCDB_COMMENT`), Grammar-Kit then aborts the file loop and the rest of the file silently loses PSI structure. The stop pattern is `IDENT (EQ | DOT)`. The integration test asserts routes after the SQL region exist, specifically to catch this.
- Default code style is tab indentation plus spacing that mirrors the stock config: comparisons and global `name=value` tight, `&&`/`||`/route-level `=`/`+` spaced, `if (`. All spacing rules are wired to standard IntelliJ checkboxes (`customizeSettings` SPACING_SETTINGS) with defaults set in `customizeDefaults` of `KamailioLanguageCodeStyleSettingsProvider`.
- Spacing rules must stay scoped (`aroundInside`/`betweenInside`) so they never touch the inside of pseudo-variables or transformations — whitespace there can change runtime semantics. Commas are formatted only inside `ARG_LIST`/`MODPARAM_STMT`.
- Parser-recovery output (PsiErrorElement, Grammar-Kit `DUMMY_BLOCK`) is opaque to the formatter: leaf blocks + `Spacing.getReadOnlySpacing()` around them (`isRecoveryNode` in `KamailioBlock`), so the SQL garbage in inactive ifdefs is left byte-for-byte intact. The `testReformatOfStockConfigIsIdempotent` test guards the whole formatter.
- Token debug names in lexer tests are the token *text* from the BNF `tokens=[]` block (`#!define`, `"`), not the constant names.
- Cross-file resolve is an include-graph walk (`KamailioIncludeGraph`, cached); definitions in the *including* file are not visible from included files (StubIndex is the planned fix).
- Hover docs: extra sources plug in via the `io.github.braams.kamailio.docSource` extension point (`KamailioDocSource`); the bundled fallback is `resources/docs/core.json` + `modules.json` — Markdown generated from the official Kamailio docs, taken from the author's [vscode-kamailio-hover](https://github.com/braams/vscode-kamailio-hover) project (update them from there) (name → md text; modules keyed module → overview/parameters/functions). `BundledJsonDocSource` turns the leading `###` heading into the syntax line; Markdown bodies are rendered with the platform's `DocMarkdownToHtmlConverter` (in `lib/app-client.jar`, no extra dependency). Category coverage includes MODULE (hover on the `loadmodule` string) and KEYWORD (bare-IDENT fallback tried *after* local route/define resolution so it never shadows references).
- Completion (`KamailioCompletionContributor`) is driven by the same doc DB (`KamailioDocService.entries`). Route names and `#!define` constants are completed by the references' `getVariants` (legacy contributor), so the contributor must return early in those contexts or the doc-DB items drown them out. Context is decided by the position token type (IDENT vs STRING_TEXT/STRING_INTERP) plus PSI ancestors; pv-in-string completion re-parses the flat STRING_INTERP token with a custom prefix matcher.
