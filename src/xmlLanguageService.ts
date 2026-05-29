import { parseXMLDocument } from "./parser/xmlParser.js";
import { printAST, printCST, printTreeAST, PrintOptions } from "./utils/xmlPrinter.js";
import { XMLDocument } from "./parser/xmlNode.js";
import { Position } from "./utils/positionUtils.js";
import { doComplete, CompletionList } from "./services/xmlCompletion.js";
import { doHover, HoverResult } from "./services/xmlHover.js";
import { findDocumentSymbols, DocumentSymbol } from "./services/xmlSymbols.js";
import { getFoldingRanges, FoldingRange } from "./services/xmlFolding.js";
import { format, TextEdit, FormatterOptions } from "./services/xmlFormatter.js";
import { doRename } from "./services/xmlRename.js";
import { doDefinition, DefinitionResult } from "./services/xmlDefinition.js";
import { findReferences, ReferenceResult } from "./services/xmlReferences.js";
import { SchemaProvider, SchemaInfo, SchemaAssociation, ResolvedSchema } from "./schema/schemaProvider.js";
import { Diagnostic } from "./schema/xsdValidator.js";

export function getLanguageService() {
  const schemaProvider = new SchemaProvider();

  return {
    // ── Phase 01 — XML Core ──────────────────────────────────────────────────

    parseXMLDocument(uri: string, text: string): XMLDocument {
      return parseXMLDocument(uri, text);
    },

    doComplete(document: XMLDocument, position: Position, fileName?: string, documentPath?: string): CompletionList {
      const xmlns = (document as any).getNamespace?.() ?? undefined;
      const completionProvider = schemaProvider.resolveSchemaForDocument(fileName ?? '', xmlns, documentPath);
      return doComplete(document, position, completionProvider);
    },

    doHover(document: XMLDocument, position: Position, fileName?: string, documentPath?: string): HoverResult | null {
      const xmlns = (document as any).getNamespace?.() ?? undefined;
      const completionProvider = schemaProvider.resolveSchemaForDocument(fileName ?? '', xmlns, documentPath);
      return doHover(document, position, completionProvider);
    },

    findDocumentSymbols(document: XMLDocument): DocumentSymbol[] {
      return findDocumentSymbols(document);
    },

    getFoldingRanges(document: XMLDocument): FoldingRange[] {
      return getFoldingRanges(document);
    },

    format(document: XMLDocument, options?: FormatterOptions): TextEdit[] {
      return format(document, options);
    },

    doRename(document: XMLDocument, position: Position, newName: string): TextEdit[] | null {
      return doRename(document, position, newName);
    },

    doDefinition(document: XMLDocument, position: Position): DefinitionResult | null {
      return doDefinition(document, position);
    },

    findReferences(document: XMLDocument, position: Position): ReferenceResult[] {
      return findReferences(document, position);
    },

    // ── Debug / Inspection ───────────────────────────────────────────────────

    printAST(document: XMLDocument, options?: PrintOptions): string {
      return printAST(document, options);
    },

    printCST(document: XMLDocument, options?: PrintOptions): string {
      return printCST(document, options);
    },

    printTreeAST(document: XMLDocument): string {
      return printTreeAST(document);
    },

    // ── Phase 02 — XSD Schema Validation ────────────────────────────────────

    async registerSchema(info: SchemaInfo): Promise<void> {
      return schemaProvider.registerSchema(info);
    },

    async validate(schemaUri: string, document: XMLDocument): Promise<Diagnostic[]> {
      return schemaProvider.validate(schemaUri, document);
    },

    hasSchema(uri: string): boolean {
      return schemaProvider.hasSchema(uri);
    },

    /** Returns the raw schema (xsdText + source) matched to the given file name and xmlns, or null if none found. */
    resolveSchemaForDocument(fileName: string, xmlns?: string, documentPath?: string): ResolvedSchema | null {
      return schemaProvider.findSchemaForDocument(fileName, xmlns, documentPath);
    },

    /** Registers a custom file-to-schema mapping, taking priority over built-in associations. */
    addUserAssociation(association: SchemaAssociation): void {
      schemaProvider.addUserAssociation(association);
    },

    /** Clears all auto-registered schemas so they are re-loaded from disk on next validation. */
    invalidateAutoSchemas(): void {
      schemaProvider.invalidateAutoSchemas();
    },

    dispose(): void {
      schemaProvider.dispose();
    },
  };
}
