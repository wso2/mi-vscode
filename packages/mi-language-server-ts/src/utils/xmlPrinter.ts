import { XMLDocument, XMLNode, XMLAttribute } from "../parser/xmlNode.js";

// ── Tree-style ────────────────────────────────────────────────────────────────

/**
 * Renders the AST as a terminal-style tree using box-drawing characters,
 * identical to the `tree` command output.
 *
 * Example:
 *   Document
 *   └── root
 *       ├── child
 *       └── sibling
 */
export function printTreeAST(document: XMLDocument): string {
  const lines: string[] = ["Document"];
  renderTreeNodes(document.children.filter((c) => c.type === "element"), "", lines);
  return lines.join("\n");
}

function renderTreeNodes(nodes: XMLNode[], prefix: string, lines: string[]): void {
  nodes.forEach((node, i) => {
    const isLast = i === nodes.length - 1;
    lines.push(prefix + (isLast ? "└── " : "├── ") + (node.name ?? node.type));
    const childPrefix = prefix + (isLast ? "    " : "│   ");
    renderTreeNodes(node.children.filter((c) => c.type === "element"), childPrefix, lines);
  });
}

export type PrintFormat = "tree" | "json";

export interface PrintOptions {
  /** Spaces per indent level. Default: 2 */
  indent?: number;
  /** Include startOffset/endOffset on every node. Default: false */
  includePositions?: boolean;
  /** Include raw tokens in CST output. Default: true */
  includeTokens?: boolean;
  /** Output format. Default: "tree" */
  format?: PrintFormat;
}

interface ResolvedOptions {
  indent: number;
  includePositions: boolean;
  includeTokens: boolean;
  format: PrintFormat;
}

function resolveOptions(opts?: PrintOptions): ResolvedOptions {
  return {
    indent: opts?.indent ?? 2,
    includePositions: opts?.includePositions ?? false,
    includeTokens: opts?.includeTokens ?? true,
    format: opts?.format ?? "tree",
  };
}

// ── AST ───────────────────────────────────────────────────────────────────────

/**
 * Pretty-prints the AST (XMLNode tree) of a parsed document.
 */
export function printAST(document: XMLDocument, options?: PrintOptions): string {
  const opts = resolveOptions(options);
  if (opts.format === "json") {
    return JSON.stringify(astNodeToJSON(document, opts), null, opts.indent);
  }
  const lines: string[] = [];
  renderASTNode(document, opts, 0, lines);
  return lines.join("\n");
}

function nodePos(node: XMLNode, opts: ResolvedOptions): string {
  return opts.includePositions ? ` [${node.startOffset}..${node.endOffset}]` : "";
}

function attrPos(attr: XMLAttribute, opts: ResolvedOptions): string {
  return opts.includePositions
    ? ` [${attr.nameStart}..${attr.valueEnd ?? attr.nameEnd}]`
    : "";
}

function renderASTNode(node: XMLNode, opts: ResolvedOptions, depth: number, lines: string[]): void {
  const pad = " ".repeat(depth * opts.indent);
  const childPad = " ".repeat((depth + 1) * opts.indent);

  if (node.type === "root") {
    const doc = node as XMLDocument;
    const errStr = doc.syntaxErrors.length > 0 ? ` (${doc.syntaxErrors.length} syntax error(s))` : "";
    lines.push(`${pad}Document${nodePos(node, opts)}${errStr}`);
    for (const child of node.children) {
      renderASTNode(child, opts, depth + 1, lines);
    }
    return;
  }

  if (node.type === "element") {
    const selfClose = node.isSelfClosing ? " /" : "";
    lines.push(`${pad}Element <${node.name ?? "?"}>` + selfClose + nodePos(node, opts));
    for (const attr of node.attributes) {
      const val = attr.value !== undefined ? `="${attr.value}"` : "";
      lines.push(`${childPad}Attribute ${attr.name}${val}${attrPos(attr, opts)}`);
    }
    for (const child of node.children) {
      renderASTNode(child, opts, depth + 1, lines);
    }
    return;
  }

  if (node.type === "text") {
    lines.push(`${pad}Text${nodePos(node, opts)}`);
    return;
  }

  if (node.type === "comment") {
    lines.push(`${pad}Comment${nodePos(node, opts)}`);
    return;
  }

  lines.push(`${pad}(${node.type})${nodePos(node, opts)}`);
}

function astNodeToJSON(node: XMLNode, opts: ResolvedOptions): Record<string, unknown> {
  const out: Record<string, unknown> = { type: node.type };

  if (node.name !== undefined) out.name = node.name;

  if (node.type === "root") {
    const doc = node as XMLDocument;
    out.uri = doc.uri;
    if (doc.syntaxErrors.length > 0) out.syntaxErrors = doc.syntaxErrors;
  }

  if (opts.includePositions) {
    out.startOffset = node.startOffset;
    out.endOffset = node.endOffset;
  }

  if (node.isSelfClosing) out.isSelfClosing = true;

  if (node.attributes.length > 0) {
    out.attributes = node.attributes.map((attr) => {
      const a: Record<string, unknown> = { name: attr.name };
      if (attr.value !== undefined) a.value = attr.value;
      if (opts.includePositions) {
        a.nameStart = attr.nameStart;
        a.nameEnd = attr.nameEnd;
        if (attr.valueStart !== undefined) a.valueStart = attr.valueStart;
        if (attr.valueEnd !== undefined) a.valueEnd = attr.valueEnd;
      }
      return a;
    });
  }

  if (node.children.length > 0) {
    out.children = node.children.map((child) => astNodeToJSON(child, opts));
  }

  return out;
}

// ── CST ───────────────────────────────────────────────────────────────────────

/**
 * Pretty-prints the raw Chevrotain CST captured during parsing.
 * The CST shows every grammar rule and token, including structural syntax (`<`, `>`, `=`).
 */
export function printCST(document: XMLDocument, options?: PrintOptions): string {
  const rawCST = (document as any).rawCST;
  if (rawCST == null) {
    return "(CST not available — document was not created via parseXMLDocument)";
  }
  const opts = resolveOptions(options);
  if (opts.format === "json") {
    return JSON.stringify(cstNodeToJSON(rawCST, opts), null, opts.indent);
  }
  const lines: string[] = [];
  renderCSTNode(rawCST, opts, 0, lines);
  return lines.join("\n");
}

type CSTRule = { name: string; children: Record<string, unknown[]>; location?: Record<string, number> };
type CSTToken = { image: string; tokenType?: { name?: string }; startOffset?: number; endOffset?: number };

function isCSTRule(node: unknown): node is CSTRule {
  return (
    typeof node === "object" &&
    node !== null &&
    "name" in node &&
    "children" in node &&
    typeof (node as any).children === "object" &&
    !("image" in node)
  );
}

function isCSTToken(node: unknown): node is CSTToken {
  return typeof node === "object" && node !== null && "image" in node;
}

function cstTokenPos(token: CSTToken, opts: ResolvedOptions): string {
  return opts.includePositions && token.startOffset !== undefined
    ? ` [${token.startOffset}..${token.endOffset}]`
    : "";
}

function cstRulePos(rule: CSTRule, opts: ResolvedOptions): string {
  return opts.includePositions && rule.location
    ? ` [${rule.location.startOffset}..${rule.location.endOffset}]`
    : "";
}

function renderCSTNode(node: unknown, opts: ResolvedOptions, depth: number, lines: string[]): void {
  const pad = " ".repeat(depth * opts.indent);
  const childPad = " ".repeat((depth + 1) * opts.indent);

  if (isCSTRule(node)) {
    lines.push(`${pad}(${node.name})${cstRulePos(node, opts)}`);
    for (const [key, items] of Object.entries(node.children)) {
      for (const item of items) {
        if (isCSTToken(item)) {
          if (!opts.includeTokens) continue;
          const tokenName = item.tokenType?.name ?? key;
          lines.push(`${childPad}${tokenName}: ${JSON.stringify(item.image)}${cstTokenPos(item, opts)}`);
        } else {
          renderCSTNode(item, opts, depth + 1, lines);
        }
      }
    }
    return;
  }

  if (isCSTToken(node)) {
    if (!opts.includeTokens) return;
    const tokenName = node.tokenType?.name ?? "Token";
    lines.push(`${pad}${tokenName}: ${JSON.stringify(node.image)}${cstTokenPos(node, opts)}`);
    return;
  }

  lines.push(`${pad}(unknown)`);
}

function cstNodeToJSON(node: unknown, opts: ResolvedOptions): unknown {
  if (isCSTRule(node)) {
    const out: Record<string, unknown> = { rule: node.name };
    if (opts.includePositions && node.location) out.location = node.location;
    const childrenOut: Record<string, unknown[]> = {};
    for (const [key, items] of Object.entries(node.children)) {
      const converted: unknown[] = [];
      for (const item of items) {
        if (isCSTToken(item)) {
          if (!opts.includeTokens) continue;
          const t: Record<string, unknown> = {
            token: item.tokenType?.name ?? key,
            image: item.image,
          };
          if (opts.includePositions) {
            t.startOffset = item.startOffset;
            t.endOffset = item.endOffset;
          }
          converted.push(t);
        } else {
          const child = cstNodeToJSON(item, opts);
          if (child !== null) converted.push(child);
        }
      }
      if (converted.length > 0) childrenOut[key] = converted;
    }
    if (Object.keys(childrenOut).length > 0) out.children = childrenOut;
    return out;
  }

  if (isCSTToken(node)) {
    if (!opts.includeTokens) return null;
    const t: Record<string, unknown> = {
      token: node.tokenType?.name ?? "Token",
      image: node.image,
    };
    if (opts.includePositions) {
      t.startOffset = node.startOffset;
      t.endOffset = node.endOffset;
    }
    return t;
  }

  return null;
}
