import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import { findReferences } from "../../src/services/xmlReferences.js";

const xml = "<root><item/><item/><item/></root>";
const uri = "file:///test.xml";

describe("findReferences", () => {
  const doc = parseXMLDocument(uri, xml);

  it("findReferences on 'item' returns 3 results", () => {
    // offset 7 is inside the first <item/>  (starts at 6)
    const results = findReferences(doc, { line: 0, character: 7 });
    expect(results).toHaveLength(3);
  });

  it("all results have the correct uri", () => {
    const results = findReferences(doc, { line: 0, character: 7 });
    for (const r of results) {
      expect(r.uri).toBe(uri);
    }
  });

  it("all results have valid ranges with start before end", () => {
    const results = findReferences(doc, { line: 0, character: 7 });
    for (const r of results) {
      const { start, end } = r.range;
      const startsBefore =
        start.line < end.line ||
        (start.line === end.line && start.character <= end.character);
      expect(startsBefore).toBe(true);
    }
  });

  it("findReferences on 'root' returns 1 result (single root element)", () => {
    // offset 1 is inside <root>
    const results = findReferences(doc, { line: 0, character: 1 });
    expect(results).toHaveLength(1);
  });

  it("findReferences on an empty document returns []", () => {
    const emptyDoc = parseXMLDocument(uri, "");
    const results = findReferences(emptyDoc, { line: 0, character: 0 });
    expect(results).toHaveLength(0);
  });

  it("all ReferenceResult objects have both uri and range properties", () => {
    const results = findReferences(doc, { line: 0, character: 7 });
    for (const r of results) {
      expect(typeof r.uri).toBe("string");
      expect(r.range).toBeDefined();
      expect(r.range.start).toBeDefined();
      expect(r.range.end).toBeDefined();
    }
  });

  it("cursor on text content returns references for the enclosing element (AST behavior)", () => {
    const xmlWithText = "<root>hello world</root>";
    const docWithText = parseXMLDocument(uri, xmlWithText);
    // findNodeAt on text content returns the enclosing element node,
    // so findReferences returns references to 'root' (1 result)
    const results = findReferences(docWithText, { line: 0, character: 7 });
    expect(Array.isArray(results)).toBe(true);
    // The parser returns the enclosing 'root' element — at least 1 result
    expect(results.length).toBeGreaterThanOrEqual(1);
  });

  it("multi-line document: references across lines all report correct uri", () => {
    const multiLine = "<root>\n  <item/>\n  <item/>\n</root>";
    const multiDoc = parseXMLDocument(uri, multiLine);
    // line 1, character 3 is inside the first <item/>
    const results = findReferences(multiDoc, { line: 1, character: 3 });
    expect(results.length).toBeGreaterThanOrEqual(2);
    for (const r of results) {
      expect(r.uri).toBe(uri);
    }
  });

  it("range start character is non-negative for all results", () => {
    const results = findReferences(doc, { line: 0, character: 7 });
    for (const r of results) {
      expect(r.range.start.character).toBeGreaterThanOrEqual(0);
      expect(r.range.start.line).toBeGreaterThanOrEqual(0);
    }
  });
});
