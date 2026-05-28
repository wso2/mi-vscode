# xml-language-server

LSP (Language Server Protocol) wrapper around `xml-language-service`.
Connects any LSP-compatible editor (VS Code, Neovim, etc.) to the XML language features.

## Architecture

```
VS Code / Editor  ──JSON-RPC over stdio──►  xml-language-server  ──in-process──►  xml-language-service
                  ◄──diagnostics push──                                            └─ Xerces WASM (XSD)
```

## File structure

```
src/
├── server.ts             — entry point; LSP lifecycle (initialize, config, shutdown) + bootstrap
├── requestHandlers.ts    — registers all LSP request handlers (completion, hover, symbols, etc.)
├── diagnosticsHandler.ts — XSD schema loading, validation via Xerces WASM, pushes diagnostics to client
├── configuration.ts      — schema association config (SchemaConfig, applySchemaSettings)
└── utils.ts              — string helpers + LSP ↔ xml-language-service type adapters
```

## How it works

1. The editor spawns the server process and communicates over **stdio** using standard JSON-RPC (LSP).
2. On `initialize`, the server reads schema associations from `initializationOptions` and registers them.
3. On `onInitialized`, it fetches `xmlLanguageServer.schemas` from the editor workspace config and applies them.
4. Every time a document changes, `DiagnosticsHandler` resolves the matching XSD schema (by filename or namespace), loads all `xs:include`/`xs:import` references from disk, and pushes validation diagnostics back to the editor.
5. All other requests (completion, hover, rename, etc.) parse the document on demand and delegate directly to `xml-language-service`.

## Configuration

In your editor's workspace settings, map file patterns to local XSD schemas:

```json
"xmlLanguageServer.schemas": [
  { "pattern": "**/*.synapse.xml", "xsdPath": "schemas/synapse.xsd" },
  { "pattern": "**/proxy-*.xml",   "xsdPath": "schemas/proxy.xsd" }
]
```

`xsdPath` can be absolute or relative to the workspace root. The server automatically follows `xs:include`/`xs:import` chains in the XSD (up to 10 levels deep, 200 files, 20 MB total).

## LSP capabilities

| Feature | Trigger |
|---|---|
| Completion | `<`, space, `"`, `/` |
| Hover | cursor over element / attribute |
| Document symbols | outline panel |
| Folding ranges | editor fold gutter |
| Rename | rename an XML tag (open + close updated together) |
| Go to definition | navigate to matching tag |
| Find references | find all usages of a tag |
| Formatting | format document command |
| Diagnostics | pushed on every document change |

## Related

- [`xml-language-service`](https://github.com/harshanacz/xml-language-service) — core library
- `xml-language-server` — this package
