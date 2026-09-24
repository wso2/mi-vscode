/** Represents a single attribute on an XML element, including its name and optional value with source offsets. */
export interface XMLAttribute {
  name: string;
  value: string | undefined;
  nameStart: number;
  nameEnd: number;
  valueStart: number | undefined;
  valueEnd: number | undefined;
}

/** A syntax error produced by the fault-tolerant CST parser (lex or parse phase). */
export interface SyntaxError {
  message: string;
  /** 0-based line number */
  line: number;
  /** 0-based character offset */
  character: number;
}

/** Represents a node in the XML parse tree, covering elements, text, comments, and the root. */
export interface XMLNode {
  type: "element" | "text" | "comment" | "root";
  name: string | undefined;
  attributes: XMLAttribute[];
  children: XMLNode[];
  startOffset: number;
  endOffset: number;
  parent: XMLNode | undefined;
  isSelfClosing: boolean;
}

/** Represents the root of a parsed XML document, extending XMLNode with document-level metadata. */
export interface XMLDocument extends XMLNode {
  type: "root";
  uri: string;
  text: string;
  syntaxErrors: SyntaxError[];
  findNodeAt(offset: number): XMLNode;
  traverse(callback: (node: XMLNode) => void): void;
}
