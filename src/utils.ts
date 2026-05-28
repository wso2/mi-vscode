import {
  CompletionItemKind,
  SymbolKind,
  FoldingRange as LSPFoldingRange,
  CompletionList as LSPCompletionList,
  DocumentSymbol as LSPDocumentSymbol,
  Hover,
  MarkupKind,
} from "vscode-languageserver/node.js";
import {
  CompletionItem as XmlCompletionItem,
  DocumentSymbol as XmlDocumentSymbol,
  HoverResult,
  FoldingRange as XmlFoldingRange,
} from "xml-language-service";

// ── String utilities ─────────────────────────────────────────────────────────

export function getFileName(uri: string): string {
  return uri.split("/").pop()!;
}

export function getDocumentPath(uri: string): string | undefined {
  if (!uri.startsWith("file://")) return undefined;
  return decodeURIComponent(uri.replace("file://", ""));
}

export function escapeMd(text: string): string {
  return text.replace(/[<>&]/g, (c) => ({ "<": "&lt;", ">": "&gt;", "&": "&amp;" }[c]!));
}

export function formatError(error: unknown): string {
  if (error instanceof Error) return error.stack ?? error.message;
  return String(error);
}

// ── LSP type adapters ────────────────────────────────────────────────────────

const COMPLETION_KIND_MAP: Record<XmlCompletionItem["kind"], CompletionItemKind> = {
  element: CompletionItemKind.Class,
  attribute: CompletionItemKind.Property,
  value: CompletionItemKind.Value,
  closeTag: CompletionItemKind.Keyword,
};

const DOCUMENT_SYMBOL_KIND_MAP: Record<XmlDocumentSymbol["kind"], SymbolKind> = {
  namespace: SymbolKind.Namespace,
  class: SymbolKind.Class,
  method: SymbolKind.Method,
  function: SymbolKind.Function,
  property: SymbolKind.Property,
  field: SymbolKind.Field,
  interface: SymbolKind.Interface,
  struct: SymbolKind.Struct,
  array: SymbolKind.Array,
  object: SymbolKind.Object,
};

export function toLSPCompletionList(list: {
  items: XmlCompletionItem[];
  isIncomplete: boolean;
}): LSPCompletionList {
  return {
    isIncomplete: list.isIncomplete,
    items: list.items.map((item) => ({
      label: item.label,
      kind: COMPLETION_KIND_MAP[item.kind],
      insertText: item.insertText,
      detail: item.detail,
    })),
  };
}

export function toLSPDocumentSymbol(sym: XmlDocumentSymbol): LSPDocumentSymbol {
  return {
    name: sym.name,
    kind: DOCUMENT_SYMBOL_KIND_MAP[sym.kind],
    range: sym.range,
    selectionRange: sym.selectionRange,
    children: sym.children.map(toLSPDocumentSymbol),
  };
}

export function toLSPHover(result: HoverResult): Hover {
  return {
    contents: { kind: MarkupKind.Markdown, value: result.contents },
    range: result.range,
  };
}

export function toLSPFoldingRange(r: XmlFoldingRange): LSPFoldingRange {
  return { startLine: r.startLine, endLine: r.endLine, kind: r.kind };
}
