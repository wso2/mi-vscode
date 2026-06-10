import { XsdValidatorService, Diagnostic, XsdInput, SchemaBundle } from "./xsdValidator.js";
import { XMLDocument } from "../parser/xmlNode.js";
import { XsdCompletionProvider } from "./xsdCompletionProvider.js";
import { SchemaAssociator, SchemaAssociation, ResolvedSchema } from "./schemaAssociator.js";

// Resolves a relative schemaLocation against a current file's path (both relative to
// the entry schema's directory).  Handles ../ and ./ segments via standard URI resolution.
function resolveRelativePath(basePath: string, location: string): string {
  const parts = basePath.split("/");
  parts.pop(); // drop filename, keep directory segments
  for (const seg of location.split("/")) {
    if (seg === "..") parts.pop();
    else if (seg !== ".") parts.push(seg);
  }
  return parts.join("/");
}

// Inlines xs:include / xs:redefine references into a single flat XSD string.
// `currentPath` is the path of `text` relative to the entry schema's directory.
// Uses `visited` to break cycles.
function inlineIncludes(
  text: string,
  imports: Record<string, string>,
  currentPath: string = "",
  visited: Set<string> = new Set(),
): string {
  return text.replace(
    /<xs:(?:include|redefine)\s+schemaLocation\s*=\s*"([^"]+)"\s*(?:\/>|>[\s\S]*?<\/xs:(?:include|redefine)>)/g,
    (_match, location: string) => {
      const resolved = resolveRelativePath(currentPath || "entry.xsd", location);
      const content = imports[resolved];
      if (!content || visited.has(resolved)) return "";
      visited.add(resolved);
      const expanded = inlineIncludes(content, imports, resolved, visited);
      const body = expanded.match(/<xs:schema\b[^>]*>([\s\S]*)<\/xs:schema>/);
      return body ? body[1] : "";
    },
  );
}

export { SchemaAssociation, ResolvedSchema };

/**
 * Identifies a schema to register.
 * `xsdText` is the root XSD content.
 * `imports` optionally maps relative filenames (matching xs:include / xs:import
 * schemaLocation values) to their XSD content, enabling multi-file schemas.
 * `xsdPath` is an optional stable key for the XSD file itself (e.g. its absolute
 * path on disk).  When provided, multiple documents that share the same XSD path
 * will reuse a single compiled XsdValidatorService instead of creating one copy
 * per open document.  Defaults to `uri` when omitted.
 */
export interface SchemaInfo {
  uri: string;
  xsdText: string;
  xsdPath?: string;
  imports?: Record<string, string>;
}

export { SchemaBundle };

/** Registry that manages compiled XSD validators and routes validation requests to them. */
export class SchemaProvider {
  // xsdKey (xsdPath ?? uri) → one validator per unique XSD
  private validators = new Map<string, XsdValidatorService>();
  // documentUri → xsdKey — many documents can share the same validator
  private documentToSchema = new Map<string, string>();
  private completionProviders: Map<string, XsdCompletionProvider>;
  private associator: SchemaAssociator;

  constructor() {
    this.completionProviders = new Map();
    this.associator = new SchemaAssociator();
  }

  /**
   * Compiles the given XSD (if not already compiled for the same xsdPath) and
   * maps the document URI to that validator.  Multiple documents sharing the
   * same xsdPath reuse a single XsdValidatorService instance.
   * Also builds and caches a completion provider for the document URI.
   */
  async registerSchema(info: SchemaInfo): Promise<void> {
    const xsdKey = info.xsdPath ?? info.uri;
    const prevKey = this.documentToSchema.get(info.uri);
    this.documentToSchema.set(info.uri, xsdKey);

    // If this document previously pointed to a different XSD key, release that
    // validator when no other document still references it.
    if (prevKey && prevKey !== xsdKey && !this._isKeyReferenced(prevKey)) {
      this.validators.get(prevKey)?.dispose();
      this.validators.delete(prevKey);
    }

    // Compile the validator only once per unique XSD key.
    if (!this.validators.has(xsdKey)) {
      const xsd: XsdInput = info.imports
        ? { entry: info.xsdText, imports: info.imports }
        : info.xsdText;
      this.validators.set(xsdKey, await XsdValidatorService.create(xsd));
    }

    // Build the completion provider from the fully inlined XSD so that types
    // defined in xs:include'd schemas are available for hover and completions.
    const completionXsd = info.imports
      ? inlineIncludes(info.xsdText, info.imports)
      : info.xsdText;
    const provider = new XsdCompletionProvider(completionXsd);
    console.error(`[schemaProvider] Built provider for ${info.uri}: ${provider.getAllElements().length} elements, payloadFactory=${provider.getElement("payloadFactory") !== undefined}, inlinedXsdLen=${completionXsd.length}`);
    this.completionProviders.set(info.uri, provider);
  }

  private _isKeyReferenced(xsdKey: string): boolean {
    for (const k of this.documentToSchema.values()) {
      if (k === xsdKey) return true;
    }
    return false;
  }

  /** Registers a custom file-to-schema mapping that takes priority over built-in associations. */
  addUserAssociation(association: SchemaAssociation): void {
    this.associator.addUserAssociation(association);
  }

  /** Removes all user-registered associations so a fresh set can be applied. */
  clearUserAssociations(): void {
    this.associator.clearUserAssociations();
  }

  /** Returns the raw ResolvedSchema (with xsdText) for the given file name and optional namespace, or null if none matches. */
  findSchemaForDocument(fileName: string, xmlns?: string, documentPath?: string): ResolvedSchema | null {
    return this.associator.findSchema(fileName, xmlns, documentPath);
  }

  /**
   * Resolves and returns the XsdCompletionProvider for the given file name and optional namespace.
   * Automatically loads and caches the provider on first access via the built-in associations.
   * Returns null if no matching schema is found.
   */
  resolveSchemaForDocument(fileName: string, xmlns?: string, documentPath?: string): XsdCompletionProvider | null {
    // Prefer the completion provider that was built during registerSchema (which has
    // all xs:include content inlined).  diagnosticsHandler registers under auto://<path>.
    if (documentPath) {
      const registered = this.completionProviders.get(`auto://${documentPath}`);
      if (registered) return registered;
    }

    const cacheKey = `${documentPath ?? fileName}|${xmlns ?? ""}`;
    const cached = this.completionProviders.get(cacheKey);
    if (cached) return cached;

    const resolved = this.associator.findSchema(fileName, xmlns, documentPath);
    if (!resolved) return null;

    // Build a provider from the raw XSD text (no xs:include inlining). This is a
    // partial provider used only when the auto:// provider is not ready yet. It is
    // intentionally NOT cached so that once validateAndSend registers the full
    // auto:// provider it is used immediately on the next request.
    return new XsdCompletionProvider(resolved.xsdText);
  }

  /**
   * Validates the document against the schema registered under schemaUri.
   * Returns a warning diagnostic when no matching schema is found.
   */
  async validate(schemaUri: string, document: XMLDocument): Promise<Diagnostic[]> {
    const xsdKey = this.documentToSchema.get(schemaUri);
    const validator = xsdKey ? this.validators.get(xsdKey) : undefined;
    if (!validator) {
      // No schema registered — fall back to the parser's own syntax errors so
      // basic well-formedness problems are still reported without Xerces.
      return document.syntaxErrors.map((e) => ({
        message: e.message,
        severity: "error" as const,
        source: "syntax" as const,
        range: {
          start: { line: e.line, character: e.character },
          end:   { line: e.line, character: e.character },
        },
      }));
    }
    // Xerces-wasm does not support remote protocols (http/https). If xsi:schemaLocation is present,
    // it will try to fetch the URL and fail with "unsupported protocol in URL".
    // Replacing the entire attribute with spaces preserves line/col offsets and forces it to use our cached schema.
    const sanitizedText = document.text
      .replace(/\bxsi:schemaLocation\s*=\s*(["'])([\s\S]*?)\1/g, (m) => " ".repeat(m.length))
      .replace(/\bxsi:noNamespaceSchemaLocation\s*=\s*(["'])([\s\S]*?)\1/g, (m) => " ".repeat(m.length));

    return validator.validate(sanitizedText);
  }

  /** Returns true when a compiled validator for the given document URI exists in the registry. */
  hasSchema(uri: string): boolean {
    return this.documentToSchema.has(uri);
  }

  /** Removes all auto:// schemas so they are re-registered fresh on next validation. */
  invalidateAutoSchemas(): void {
    const keysToCheck = new Set<string>();
    for (const [docUri, xsdKey] of this.documentToSchema) {
      if (docUri.startsWith("auto://")) {
        keysToCheck.add(xsdKey);
        this.documentToSchema.delete(docUri);
      }
    }
    for (const xsdKey of keysToCheck) {
      if (!this._isKeyReferenced(xsdKey)) {
        this.validators.get(xsdKey)?.dispose();
        this.validators.delete(xsdKey);
      }
    }
    for (const key of this.completionProviders.keys()) {
      if (key.startsWith("auto://")) {
        this.completionProviders.delete(key);
      }
    }
  }

  /** Disposes all registered validators and clears the registry. */
  dispose(): void {
    for (const validator of this.validators.values()) {
      validator.dispose();
    }
    this.validators.clear();
    this.documentToSchema.clear();
    this.completionProviders.clear();
  }
}
