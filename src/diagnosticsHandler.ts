import {
  Connection,
  Diagnostic,
  DiagnosticSeverity,
} from "vscode-languageserver/node.js";
import { TextDocument } from "vscode-languageserver-textdocument";
import { getLanguageService } from "./xmlLanguageService.js";
import * as fs from "fs";
import * as path from "path";
// @ts-ignore — local CJS bundle, no ESM wrapper
import { createProjectValidator } from "../xerces-wasm/dist/index.js";

type LanguageService = ReturnType<typeof getLanguageService>;

const MAX_SCHEMA_IMPORT_FILES = 200;
const MAX_SCHEMA_IMPORT_FILE_BYTES = 2 * 1024 * 1024;
const MAX_SCHEMA_IMPORT_TOTAL_BYTES = 20 * 1024 * 1024;
const MAX_SCHEMA_IMPORT_DEPTH = 10;

const SEVERITY_MAP: Record<"error" | "warning" | "info", DiagnosticSeverity> =
  {
    error: DiagnosticSeverity.Error,
    warning: DiagnosticSeverity.Warning,
    info: DiagnosticSeverity.Information,
  };

export class DiagnosticsHandler {
  private connection: Connection;
  private service: LanguageService;
  private diagnosticsByUri = new Map<string, Diagnostic[]>();
  private projectValidators = new Map<string, any>(); // schemaUri → ProjectValidator (reuses locked grammar pool)

  constructor(connection: Connection, service: LanguageService) {
    this.connection = connection;
    this.service = service;
  }

  async validateAndSend(document: TextDocument): Promise<void> {
    const fileName = document.uri.split("/").pop() ?? "";
    const documentPath = document.uri.startsWith("file://")
      ? decodeURIComponent(document.uri.replace("file://", ""))
      : undefined;
    const text = document.getText();
    const xmlDoc = this.service.parseXMLDocument(document.uri, text);
    const xmlns = (xmlDoc as any).getNamespace?.() ?? undefined;

    // Auto-resolve schema by file name / namespace.
    const resolved = this.service.resolveSchemaForDocument(fileName, xmlns, documentPath);
    if (resolved) {
      const autoUri = `auto://${documentPath ?? fileName}`;

      // Register schema for completions/hover (unchanged from before).
      if (!this.service.hasSchema(autoUri)) {
        const imports = resolved.xsdPath
          ? this.loadReferencedXsds(resolved.xsdPath, resolved.xsdText)
          : undefined;
        const importKeys = imports ? Object.keys(imports) : [];
        this.connection.console.log(
          `[DiagnosticsHandler] Auto-registering schema for ${fileName}: ${importKeys.length} referenced import files (${importKeys.filter(k => k.includes("/")).length} in subdirs)`
        );
        await this.service.registerSchema({
          uri: autoUri,
          xsdText: resolved.xsdText,
          imports,
          xsdPath: resolved.xsdPath,
        });
        this.connection.console.log(
          `[DiagnosticsHandler] Schema registered at ${autoUri}`
        );
      }

      // Use ProjectValidator for diagnostics when we have an absolute XSD path.
      const schemaFolder = resolved.xsdPath
        ? path.dirname(path.resolve(resolved.xsdPath))
        : undefined;

      if (schemaFolder && this.isXmlDocument(xmlDoc)) {
        this.connection.console.log(
          `[DiagnosticsHandler] Validating ${document.uri} with ProjectValidator`
        );
        try {
          const validator = await this.getOrCreateValidator(autoUri, schemaFolder, resolved.xsdPath);
          const result = await validator.validate(text);
          const allErrors = [...result.parseErrors, ...result.schemaErrors];
          const xmlLines = text.split("\n");
          const diagnostics: Diagnostic[] = allErrors.map((e: any) => {
            const line = e.line > 0 ? e.line - 1 : 0;
            const col = e.column > 0 ? e.column - 1 : 0;
            const lineText = xmlLines[line] ?? "";
            const tagStart = lineText.lastIndexOf("<", col);
            const start = tagStart >= 0 ? { line, character: tagStart } : { line, character: col };
            return {
              range: { start, end: { line, character: col + 1 } },
              message: e.message,
              severity: DiagnosticSeverity.Error,
              source: "xml-language-service",
            };
          });
          this.send(document.uri, filterDiagnostics(diagnostics));
          return;
        } catch (error) {
          const message = error instanceof Error ? error.message : String(error);
          this.warn(`[DiagnosticsHandler] ProjectValidator failed for ${document.uri}; falling back to schema provider validation: ${message}`);
        }
      }

      // Fallback: service.validate() when no absolute xsdPath is available.
      this.connection.console.log(
        `[DiagnosticsHandler] Validating ${document.uri} against auto schema`
      );
      const raw = await this.service.validate(autoUri, xmlDoc);
      const converted = this.toDiagnostics(raw);
      this.send(document.uri, filterDiagnostics(converted));
      return;
    }

    // No schema found — clear any stale diagnostics.
    this.connection.console.log(
      `[DiagnosticsHandler] No schema for ${document.uri}, clearing diagnostics`
    );
    this.send(document.uri, []);
  }

  /** Clears all diagnostics for the given document URI. */
  clearDiagnostics(uri: string): void {
    this.connection.console.log(`[DiagnosticsHandler] Clearing diagnostics for ${uri}`);
    this.send(uri, []);
  }

  /** Destroys all cached ProjectValidators and releases their grammar pools. */
  dispose(): void {
    for (const validator of this.projectValidators.values()) {
      try {
        validator.destroy();
      } catch {
        // ignore errors during teardown
      }
    }
    this.projectValidators.clear();
    this.connection.console.log("[validator] All ProjectValidators destroyed");
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  /** Returns true if there is any XSD or syntax diagnostic covering the given position. */
  hasErrorAt(uri: string, line: number, character: number): boolean {
    const all = this.diagnosticsByUri.get(uri) ?? [];
    return all.some((d) => {
      const { start, end } = d.range;
      if (line < start.line || line > end.line) return false;
      if (line === start.line && character < start.character) return false;
      if (line === end.line && character > end.character) return false;
      return true;
    });
  }

  private send(uri: string, diagnostics: Diagnostic[]): void {
    this.diagnosticsByUri.set(uri, diagnostics);
    this.connection.sendDiagnostics({ uri, diagnostics });
  }

  private warn(message: string): void {
    const console = this.connection.console as Partial<Connection["console"]>;
    if (typeof console.warn === "function") {
      console.warn(message);
    } else {
      this.connection.console.log(message);
    }
  }

  /** Recursively reads all .xsd and .dtd files from schemaFolder.
   *  Keys are paths relative to schemaFolder using forward slashes,
   *  e.g. "synapse_config.xsd", "mediators/mediators.xsd". */
  private async buildFilesMap(schemaFolder: string): Promise<Record<string, string>> {
    const result: Record<string, string> = {};

    const walk = (dir: string, prefix: string) => {
      let entries: fs.Dirent[];
      try {
        entries = fs.readdirSync(dir, { withFileTypes: true });
      } catch {
        return;
      }
      for (const entry of entries) {
        const rel = prefix ? `${prefix}/${entry.name}` : entry.name;
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          walk(full, rel);
        } else if (entry.isFile()) {
          const lower = entry.name.toLowerCase();
          if (lower.endsWith(".xsd") || lower.endsWith(".dtd")) {
            try {
              result[rel] = fs.readFileSync(full, "utf-8");
            } catch {
              // skip unreadable files
            }
          }
        }
      }
    };

    walk(schemaFolder, "");
    return result;
  }

  /** Returns a cached ProjectValidator for schemaUri, creating one if needed.
   *  Each validator compiles the grammar once and reuses the locked pool on
   *  subsequent validate() calls. */
  private async getOrCreateValidator(schemaUri: string, schemaFolder: string, entryPath?: string): Promise<any> {
    const existing = this.projectValidators.get(schemaUri);
    if (existing) return existing;

    const filesMap = await this.buildFilesMap(schemaFolder);

    const requestedEntry = entryPath
      ? this.toImportKey(schemaFolder, path.resolve(entryPath))
      : undefined;

    // Prefer the resolved XSD path as the entry point, then the built-in Synapse
    // entry name, otherwise use the first root-level XSD.
    let entry = "";
    if (requestedEntry && requestedEntry in filesMap) {
      entry = requestedEntry;
    } else if ("synapse_config.xsd" in filesMap) {
      entry = "synapse_config.xsd";
    } else {
      entry = Object.keys(filesMap).find((k) => !k.includes("/") && k.endsWith(".xsd")) ?? "";
    }

    const validator = await createProjectValidator({ entry, files: filesMap });
    this.projectValidators.set(schemaUri, validator);
    this.connection.console.log(
      `[validator] Created ProjectValidator for ${schemaUri} (entry: ${entry}, files: ${Object.keys(filesMap).length})`
    );
    return validator;
  }

  /** Recursively loads referenced XSD files from xs:include/xs:import/xs:redefine schemaLocation values,
   *  plus DTD files explicitly referenced by loaded XSD/DTD files.
   *  Keys are relative paths from the entry XSD's directory (e.g. "misc/common.xsd", "mediators/mediators.xsd").
   *  The WASM bridge registers them as memory:///key, which matches the URIs Xerces computes when
   *  resolving schemaLocation values relative to memory:///main.xsd. */
  private loadReferencedXsds(entryPath: string, entryText: string): Record<string, string> | undefined {
    try {
      const rootDir = path.dirname(path.resolve(entryPath));
      const entryFullPath = path.resolve(entryPath);
      const imports: Record<string, string> = {};
      const visited = new Set<string>([entryFullPath]);
      let totalBytes = 0;

      const loadFile = (fullPath: string, location: string): string | undefined => {
        let stat: fs.Stats;
        try {
          stat = fs.lstatSync(fullPath);
        } catch {
          this.warn(`[DiagnosticsHandler] Skipping missing schema reference '${location}'`);
          return undefined;
        }

        if (stat.isSymbolicLink() || !stat.isFile()) {
          this.warn(`[DiagnosticsHandler] Skipping non-file schema reference '${location}'`);
          return undefined;
        }

        if (stat.size > MAX_SCHEMA_IMPORT_FILE_BYTES) {
          this.warn(
            `[DiagnosticsHandler] Skipping schema reference '${location}' because it is ${stat.size} bytes, above the ${MAX_SCHEMA_IMPORT_FILE_BYTES} byte limit`
          );
          return undefined;
        }

        if (totalBytes + stat.size > MAX_SCHEMA_IMPORT_TOTAL_BYTES) {
          this.warn(
            `[DiagnosticsHandler] Skipping schema reference '${location}' because total referenced schema size would exceed ${MAX_SCHEMA_IMPORT_TOTAL_BYTES} bytes`
          );
          return undefined;
        }

        try {
          const text = fs.readFileSync(fullPath, "utf-8");
          totalBytes += stat.size;
          return text;
        } catch {
          this.warn(`[DiagnosticsHandler] Skipping unreadable schema reference '${location}'`);
          return undefined;
        }
      };

      const loadReference = (currentPath: string, location: string, allowDtd: boolean): string | undefined => {
        const resolvedPath = this.resolveLocalReference(currentPath, rootDir, location, allowDtd);
        if (!resolvedPath) return undefined;
        if (visited.has(resolvedPath)) return undefined;

        if (Object.keys(imports).length >= MAX_SCHEMA_IMPORT_FILES) {
          this.warn(
            `[DiagnosticsHandler] Skipping schema reference '${location}' because max import file count ${MAX_SCHEMA_IMPORT_FILES} was reached`
          );
          return undefined;
        }

        const text = loadFile(resolvedPath, location);
        visited.add(resolvedPath);
        if (text === undefined) return undefined;

        const rel = this.toImportKey(rootDir, resolvedPath);
        imports[rel] = text;
        return resolvedPath;
      };

      const visit = (currentPath: string, currentText: string, depth: number) => {
        if (depth >= MAX_SCHEMA_IMPORT_DEPTH) {
          this.warn(
            `[DiagnosticsHandler] Stopping schema reference traversal at ${currentPath}; max depth ${MAX_SCHEMA_IMPORT_DEPTH} reached`
          );
          return;
        }

        for (const location of this.findSchemaLocations(currentText)) {
          const resolvedPath = loadReference(currentPath, location, false);
          if (!resolvedPath) continue;
          const text = imports[this.toImportKey(rootDir, resolvedPath)];
          if (text === undefined) continue;
          visit(resolvedPath, text, depth + 1);
        }

        for (const location of this.findDtdLocations(currentText)) {
          const resolvedPath = loadReference(currentPath, location, true);
          if (!resolvedPath) continue;
          const text = imports[this.toImportKey(rootDir, resolvedPath)];
          if (text === undefined) continue;
          visit(resolvedPath, text, depth + 1);
        }
      };

      visit(entryFullPath, entryText, 0);
      return Object.keys(imports).length > 0 ? imports : undefined;
    } catch {
      return undefined;
    }
  }

  private findSchemaLocations(xsdText: string): string[] {
    const locations: string[] = [];
    const tagPattern = /<(?:(?:\w+):)?(?:include|import|redefine)\b[^>]*\bschemaLocation\s*=\s*(["'])([^"']+)\1/gi;
    let match: RegExpExecArray | null;
    while ((match = tagPattern.exec(xsdText)) !== null) {
      locations.push(match[2]);
    }
    return locations;
  }

  private findDtdLocations(text: string): string[] {
    const locations: string[] = [];
    const doctypePattern = /<!DOCTYPE\b[\s\S]*?(?:SYSTEM\s+(["'])([^"']+)\1|PUBLIC\s+(["'])[^"']+\3\s+(["'])([^"']+)\4)[\s\S]*?>/gi;
    const entityPattern = /<!ENTITY\s+%\s+[\w.-]+\s+(?:SYSTEM\s+(["'])([^"']+)\1|PUBLIC\s+(["'])[^"']+\3\s+(["'])([^"']+)\4)\s*>/gi;
    let match: RegExpExecArray | null;

    while ((match = doctypePattern.exec(text)) !== null) {
      locations.push(match[2] ?? match[5]);
    }

    while ((match = entityPattern.exec(text)) !== null) {
      locations.push(match[2] ?? match[5]);
    }

    return locations.filter((location) => location.toLowerCase().endsWith(".dtd"));
  }

  private resolveLocalReference(currentPath: string, rootDir: string, location: string, allowDtd: boolean): string | undefined {
    if (/^[a-z][a-z0-9+.-]*:/i.test(location)) {
      this.warn(`[DiagnosticsHandler] Skipping remote schema reference '${location}'`);
      return undefined;
    }

    const lowerLocation = location.toLowerCase();
    if (!lowerLocation.endsWith(".xsd") && !(allowDtd && lowerLocation.endsWith(".dtd"))) {
      this.warn(`[DiagnosticsHandler] Skipping unsupported schema reference '${location}'`);
      return undefined;
    }

    const resolvedPath = path.resolve(path.dirname(currentPath), location);
    const relativeToRoot = path.relative(rootDir, resolvedPath);
    if (relativeToRoot.startsWith("..") || path.isAbsolute(relativeToRoot)) {
      this.warn(`[DiagnosticsHandler] Skipping schema reference outside schema root '${location}'`);
      return undefined;
    }

    return resolvedPath;
  }

  private toImportKey(rootDir: string, fullPath: string): string {
    return path.relative(rootDir, fullPath).split(path.sep).join("/");
  }

  private isXmlDocument(document: unknown): boolean {
    return typeof document === "object" && document !== null && Array.isArray((document as any).syntaxErrors);
  }

  private toDiagnostics(raw: Awaited<ReturnType<LanguageService["validate"]>>): Diagnostic[] {
    return raw.map((d) => ({
      range: d.range,
      message: d.message,
      severity: SEVERITY_MAP[d.severity],
      source: "xml-language-service",
    }));
  }
}

/**
 * Removes redundant attribute and content-model diagnostics for elements that
 * are already reported as unknown ("no declaration found for element 'X'").
 * Keeping only the single "unknown element" error avoids noisy cascades like:
 *   - "attribute 'name' is not declared for element 'variable'"
 *   - "element 'variable' is not allowed for content model '(...)'"
 */
function filterDiagnostics(diagnostics: Diagnostic[]): Diagnostic[] {
  // Step 1: collect element names that are outright unknown
  const unknownElements = new Set<string>();
  for (const d of diagnostics) {
    const m = d.message.match(/no declaration found for element '([^']+)'/);
    if (m) unknownElements.add(m[1]);
  }

  if (unknownElements.size === 0) return diagnostics;

  // Step 2: drop attribute and content-model noise for those elements
  const filtered = diagnostics.filter((d) => {
    const msg = d.message;

    // "attribute 'X' is not declared for element 'name'" — redundant when element is unknown
    if (msg.includes("is not declared for element '") && msg.includes("attribute")) {
      const m = msg.match(/is not declared for element '([^']+)'/);
      if (m && unknownElements.has(m[1])) return false;
    }

    // "element 'name' is not allowed for content model '(...)'" — redundant when element is unknown
    if (msg.includes("is not allowed for content model")) {
      const m = msg.match(/element '([^']+)' is not allowed for content model/);
      if (m && unknownElements.has(m[1])) return false;
    }

    return true;
  });

  // Step 3: deduplicate — Xerces can emit the same message twice for the same position
  const seen = new Set<string>();
  return filtered.filter((d) => {
    const key = `${d.message}|${d.range.start.line}|${d.range.start.character}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}
