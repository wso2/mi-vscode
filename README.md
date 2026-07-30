# WSO2 MI XML Language Server

A TypeScript Language Server Protocol (LSP) implementation for WSO2 Micro Integrator XML configuration files. Provides schema-aware editing, autocompletion, hover documentation, and strict validation powered by Apache Xerces WebAssembly.

## Features

- **Validation:** Strict XSD syntax and schema validation via Xerces WASM.
- **Completions & Hover:** Schema-driven $O(1)$ autocomplete suggestions and markdown hover documentation.
- **Navigation:** Go to definition, find references, and document symbols.
- **Editing:** Document formatting, range formatting, folding ranges, and symbol renaming.

> Note: `pom.xml` files use strict Maven 4.0.0 validation (completions and hover are disabled).

## Supported Schemas

| Target Version | Schema Location | Default Namespace |
| :--- | :--- | :--- |
| **MI 4.3.0** | `resources/schemas/430/` | `http://ws.apache.org/ns/synapse` |
| **MI 4.4.0** | `resources/schemas/440/` (Default) | `http://ws.apache.org/ns/synapse` |
| **Maven 4.0.0** | `resources/schemas/maven/` | `http://maven.apache.org/POM/4.0.0` |

## Project Structure

```text
src/
  server.ts             LSP entry point & lifecycle management
  requestHandlers.ts    LSP request event handlers
  diagnosticsHandler.ts Diagnostic reporting & Xerces WASM validation
  xmlLanguageService.ts Main service facade
  schema/               XSD schema provider & CST compilation engine
  services/             Feature implementations (completion, hover, etc.)
  parser/               XML parser wrapper (@xml-tools/parser)
xerces-wasm/            Pre-built Apache Xerces-C WebAssembly module
tests/                  Vitest test suite
```

## Quick Start

```bash
# Install dependencies
npm install

# Run unit tests
npm test

# Build production bundle (dist/)
npm run bundle
```

## Client Configuration

LSP clients can configure schema associations via `initializationOptions` or `workspace/didChangeConfiguration`:

```json
{
  "initializationOptions": {
    "schemas": [
      { "pattern": "**/*.xml", "schema": "440" }
    ]
  }
}
```
