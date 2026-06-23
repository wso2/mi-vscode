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
 * will reuse a single compiled completion provider instead of creating one copy
 * per open document.  Defaults to `uri` when omitted.
 */
export interface SchemaInfo {
  uri: string;
  xsdText: string;
  xsdPath?: string;
  imports?: Record<string, string>;
}

/** Registry that builds and caches XSD completion providers per resolved schema. */
export class SchemaProvider {
  // documentUri → xsdKey — many documents can share the same completion provider
  private documentToSchema = new Map<string, string>();
  private completionProviders: Map<string, XsdCompletionProvider>;
  private associator: SchemaAssociator;

  constructor() {
    this.completionProviders = new Map();
    this.associator = new SchemaAssociator();
  }

  /**
   * issues/30 - https://github.com/harshanacz/wso2-mi-language-server-ts/issues/30
   */
  async buildAndCacheCompletionProvider(info: SchemaInfo): Promise<void> {
    const xsdKey = info.xsdPath ?? info.uri;
    this.documentToSchema.set(info.uri, xsdKey);

    if (!this.completionProviders.has(xsdKey)) {
      const completionXsd = info.imports
        ? inlineIncludes(info.xsdText, info.imports)
        : info.xsdText;
      const provider = new XsdCompletionProvider(completionXsd);
      console.error(`[schemaProvider] Built provider for ${xsdKey}: ${provider.getAllElements().length} elements, payloadFactory=${provider.getElement("payloadFactory") !== undefined}, inlinedXsdLen=${completionXsd.length}`);
      this.completionProviders.set(xsdKey, provider);
    }
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
    // Prefer the completion provider that was built during buildAndCacheCompletionProvider (which has
    // all xs:include content inlined).  Resolve via documentToSchema so the lookup works regardless
    // of whether the provider is keyed by xsdPath or a fallback auto:// uri.
    if (documentPath) {
      const autoUri = `auto://${documentPath}`;
      const xsdKey = this.documentToSchema.get(autoUri);
      if (xsdKey) {
        const registered = this.completionProviders.get(xsdKey);
        if (registered) return registered;
      }
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

  /** Returns true when a completion provider for the given document URI exists in the registry. */
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
        this.completionProviders.delete(xsdKey);
      }
    }
  }

  /** Clears all cached completion providers and the registry. */
  dispose(): void {
    this.documentToSchema.clear();
    this.completionProviders.clear();
  }
}
