# WSO2 MI XML Language Server

An LSP-compliant language server for WSO2 Micro Integrator XML configuration files. Provides schema-aware editing features powered by Apache Xerces-C (via WebAssembly).

## Features

- **Validation** — XSD validation using the bundled Xerces-C WASM engine; reports both syntax errors and schema violations
- **Completions** — context-aware element and attribute suggestions based on the active schema
- **Hover** — documentation for known elements and their attributes
- **Go to definition / Find references** — navigate between XML elements
- **Rename** — rename elements across the document
- **Document symbols** — outline view of the XML structure
- **Folding** — collapse/expand XML blocks
- **Formatting** — auto-format XML documents

## Supported schema versions

| Version | Folder |
|---------|--------|
| MI 4.3.0 | `resources/schemas/430/` |
| MI 4.4.0 | `resources/schemas/440/` |

The built-in default is 4.4.0 (matched by `xmlns="http://ws.apache.org/ns/synapse"`). A specific version can be selected by the client via schema association settings.

## Project structure

```
src/                    TypeScript source
  server.ts             LSP entry point (connection, lifecycle)
  requestHandlers.ts    LSP request handlers (completion, hover, symbols, …)
  diagnosticsHandler.ts XSD validation → LSP diagnostics
  xmlLanguageService.ts Façade over parser + schema services
  schema/               XSD resolution, validation, completion provider
  services/             Per-feature logic (hover, completion, rename, …)
  parser/               XML parser wrapper
resources/schemas/      Bundled XSD schemas (430, 440)
xerces-wasm/            Pre-built Xerces-C WebAssembly module
tests/                  Vitest test suite
```

## Setup

```bash
npm install
npm run build       # TypeScript compile only
npm run bundle      # Full production bundle (dist/)
npm test            # Run test suite
```

## Client configuration

The client can pass schema associations at initialisation:

```json
{
  "initializationOptions": {
    "schemas": [
      { "pattern": "**/*.xml", "schema": "430" }
    ]
  }
}
```

Schema changes sent via `workspace/didChangeConfiguration` are picked up immediately — the server invalidates its cached providers and re-validates all open documents.
