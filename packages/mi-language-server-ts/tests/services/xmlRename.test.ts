import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import { doRename } from "../../src/services/xmlRename.js";

// <root><child>text</child></root>
// offset 1 = 'r' in <root>
// offset 27 = 'r' in </root>
const xml = "<root><child>text</child></root>";
const uri = "file:///test.xml";

describe("doRename", () => {
  const doc = parseXMLDocument(uri, xml);

  it("rename at a position inside 'root' returns 2 TextEdits", () => {
    const edits = doRename(doc, { line: 0, character: 1 }, "element");
    expect(edits).not.toBeNull();
    expect(edits).toHaveLength(2);
  });

  it("first edit targets the opening tag name", () => {
    const edits = doRename(doc, { line: 0, character: 1 }, "element")!;
    // '<root>' — name starts at offset 1
    expect(edits[0].startOffset).toBe(1);
    expect(edits[0].newText).toBe("element");
  });

  it("second edit targets the closing tag name", () => {
    const edits = doRename(doc, { line: 0, character: 1 }, "element")!;
    // '</root>' — name starts at offset 27
    expect(edits[1].startOffset).toBe(27);
    expect(edits[1].newText).toBe("element");
  });

  it("rename with an empty string returns null", () => {
    expect(doRename(doc, { line: 0, character: 1 }, "")).toBeNull();
  });

  it("rename with '<' in the name returns null", () => {
    expect(doRename(doc, { line: 0, character: 1 }, "<bad>")).toBeNull();
  });

  it("rename with '>' in the name returns null", () => {
    expect(doRename(doc, { line: 0, character: 1 }, "bad>tag")).toBeNull();
  });

  it("rename with spaces in the name returns null", () => {
    expect(doRename(doc, { line: 0, character: 1 }, "bad name")).toBeNull();
  });

  it("rename a self-closing tag returns 1 TextEdit", () => {
    const selfDoc = parseXMLDocument(uri, "<root><br/></root>");
    // offset 7 = 'b' in <br/>
    const edits = doRename(selfDoc, { line: 0, character: 7 }, "img");
    expect(edits).not.toBeNull();
    expect(edits).toHaveLength(1);
    expect(edits![0].newText).toBe("img");
  });

  it("rename a self-closing tag edit has a valid endOffset > startOffset", () => {
    const selfDoc = parseXMLDocument(uri, "<root><br/></root>");
    const edits = doRename(selfDoc, { line: 0, character: 7 }, "img")!;
    expect(edits[0].endOffset).toBeGreaterThan(edits[0].startOffset);
  });

  it("both edits for an open+close pair have endOffset > startOffset", () => {
    const edits = doRename(doc, { line: 0, character: 1 }, "element")!;
    expect(edits[0].endOffset).toBeGreaterThan(edits[0].startOffset);
    expect(edits[1].endOffset).toBeGreaterThan(edits[1].startOffset);
  });

  it("both rename edits use the new name as newText", () => {
    const newName = "renamed";
    const edits = doRename(doc, { line: 0, character: 1 }, newName)!;
    expect(edits.every((e) => e.newText === newName)).toBe(true);
  });

  it("cursor on text content renames the enclosing element (AST behavior)", () => {
    // findNodeAt returns the enclosing element, not a text node.
    // So rename at offset 13 (inside 'text' in <child>text</child>) renames 'child'.
    const result = doRename(doc, { line: 0, character: 13 }, "newname");
    // Result is non-null: the enclosing 'child' element is renamed
    expect(result).not.toBeNull();
    expect(result!.every((e) => e.newText === "newname")).toBe(true);
  });
});
