import { XMLDocument, XMLNode } from "../parser/xmlNode.js";
import { Range, offsetsToRange } from "../utils/rangeUtils.js";

export type DocumentSymbolKind =
  | "namespace"
  | "class"
  | "method"
  | "function"
  | "property"
  | "field"
  | "interface"
  | "struct"
  | "array"
  | "object";

/** Represents a named symbol (element) in the XML document, mirroring the LSP DocumentSymbol structure. */
export interface DocumentSymbol {
  name: string;
  kind: DocumentSymbolKind;
  range: Range;
  selectionRange: Range;
  children: DocumentSymbol[];
}

const ARTIFACT_ELEMENTS = new Set([
  "api",
  "endpoint",
  "inboundEndpoint",
  "inbound-endpoint",
  "localEntry",
  "local-entry",
  "messageProcessor",
  "message-processor",
  "messageStore",
  "message-store",
  "proxy",
  "proxy-service",
  "sequence",
  "task",
  "template",
]);

const FLOW_ELEMENTS = new Set([
  "faultSequence",
  "inSequence",
  "outSequence",
  "resource",
  "target",
]);

const PROPERTY_ELEMENTS = new Set([
  "arg",
  "header",
  "parameter",
  "property",
  "variable",
]);

const ENDPOINT_ELEMENTS = new Set([
  "address",
  "failover",
  "http",
  "loadbalance",
  "recipientlist",
  "wsdl",
]);

const MEDIATOR_ELEMENTS = new Set([
  "call",
  "call-template",
  "class",
  "clone",
  "dblookup",
  "dbreport",
  "drop",
  "enrich",
  "filter",
  "iterate",
  "log",
  "loopback",
  "payloadFactory",
  "respond",
  "send",
  "smooks",
  "store",
  "switch",
  "throttle",
  "validate",
]);

/**
 * Walks the XML document tree and returns a hierarchical list of DocumentSymbol objects,
 * one per element node, preserving parent-child nesting.
 */
export function findDocumentSymbols(document: XMLDocument): DocumentSymbol[] {
  return buildSymbols(document.children, document.text);
}

function buildSymbols(nodes: XMLNode[], text: string): DocumentSymbol[] {
  const symbols: DocumentSymbol[] = [];
  for (const node of nodes) {
    if (node.type !== "element" || !node.name) continue;
    symbols.push({
      name: getSymbolName(node),
      kind: getSymbolKind(node),
      range: offsetsToRange(text, node.startOffset, node.endOffset),
      // selectionRange covers '<' + the tag name
      selectionRange: offsetsToRange(
        text,
        node.startOffset,
        node.startOffset + node.name.length + 1
      ),
      children: buildSymbols(node.children, text),
    });
  }
  return symbols;
}

function getSymbolName(node: XMLNode): string {
  const identifier = node.attributes.find((attr) =>
    ["name", "id", "key"].includes(attr.name)
  )?.value;

  return identifier != null && identifier !== ""
    ? `${node.name} (${identifier})`
    : node.name ?? "";
}

function getSymbolKind(node: XMLNode): DocumentSymbolKind {
  const name = node.name;
  if (!name) return "object";

  if (name === "definitions") return "namespace";
  if (FLOW_ELEMENTS.has(name)) return "method";
  if (PROPERTY_ELEMENTS.has(name)) return "property";
  if (ENDPOINT_ELEMENTS.has(name)) return "interface";
  if (name === "args") return "array";
  if (name === "format") return "struct";
  if (MEDIATOR_ELEMENTS.has(name)) return "function";
  if (ARTIFACT_ELEMENTS.has(name) && hasIdentifier(node)) return "class";

  return node.children.length > 0 ? "object" : "field";
}

function hasIdentifier(node: XMLNode): boolean {
  return node.attributes.some(
    (attr) =>
      ["name", "id", "key"].includes(attr.name) &&
      attr.value != null &&
      attr.value !== ""
  );
}
