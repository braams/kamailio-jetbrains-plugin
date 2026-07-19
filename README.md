# Kamailio Config — JetBrains IDE plugin

An IntelliJ Platform plugin that adds language support for [Kamailio SIP server](https://www.kamailio.org) configuration files (`kamailio.cfg`).

## Features

- **Syntax highlighting** — full lexer for the Kamailio config language: preprocessor directives (`#!define`, `#!ifdef`, `#!substdef`, …), pseudo-variables (including interpolation inside double-quoted strings), transformations, `event_route` names, comments.
- **Hover documentation** — quick doc popups for core parameters, core functions and keywords, module functions, `modparam` parameters, pseudo-variables, transformations, and module overviews (on `loadmodule` and on the module name in `modparam`).
- **Code completion** — core parameters, functions of all bundled modules, module names in `loadmodule`/`modparam`, `modparam` parameter names filtered by module, pseudo-variables (also inside strings), transformations, route names and `#!define` constants.
- **Go to definition** — routes (`route(NAME)` → `route[NAME]`), `#!define` constants, cross-file navigation through `include_file`.
- **Structure view** — routes, defines, module parameters at a glance.
- **Formatter** — tab indentation and spacing that mirror the stock Kamailio config style; preprocessor directives stay at column 0; content of inactive `#!ifdef` regions is left untouched.
- **Inspections** — parser-level syntax checks plus semantic checks on top.
- **File type detection** — `kamailio*.cfg` file name patterns and content-based detection of the `#!KAMAILIO` marker.

## Installation

Build the distribution and install it manually:

```sh
./gradlew buildPlugin
```

Then in the IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the zip from `build/distributions/`.

Requires IntelliJ IDEA 2025.1 or newer; the plugin only depends on the platform, so any IntelliJ-based IDE of build 251+ works.

## Development

- `./gradlew runIde` — launch a sandboxed IDE with the plugin installed
- `./gradlew test` — run the test suite
- `./gradlew generateLexer generateParser` — regenerate the lexer/parser from `src/main/grammar/` (JFlex + Grammar-Kit)
- `./gradlew verifyPlugin` — run JetBrains plugin verification

`kamailio.cfg` at the repo root is the stock Kamailio v6.2 default config, used as the reference input for the grammar and the formatter.

## License

[MIT](LICENSE)

## Credits

- Hover documentation content comes from the [vscode-kamailio-hover](https://github.com/braams/vscode-kamailio-hover) project (generated from the official Kamailio documentation).
- Grammar references: Kamailio's own [`cfg.lex`](https://github.com/kamailio/kamailio/blob/master/src/core/cfg.lex) / [`cfg.y`](https://github.com/kamailio/kamailio/blob/master/src/core/cfg.y) and [tree-sitter-kamailio-cfg](https://github.com/IbrahimShahzad/tree-sitter-kamailio-cfg).
