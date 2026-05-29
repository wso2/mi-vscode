import { XMLDocument, XMLNode } from "../parser/xmlNode.js";

/** Options controlling how the formatter re-indents the document. */
export interface FormatterOptions {
  tabSize: number;
  insertSpaces: boolean;
  maxLineLength?: number;
}

/** Represents a replacement of a text range within a document. */
export interface TextEdit {
  startOffset: number;
  endOffset: number;
  newText: string;
}

/**
 * Re-indents the entire XML document using the parsed XML tree and returns
 * a single TextEdit that replaces the full document text with the formatted version.
 * Returns an empty array for empty documents. Does not modify the AST.
 */
export function format(document: XMLDocument, options?: FormatterOptions): TextEdit[] {
  if (!document.text) return [];

  const tabSize = options?.tabSize ?? 2;
  const insertSpaces = options?.insertSpaces ?? true;
  const unit = insertSpaces ? " ".repeat(tabSize) : "\t";

  const output = formatTopLevel(document, unit);

  return [{ startOffset: 0, endOffset: document.text.length, newText: output.join("\n") }];
}

function formatTopLevel(document: XMLDocument, unit: string): string[] {
  const output: string[] = [];
  let cursor = 0;

  for (const child of document.children) {
    output.push(...formatLooseContent(document.text.slice(cursor, child.startOffset), 0, unit));
    output.push(...formatNode(child, document.text, 0, unit));
    cursor = child.endOffset;
  }

  output.push(...formatLooseContent(document.text.slice(cursor), 0, unit));
  return output;
}

function formatNode(node: XMLNode, text: string, level: number, unit: string): string[] {
  const openTagEnd = findOpeningTagEnd(text, node.startOffset);
  if (openTagEnd < 0 || node.isSelfClosing) {
    return [indent(level, unit) + normalizeTag(text.slice(node.startOffset, node.endOffset))];
  }

  const closeTagStart = findClosingTagStart(text, node, openTagEnd);
  if (closeTagStart < 0) {
    return [indent(level, unit) + normalizeTag(text.slice(node.startOffset, node.endOffset))];
  }

  const openTag = normalizeTag(text.slice(node.startOffset, openTagEnd + 1));
  const closeTag = normalizeTag(text.slice(closeTagStart, node.endOffset));
  const contentStart = openTagEnd + 1;
  const contentEnd = closeTagStart;
  const content = text.slice(contentStart, contentEnd);

  if (node.children.length === 0) {
    return formatLeafNode(openTag, closeTag, content, level, unit);
  }

  if (hasMeaningfulTextBetweenChildren(node, text, contentStart, contentEnd)) {
    return formatMixedContentNode(node, text, level, unit);
  }

  const output = [indent(level, unit) + openTag];
  let cursor = contentStart;

  for (const child of node.children) {
    output.push(...formatLooseContent(text.slice(cursor, child.startOffset), level + 1, unit));
    output.push(...formatNode(child, text, level + 1, unit));
    cursor = child.endOffset;
  }

  output.push(...formatLooseContent(text.slice(cursor, contentEnd), level + 1, unit));
  output.push(indent(level, unit) + closeTag);
  return output;
}

function formatLeafNode(
  openTag: string,
  closeTag: string,
  content: string,
  level: number,
  unit: string
): string[] {
  if (!content.trim()) {
    if (content.includes("\n") || content.includes("\r")) {
      return [indent(level, unit) + openTag, indent(level, unit) + closeTag];
    }

    return [indent(level, unit) + openTag + closeTag];
  }

  if (!hasMeaningfulText(content)) {
    return [
      indent(level, unit) + openTag,
      ...formatLooseContent(content, level + 1, unit),
      indent(level, unit) + closeTag,
    ];
  }

  if (!content.includes("\n") && !content.includes("\r")) {
    return [indent(level, unit) + openTag + content.trim() + closeTag];
  }

  return [
    indent(level, unit) + openTag,
    ...formatTextLines(content, level + 1, unit),
    indent(level, unit) + closeTag,
  ];
}

function formatMixedContentNode(
  node: XMLNode,
  text: string,
  level: number,
  unit: string
): string[] {
  const raw = text.slice(node.startOffset, node.endOffset).trim();
  return raw.split(/\r?\n/).map((line) => indent(level, unit) + line.trim());
}

function hasMeaningfulTextBetweenChildren(
  node: XMLNode,
  text: string,
  contentStart: number,
  contentEnd: number
): boolean {
  let cursor = contentStart;

  for (const child of node.children) {
    if (hasMeaningfulText(text.slice(cursor, child.startOffset))) return true;
    cursor = child.endOffset;
  }

  return hasMeaningfulText(text.slice(cursor, contentEnd));
}

function formatLooseContent(content: string, level: number, unit: string): string[] {
  const output: string[] = [];
  const tokenPattern = /(<!--[\s\S]*?-->|<\?[\s\S]*?\?>)/g;
  let cursor = 0;
  let match: RegExpExecArray | null;

  while ((match = tokenPattern.exec(content)) != null) {
    output.push(...formatTextLines(content.slice(cursor, match.index), level, unit));
    output.push(...formatToken(match[0], level, unit));
    cursor = match.index + match[0].length;
  }

  output.push(...formatTextLines(content.slice(cursor), level, unit));
  return output;
}

function formatToken(token: string, level: number, unit: string): string[] {
  return token
    .trim()
    .split(/\r?\n/)
    .map((line) => indent(level, unit) + line.trim());
}

function formatTextLines(text: string, level: number, unit: string): string[] {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => indent(level, unit) + line);
}

function hasMeaningfulText(text: string): boolean {
  return text.replace(/<!--[\s\S]*?-->|<\?[\s\S]*?\?>/g, "").trim() !== "";
}

function findOpeningTagEnd(text: string, startOffset: number): number {
  let quote: string | undefined;

  for (let i = startOffset; i < text.length; i++) {
    const char = text[i];
    if (quote != null) {
      if (char === quote) quote = undefined;
    } else if (char === "\"" || char === "'") {
      quote = char;
    } else if (char === ">") {
      return i;
    }
  }

  return -1;
}

function findClosingTagStart(text: string, node: XMLNode, openTagEnd: number): number {
  if (!node.name) return -1;
  const closeTagStart = text.lastIndexOf(`</${node.name}`, node.endOffset - 1);
  return closeTagStart > openTagEnd ? closeTagStart : -1;
}

function normalizeTag(tag: string): string {
  const trimmed = tag.trim();
  let output = "";
  let quote: string | undefined;
  let pendingWhitespace = false;

  for (const char of trimmed) {
    if (quote != null) {
      output += char;
      if (char === quote) quote = undefined;
      continue;
    }

    if (char === "\"" || char === "'") {
      if (pendingWhitespace && output && !output.endsWith("<")) output += " ";
      output += char;
      pendingWhitespace = false;
      quote = char;
      continue;
    }

    if (/\s/.test(char)) {
      pendingWhitespace = true;
      continue;
    }

    if (pendingWhitespace && output && char !== ">" && !output.endsWith("<")) {
      output += " ";
    }

    output += char;
    pendingWhitespace = false;
  }

  return output;
}

function indent(level: number, unit: string): string {
  return unit.repeat(level);
}
