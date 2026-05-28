import {
  Connection,
  TextDocuments,
  CompletionParams,
  HoverParams,
  DocumentSymbolParams,
  FoldingRangeParams,
  DocumentFormattingParams,
  RenameParams,
  DefinitionParams,
  ReferenceParams,
  MarkupKind,
} from "vscode-languageserver/node.js";
import { TextDocument } from "vscode-languageserver-textdocument";
import { getLanguageService } from "xml-language-service";
import { DiagnosticsHandler } from "./diagnosticsHandler.js";
import {
  getFileName,
  getDocumentPath,
  escapeMd,
  toLSPCompletionList,
  toLSPDocumentSymbol,
  toLSPHover,
  toLSPFoldingRange,
} from "./utils.js";

type LanguageService = ReturnType<typeof getLanguageService>;

export function registerRequestHandlers(
  connection: Connection,
  documents: TextDocuments<TextDocument>,
  service: LanguageService,
  diagnosticsHandler: DiagnosticsHandler
): void {
  connection.onCompletion((params: CompletionParams) => {
    connection.console.log(
      `[onCompletion] Triggered for ${params.textDocument.uri} at line ${params.position.line}, char ${params.position.character}`
    );
    const document = documents.get(params.textDocument.uri);
    if (!document) return [];
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    return toLSPCompletionList(
      service.doComplete(xmlDoc, params.position, getFileName(document.uri), getDocumentPath(document.uri))
    );
  });

  connection.onHover((params: HoverParams) => {
    connection.console.log(
      `[onHover] Triggered for ${params.textDocument.uri} at line ${params.position.line}, char ${params.position.character}`
    );
    const document = documents.get(params.textDocument.uri);
    if (!document) return null;
    const { line, character } = params.position;

    const errors = diagnosticsHandler.getDiagnosticsAt(document.uri, line, character);
    if (errors.length > 0) {
      const isError = (d: typeof errors[number]) => d.severity === undefined || d.severity === 1;
      const value = errors
        .map((d) => `${isError(d) ? "**ERROR**" : "**WARNING**"} — ${escapeMd(d.message)}`)
        .join("\n\n");
      return { contents: { kind: MarkupKind.Markdown, value } };
    }

    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    const result = service.doHover(xmlDoc, params.position, getFileName(document.uri), getDocumentPath(document.uri));
    connection.console.log(`[onHover] Result: ${JSON.stringify(result)}`);
    return result ? toLSPHover(result) : null;
  });

  connection.onDocumentSymbol((params: DocumentSymbolParams) => {
    connection.console.log(`[onDocumentSymbol] Triggered for ${params.textDocument.uri}`);
    const document = documents.get(params.textDocument.uri);
    if (!document) return [];
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    return service.findDocumentSymbols(xmlDoc).map(toLSPDocumentSymbol);
  });

  connection.onFoldingRanges((params: FoldingRangeParams) => {
    connection.console.log(`[onFoldingRanges] Triggered for ${params.textDocument.uri}`);
    const document = documents.get(params.textDocument.uri);
    if (!document) return [];
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    return service.getFoldingRanges(xmlDoc).map(toLSPFoldingRange);
  });

  connection.onRenameRequest((params: RenameParams) => {
    connection.console.log(
      `[onRename] Triggered for ${params.textDocument.uri} at line ${params.position.line}, char ${params.position.character} to '${params.newName}'`
    );
    const document = documents.get(params.textDocument.uri);
    if (!document) return null;
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    const edits = service.doRename(xmlDoc, params.position, params.newName);
    if (!edits) return null;
    return {
      changes: {
        [document.uri]: edits.map((e) => ({
          range: {
            start: document.positionAt(e.startOffset),
            end: document.positionAt(e.endOffset),
          },
          newText: e.newText,
        })),
      },
    };
  });

  connection.onDefinition((params: DefinitionParams) => {
    connection.console.log(
      `[onDefinition] Triggered for ${params.textDocument.uri} at line ${params.position.line}, char ${params.position.character}`
    );
    const document = documents.get(params.textDocument.uri);
    if (!document) return null;
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    const result = service.doDefinition(xmlDoc, params.position);
    return result ? { uri: result.uri, range: result.range } : null;
  });

  connection.onReferences((params: ReferenceParams) => {
    connection.console.log(
      `[onReferences] Triggered for ${params.textDocument.uri} at line ${params.position.line}, char ${params.position.character}`
    );
    const document = documents.get(params.textDocument.uri);
    if (!document) return [];
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    return service.findReferences(xmlDoc, params.position).map((r) => ({ uri: r.uri, range: r.range }));
  });

  connection.onDocumentFormatting((params: DocumentFormattingParams) => {
    connection.console.log(`[onDocumentFormatting] Triggered for ${params.textDocument.uri}`);
    const document = documents.get(params.textDocument.uri);
    if (!document) return [];
    const xmlDoc = service.parseXMLDocument(document.uri, document.getText());
    return service.format(xmlDoc, {
      tabSize: params.options.tabSize,
      insertSpaces: params.options.insertSpaces,
    }).map((edit) => ({
      range: {
        start: document.positionAt(edit.startOffset),
        end: document.positionAt(edit.endOffset),
      },
      newText: edit.newText,
    }));
  });
}
