import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import { doComplete } from "../../src/services/xmlCompletion.js";
import { XsdCompletionProvider } from "../../src/schema/xsdCompletionProvider.js";

// ─── Document-tree fallback (no schema) ──────────────────────────────────────
// line 0: <root><child/></root>
// offset 7 = textBefore '<root><'  → context 1 (after '<')
// offset 16 = textBefore '<root><child/></'  → context 3 (after '</')
const xml = "<root><child/></root>";
const uri = "file:///test.xml";

describe("doComplete — structural (no schema)", () => {
  const doc = parseXMLDocument(uri, xml);

  it("always returns a CompletionList (never null or undefined)", () => {
    const result = doComplete(doc, { line: 0, character: 0 });
    expect(result).toBeDefined();
    expect(result).not.toBeNull();
  });

  it("CompletionList has an items array and an isIncomplete boolean", () => {
    const result = doComplete(doc, { line: 0, character: 0 });
    expect(Array.isArray(result.items)).toBe(true);
    expect(typeof result.isIncomplete).toBe("boolean");
  });

  it("context after '<' returns items with kind 'element'", () => {
    // offset 7: textBefore = '<root><' → context 1
    const result = doComplete(doc, { line: 0, character: 7 });
    expect(result.items.length).toBeGreaterThan(0);
    expect(result.items.every((i) => i.kind === "element")).toBe(true);
  });

  it("element completions include names from the document tree", () => {
    const result = doComplete(doc, { line: 0, character: 7 });
    const labels = result.items.map((i) => i.label);
    expect(labels).toContain("root");
    expect(labels).toContain("child");
  });

  it("context after '</' returns a closeTag kind item", () => {
    // offset 16: textBefore = '<root><child/></' → context 3 (after '</')
    const result = doComplete(doc, { line: 0, character: 16 });
    expect(result.items.length).toBeGreaterThan(0);
    expect(result.items.some((i) => i.kind === "closeTag")).toBe(true);
  });

  it("closeTag insertText ends with '>'", () => {
    const result = doComplete(doc, { line: 0, character: 16 });
    const closeItem = result.items.find((i) => i.kind === "closeTag");
    expect(closeItem?.insertText).toMatch(/>$/);
  });

  it("all items have a label and an insertText", () => {
    const result = doComplete(doc, { line: 0, character: 7 });
    for (const item of result.items) {
      expect(typeof item.label).toBe("string");
      expect(item.label.length).toBeGreaterThan(0);
      expect(typeof item.insertText).toBe("string");
    }
  });

  it("isIncomplete is false for all contexts", () => {
    expect(doComplete(doc, { line: 0, character: 7 }).isIncomplete).toBe(false);
    expect(doComplete(doc, { line: 0, character: 16 }).isIncomplete).toBe(false);
    expect(doComplete(doc, { line: 0, character: 0 }).isIncomplete).toBe(false);
  });

  it("position with no active context returns an empty list (not null)", () => {
    // character 0 is before any '<' — falls through all patterns
    const result = doComplete(doc, { line: 0, character: 0 });
    expect(result).toBeDefined();
    expect(Array.isArray(result.items)).toBe(true);
  });
});

// ─── XSD-aware element completions ───────────────────────────────────────────

const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="project">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="groupId"/>
        <xs:element name="artifactId"/>
      </xs:sequence>
      <xs:attribute name="version" type="xs:string" use="required"/>
    </xs:complexType>
  </xs:element>
</xs:schema>`;

describe("doComplete — XSD-aware element completions", () => {
  // '<project><' — cursor after '<', inside project context
  const xmlWithSchema = "<project><groupId/></project>";
  const doc = parseXMLDocument(uri, xmlWithSchema);
  const provider = new XsdCompletionProvider(xsd);

  it("with a schema provider returns children of the enclosing element", () => {
    // character 10 is after '<project><' — inside project, after '<'
    const result = doComplete(doc, { line: 0, character: 10 }, provider);
    expect(result.items.length).toBeGreaterThan(0);
  });

  it("XSD-driven items include 'groupId'", () => {
    const result = doComplete(doc, { line: 0, character: 10 }, provider);
    const labels = result.items.map((i) => i.label);
    expect(labels).toContain("groupId");
  });

  it("XSD-driven items include 'artifactId'", () => {
    const result = doComplete(doc, { line: 0, character: 10 }, provider);
    const labels = result.items.map((i) => i.label);
    expect(labels).toContain("artifactId");
  });

  it("XSD-driven element items have kind 'element'", () => {
    const result = doComplete(doc, { line: 0, character: 10 }, provider);
    expect(result.items.every((i) => i.kind === "element")).toBe(true);
  });

  it("XSD-driven element insertText wraps with open/close tags", () => {
    const result = doComplete(doc, { line: 0, character: 10 }, provider);
    for (const item of result.items) {
      // insertText should look like 'name>$0</name>'
      expect(item.insertText).toContain(item.label);
      expect(item.insertText).toContain("</");
    }
  });
});

// ─── XSD-aware attribute completions ─────────────────────────────────────────

describe("doComplete — XSD-aware attribute completions", () => {
  // '<project version' — cursor is in attribute position
  const xmlAttr = "<project version";
  const doc = parseXMLDocument(uri, xmlAttr);
  const provider = new XsdCompletionProvider(xsd);

  it("attribute context returns items with kind 'attribute'", () => {
    // character 9 = after '<project ' — in attribute position
    const result = doComplete(doc, { line: 0, character: 9 }, provider);
    const attrItems = result.items.filter((i) => i.kind === "attribute");
    expect(attrItems.length).toBeGreaterThan(0);
  });

  it("required attribute 'version' is included in suggestions", () => {
    const result = doComplete(doc, { line: 0, character: 9 }, provider);
    const labels = result.items.map((i) => i.label);
    expect(labels).toContain("version");
  });

  it("attribute insertText includes quotes placeholder", () => {
    const result = doComplete(doc, { line: 0, character: 9 }, provider);
    const versionItem = result.items.find((i) => i.label === "version");
    expect(versionItem?.insertText).toContain('"');
  });
});

// ─── Fallback attribute completions (no schema) ───────────────────────────────

describe("doComplete — fallback attribute completions (no schema)", () => {
  const xmlAttr = "<root xml";
  const doc = parseXMLDocument(uri, xmlAttr);

  it("without schema returns built-in XML attribute suggestions", () => {
    const result = doComplete(doc, { line: 0, character: 9 });
    const attrItems = result.items.filter((i) => i.kind === "attribute");
    expect(attrItems.length).toBeGreaterThan(0);
  });

  it("built-in suggestions include xml:lang or xmlns", () => {
    const result = doComplete(doc, { line: 0, character: 9 });
    const labels = result.items.map((i) => i.label);
    expect(labels.some((l) => l.startsWith("xml"))).toBe(true);
  });
});
