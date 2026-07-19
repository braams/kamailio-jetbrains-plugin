# Building an IntelliJ Platform plugin for a custom config language — field notes

Distilled from building a language plugin (lexer → parser → references → formatter → docs →
completion) end to end. Nothing here is specific to the source language; carry it into the next
config-language plugin. Many of these were learned the hard way — the "why" notes are the value.

## Project setup

- Gradle plugins: `org.jetbrains.intellij.platform` 2.x + `org.jetbrains.grammarkit` + Kotlin JVM.
  Target the IDE's JVM (2025.1 → JVM 21, Kotlin 2.1).
- `kotlin.stdlib.default.dependency=false` in `gradle.properties` — the IDE provides the stdlib;
  bundling your own bloats the zip and risks conflicts.
- Gradle configuration cache and build cache work fine with this stack; enable both.
- `sinceBuild` from `pluginConfiguration.ideaVersion`; set `untilBuild = provider { null }` for an
  open upper range (fine while you only use stable platform APIs). Without it, plugin 2.x defaults
  to a single-major-version range.
- Key tasks: `runIde` (sandbox log: `build/idea-sandbox/<IDE>/system/log/idea.log`), `buildPlugin`
  (zip in `build/distributions/`), `verifyPlugin`, `generateLexer` / `generateParser`.
- `verifyPlugin` needs `pluginVerification { ides { ... } }`. `recommended()` may select an IDE
  version that fails to resolve as a download; pinning explicit versions
  (`ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")`) is more reliable.
- Wire generated sources: `sourceSets["main"].java.srcDirs("src/main/gen")`, gitignore
  `src/main/gen/`, and make compile tasks `dependsOn(generateLexer, generateParser)`.
- Depend only on `com.intellij.modules.platform` → the plugin runs in every IntelliJ-based IDE.
- Prefer extension points over components/services with state → the verifier marks the plugin
  dynamic ("can be enabled/disabled without restart").

## Marketplace / publishing

- **Plugin ID can never change** after the first Marketplace upload. Display name also locks in.
- The first version of a new plugin must be uploaded **manually** via the web form
  (plugins.jetbrains.com/plugin/uploadPlugin); `publishPlugin` only works for later updates.
  Moderation takes ~2 business days.
- Configure `publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }` up front;
  publish updates with `PUBLISH_TOKEN=... ./gradlew publishPlugin`.
- `<vendor>` name in plugin.xml should match the Marketplace vendor profile name, or moderation
  asks questions. An individual publishing a free OSS plugin is a "non-trader".
- Icons:
  - `META-INF/pluginIcon.svg` — 40×40 (`width`/`height` attributes), plus optional
    `pluginIcon_dark.svg`.
  - File-type icons — 16×16 SVG. **`IconLoader` uses the `width`/`height` attributes for the
    logical size**; an SVG with only a large `viewBox` renders enormous. Always set
    `width="16" height="16"`. A `_dark` suffix variant is picked up automatically.
- GitHub push protection can block pushes over secret-*looking* strings inside bundled data files
  (e.g. a Slack webhook URL in documentation examples, even an obvious placeholder). Scrub such
  strings at data-generation time.
- Experimental-API warnings from the verifier (e.g. `DocumentationTarget`-related classes) don't
  block publication.

## Lexer + parser (JFlex + Grammar-Kit)

- Put the context-sensitive pain into **lexer states**, not the parser: preprocessor-directive
  lines, string interiors with interpolation, odd name syntaxes. Keep a state *stack*
  (`pushState`/`popState`) and **clear it in the adapter's `start()`** — highlighting relexes from
  arbitrary offsets and stale state corrupts everything after the first edit.
- **Do not introduce an EOL token.** Newlines must be ordinary whitespace. A token containing
  `\n` breaks the formatting engine in non-obvious ways (spacing around it, indent computation).
  For line-bounded constructs (preprocessor directives), use an external parser predicate
  (`parserUtilClass` method that checks "still on the same line" via the token text between
  offsets) instead.
- String interpolation: lex the whole interpolated variable inside a string as **one flat token**
  (e.g. `$name`, `$name(...)`). Simple and robust — but remember every feature that touches it
  (hover, completion) must re-parse that token's text manually.
- Grammar-Kit `recoverWhile`: the stop-token set must guarantee **progress**. If a token that can
  begin arbitrary garbage (a bare identifier) is a stop token, recovery stops immediately, the
  file-level loop aborts *silently*, and everything after the bad region loses PSI structure while
  the file still "parses". Use lookahead patterns (`IDENT (EQ | DOT)`) so recovery only stops at
  plausible statement starts. Write an integration test asserting structure *after* the ugliest
  region of a real config.
- `tokens=[...]` debug names are the token **text** (`'#!define'`, `'"'`), and that text is what
  lexer tests see as the token name — not the constant names.
- PSI mixins: `mixin="..."` attribute per rule; mixin property names must not collide with
  Grammar-Kit-generated child accessors (`paramNameText` vs generated `getParamName()`).
- **Contributed references only work if the PSI base class overrides `getReferences()`** to call
  `ReferenceProvidersRegistry.getReferencesFromProviders(this)`. Set `extends=` in the BNF so all
  generated PSI inherits that base — otherwise `ReferenceContributor` registrations silently do
  nothing.

## References / resolve

- `PsiReferenceContributor` + `PsiPolyVariantReferenceBase`. Implement `getVariants()` — the
  platform's legacy completion contributor turns variants into code completion for free.
- Soft references (`soft = true`) for names that may legitimately be undefined — unresolved soft
  references aren't highlighted as errors, but still navigate.
- Cross-file resolve without stubs: walk an include graph (parse include directives, cache with
  `CachedValuesManager` keyed on PSI modification count). Limitation: definitions in the
  *including* file aren't visible from included files. StubIndex is the proper fix; plan for it.

## Formatter

- One `Block` implementation over the AST + a `SpacingBuilder`. Scope every spacing rule with
  `aroundInside`/`betweenInside` so rules never reach into constructs where whitespace changes
  runtime semantics (interpolations, transformations); format punctuation only inside known
  containers (argument lists).
- Parser-recovery output (`PsiErrorElement`, Grammar-Kit `DUMMY_BLOCK`) is opaque to the
  formatter. Detect such nodes, expose them as **leaf blocks**, and return
  `Spacing.getReadOnlySpacing()` around them so broken/foreign regions stay byte-for-byte intact.
- "This construct must be at column 0" (preprocessor directives): do it in a
  `PostFormatProcessor` *after* normal formatting. Giving those nodes
  `Indent.getAbsoluteNoneIndent()` inside the block tree derails sibling indentation.
- Defaults: set language defaults in `customizeDefaults` of the
  `LanguageCodeStyleSettingsProvider`, and expose rules through the **standard** IntelliJ
  checkboxes (`customizeSettings` + SPACING_SETTINGS) instead of custom settings where possible.
- The single most valuable formatter test: reformat a large real-world reference config and
  assert the result is **idempotent** (formatting twice = formatting once), plus golden-file
  comparison.

## Hover documentation

- Modern API: `platform.backend.documentation.targetProvider` →
  `DocumentationTargetProvider` / `DocumentationTarget` (needs a `Pointer` via
  `SmartPointerManager`). Still marked experimental; fine in practice.
- Walk up from `file.findElementAt(offset)` and decide the doc category from the PSI ancestor
  (which named element contains the leaf). Order matters when categories overlap: structural
  matches first, then local definitions (routes/constants via their references), then bare-token
  fallbacks — so generic docs never shadow real navigation targets.
- **Markdown rendering is built into the platform**:
  `com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter.convert(project, markdownText)`
  (in `lib/app-client.jar`, no extra dependency). It maintains an HTML-tag allowlist, so
  `<placeholder>`-style angle-bracket text in docs survives as text.
- Doc HTML skeleton: `<div class='definition'><pre>signature</pre></div>` +
  `<div class='content'>...</div>` + `<div class='bottom'><i>origin</i></div>`.
- Ship the doc database as JSON resources, lazy-load on first use, and expose a custom extension
  point so an external/updated database can override the bundled one. A convenient convention for
  Markdown entries: the first `###` heading (usually the signature / value type) becomes the popup
  definition line; the rest is the body.

## Completion

- One `CompletionContributor` deciding context from the **position token type** (identifier vs
  in-string tokens) plus PSI ancestors is simpler and more debuggable than pattern DSL stacks.
- Set `context.dummyIdentifier = DUMMY_IDENTIFIER_TRIMMED` in `beforeCompletion` — the default
  dummy contains a semicolon that can break parsing of line-oriented languages.
- Where references already provide variants (names of user-defined things), **return early** —
  otherwise your database items drown out the few relevant local names.
- Custom `withPrefixMatcher(...)` where the default alphanumeric-prefix heuristic fails: dotted
  names (`s.len`), names typed inside a flat interpolation token (prefix = text between the sigil
  and the caret).
- Niceties that cost little: `ParenthesesInsertHandler.getInstance(hasParams)` for functions
  (decide `hasParams` from the signature text), an insert handler adding `=` for key–value
  parameters, `withTypeText` for the origin (module/category), `AllIcons.Nodes.*` icons.
- Test gotcha: with a single matching variant `completeBasic()` **auto-inserts and returns
  null** — tests must handle both "lookup shown" and "auto-inserted" paths.

## Inspections & highlighting

- `localInspection` registrations require a description HTML file per inspection:
  `resources/inspectionDescriptions/<ShortName>.html`.
- For languages with preprocessor conditionals: compute inactive regions once (cached) and use a
  `HighlightErrorFilter` to suppress parse errors inside inactive regions instead of trying to
  parse them properly. (Same data can later drive dimming via an annotator.)
- An `Annotator` complements lexer-based highlighting for context-dependent colors.

## Testing

- `BasePlatformTestCase` covers almost everything: `myFixture.configureByText` with `<caret>`
  markup, `completeBasic()`, direct calls into providers
  (`provider.documentationTargets(myFixture.file, myFixture.caretOffset)`).
- Lexer tests: feed strings to the adapter, compare token-name sequences.
- Keep a real-world reference config as the test fixture; assert zero parse errors outside known
  regions AND presence of deep structure after tricky regions (catches silent recovery aborts).
- `println` from tests lands in the HTML report (`build/reports/tests/.../classes/*.html`), not
  in the Gradle console — useful for eyeballing rendered doc HTML.
- Only one `<caret>` per fixture text — a second one silently changes which offset is used.

## Odds and ends

- Content-based file detection: `fileTypeDetector` reading the first bytes for a marker line, in
  addition to filename `patterns` on the `fileType` registration.
- `IconLoader.getIcon("/icons/x.svg", javaClass)` in an `Icons` object with `@JvmField`.
- Structure view, folding, brace matcher, commenter are each ~50-line implementations that make
  the plugin feel complete; do them early, they're cheap.
- A documentation database doubles as the completion dictionary and as the ground truth for
  "unknown name" inspections — design its access API (`lookup` + `entries`) for all three from
  the start.
