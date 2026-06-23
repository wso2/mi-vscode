# WSO2 MI XML Language Server

LSP language server for WSO2 Micro Integrator XML config files. Schema-aware editing powered by Apache Xerces-C (WebAssembly).

## Features

- Validation (XSD syntax + schema)
- Completions
- Hover
- Go to definition
- Find references
- Rename
- Document symbols
- Folding
- Formatting

`pom.xml` files use strict Maven 4.0.0 validation (completions/hover disabled).

## Schemas

| Version | Folder |
|---------|--------|
| MI 4.3.0 | `resources/schemas/430/` |
| MI 4.4.0 | `resources/schemas/440/` |
| Maven 4.0.0 | `resources/schemas/maven/` |

Default is 4.4.0 (`xmlns="http://ws.apache.org/ns/synapse"`). Clients can override via schema associations; `pom.xml` always uses Maven.

## Project structure

```
src/
  server.ts             LSP entry point (connection, lifecycle)
  requestHandlers.ts    LSP request handlers
  diagnosticsHandler.ts XSD validation → LSP diagnostics
  xmlLanguageService.ts Façade over parser + schema services
  schema/               XSD resolution and completion provider
  services/             Per-feature logic (hover, completion, rename, …)
  parser/               XML parser wrapper
xerces-wasm/            Pre-built Xerces-C WebAssembly module
tests/                  Vitest test suite
```

## Setup

```bash
npm install
npm run bundle   # production bundle (dist/)
npm test
```

## Client configuration

```json
{
  "initializationOptions": {
    "schemas": [{ "pattern": "**/*.xml", "schema": "430" }]
  }
}
```

Changes via `workspace/didChangeConfiguration` are applied immediately.
