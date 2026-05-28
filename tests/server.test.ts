import { describe, it, expect } from "vitest";
import { TextDocument } from "vscode-languageserver-textdocument";
import { getLanguageService } from "xml-language-service";
import { toXMLDocument, toXMLPosition } from "../src/textDocumentUtils.js";

const sampleXml = `<root>\n  <child name="test"/>\n</root>`;
const doc = TextDocument.create("file:///test.xml", "xml", 1, sampleXml);
const service = getLanguageService();

describe("toXMLDocument", () => {
  it("returns an object with uri matching the input document", () => {
    const xmlDoc = toXMLDocument(doc);
    expect(xmlDoc.uri).toBe("file:///test.xml");
  });

  it("returns an object with text matching the input document", () => {
    const xmlDoc = toXMLDocument(doc);
    expect(xmlDoc.text).toBe(sampleXml);
  });

  it("returns an XMLDocument with type 'root' (XMLDocument is the root node)", () => {
    const xmlDoc = toXMLDocument(doc);
    expect(xmlDoc.type).toBe("root");
  });

  it("XMLDocument has a children array", () => {
    const xmlDoc = toXMLDocument(doc);
    expect(Array.isArray(xmlDoc.children)).toBe(true);
  });
});

describe("toXMLPosition", () => {
  it("returns { line: 0, character: 0 } unchanged", () => {
    const pos = toXMLPosition({ line: 0, character: 0 });
    expect(pos).toEqual({ line: 0, character: 0 });
  });

  it("returns { line: 1, character: 5 } unchanged", () => {
    const pos = toXMLPosition({ line: 1, character: 5 });
    expect(pos).toEqual({ line: 1, character: 5 });
  });
});

describe("xml-language-service via bridge", () => {
  it("parseXMLDocument returns a valid XMLDocument", () => {
    const xmlDoc = service.parseXMLDocument("file:///test.xml", sampleXml);
    expect(xmlDoc).toBeDefined();
    expect(xmlDoc.uri).toBe("file:///test.xml");
  });

  it("doComplete returns a CompletionList (not null)", () => {
    const xmlDoc = toXMLDocument(doc);
    const result = service.doComplete(xmlDoc, { line: 0, character: 1 });
    expect(result).not.toBeNull();
    expect(typeof result).toBe("object");
  });

  it("doHover at (0, 1) returns a result or null without throwing", () => {
    const xmlDoc = toXMLDocument(doc);
    let result: unknown;
    expect(() => {
      result = service.doHover(xmlDoc, { line: 0, character: 1 });
    }).not.toThrow();
    // result may be null or a hover object — both are valid
    expect(result === null || typeof result === "object").toBe(true);
  });

  it("findDocumentSymbols returns an array", () => {
    const xmlDoc = toXMLDocument(doc);
    const symbols = service.findDocumentSymbols(xmlDoc);
    expect(Array.isArray(symbols)).toBe(true);
  });

  it("getFoldingRanges returns an array", () => {
    const xmlDoc = toXMLDocument(doc);
    const ranges = service.getFoldingRanges(xmlDoc);
    expect(Array.isArray(ranges)).toBe(true);
  });
});
