import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";

const simpleXML = `<root><child name="test">hello</child></root>`;
const multiLineXML = `<root>\n  <child/>\n</root>`;
const brokenXML = `<root><unclosed>`;

describe("parseXMLDocument return value", () => {
  it("returns a non-null XMLDocument", () => {
    const result = parseXMLDocument("file:///test.xml", simpleXML);
    expect(result).toBeDefined();
    expect(result).not.toBeNull();
  });

  it("preserves the uri passed in", () => {
    const uri = "file:///test.xml";
    expect(parseXMLDocument(uri, simpleXML).uri).toBe(uri);
  });

  it("preserves the source text", () => {
    const result = parseXMLDocument("file:///test.xml", simpleXML);
    expect(result.text).toBe(simpleXML);
  });

  it("has type 'root'", () => {
    expect(parseXMLDocument("file:///test.xml", simpleXML).type).toBe("root");
  });
});

describe("tree structure on simpleXML", () => {
  const doc = parseXMLDocument("file:///test.xml", simpleXML);

  it("has at least one top-level child", () => {
    expect(doc.children.length).toBeGreaterThan(0);
  });

  it("first child is named 'root'", () => {
    expect(doc.children[0].name).toBe("root");
  });

  it("root element has children", () => {
    expect(doc.children[0].children.length).toBeGreaterThan(0);
  });

  it("inner child is named 'child'", () => {
    const inner = doc.children[0].children[0];
    expect(inner.name).toBe("child");
  });

  it("inner child has a 'name' attribute with value 'test'", () => {
    const inner = doc.children[0].children[0];
    const attr = inner.attributes.find((a) => a.name === "name");
    expect(attr).toBeDefined();
    expect(attr?.value).toBe("test");
  });
});

describe("findNodeAt on simpleXML", () => {
  const doc = parseXMLDocument("file:///test.xml", simpleXML);

  it("findNodeAt(0) returns a node (document root or root element)", () => {
    const node = doc.findNodeAt(0);
    expect(node).toBeDefined();
  });

  it("findNodeAt(1) returns a node inside the document", () => {
    const node = doc.findNodeAt(1);
    expect(node).toBeDefined();
    expect(node.type).not.toBe(undefined);
  });

  it("findNodeAt at an offset inside the child tag returns the child node", () => {
    // <root><child ...> starts at offset 6, so offset 7 is inside the child tag
    const childStart = simpleXML.indexOf("<child");
    const node = doc.findNodeAt(childStart + 1);
    expect(node.name).toBe("child");
  });
});

describe("fault tolerance on brokenXML", () => {
  it("does not throw when parsing broken XML", () => {
    expect(() => parseXMLDocument("file:///broken.xml", brokenXML)).not.toThrow();
  });

  it("returns a valid XMLDocument even for broken XML", () => {
    const result = parseXMLDocument("file:///broken.xml", brokenXML);
    expect(result).toBeDefined();
    expect(result.type).toBe("root");
    expect(result.text).toBe(brokenXML);
  });
});
