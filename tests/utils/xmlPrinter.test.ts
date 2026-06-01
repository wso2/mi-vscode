import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import { printAST, printCST, printTreeAST } from "../../src/utils/xmlPrinter.js";

const uri = "file:///test.xml";

const simpleXML = `<root><item id="1">Hello</item></root>`;
const selfClosingXML = `<root><br/></root>`;
const multiAttrXML = `<root><input type="text" required="true"/>  </root>`;
const deepXML = `<a><b><c><d/></c></b></a>`;
const brokenXML = `<root><unclosed>`;
const emptyXML = ``;

// ── printAST ─────────────────────────────────────────────────────────────────

describe("printAST — tree format (default)", () => {
  it("returns a non-empty string for valid XML", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printAST(doc);
    expect(out).toBeTruthy();
  });

  it("starts with 'Document'", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    expect(printAST(doc)).toMatch(/^Document/);
  });

  it("includes element names", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printAST(doc);
    expect(out).toContain("Element <root>");
    expect(out).toContain("Element <item>");
  });

  it("includes attributes with values", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printAST(doc);
    expect(out).toContain('Attribute id="1"');
  });

  it("marks self-closing elements with /", () => {
    const doc = parseXMLDocument(uri, selfClosingXML);
    const out = printAST(doc);
    expect(out).toContain("Element <br> /");
  });

  it("indents nested elements", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const lines = printAST(doc).split("\n");
    const documentLine = lines.findIndex((l) => l.startsWith("Document"));
    const rootLine = lines.findIndex((l) => l.includes("Element <root>"));
    const itemLine = lines.findIndex((l) => l.includes("Element <item>"));
    expect(documentLine).toBeGreaterThanOrEqual(0);
    // root is indented relative to Document
    expect(lines[rootLine].indexOf("Element")).toBeGreaterThan(lines[documentLine].indexOf("Document"));
    // item is indented relative to root
    expect(lines[itemLine].indexOf("Element")).toBeGreaterThan(lines[rootLine].indexOf("Element"));
  });

  it("handles deeply nested XML without throwing", () => {
    const doc = parseXMLDocument(uri, deepXML);
    expect(() => printAST(doc)).not.toThrow();
    const out = printAST(doc);
    expect(out).toContain("Element <a>");
    expect(out).toContain("Element <b>");
    expect(out).toContain("Element <c>");
    expect(out).toContain("Element <d>");
  });

  it("reports syntax errors in the Document header", () => {
    const doc = parseXMLDocument(uri, brokenXML);
    const out = printAST(doc);
    expect(out).toMatch(/syntax error/i);
  });

  it("does not crash on empty XML", () => {
    const doc = parseXMLDocument(uri, emptyXML);
    expect(() => printAST(doc)).not.toThrow();
    expect(printAST(doc)).toMatch(/^Document/);
  });

  it("handles multiple attributes", () => {
    const doc = parseXMLDocument(uri, multiAttrXML);
    const out = printAST(doc);
    expect(out).toContain('Attribute type="text"');
    expect(out).toContain('Attribute required="true"');
  });
});

describe("printAST — includePositions", () => {
  it("adds offset ranges when includePositions is true", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printAST(doc, { includePositions: true });
    expect(out).toMatch(/\[\d+\.\.\d+\]/);
  });

  it("omits offset ranges by default", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printAST(doc);
    expect(out).not.toMatch(/\[\d+\.\.\d+\]/);
  });
});

describe("printAST — JSON format", () => {
  it("produces valid JSON", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printAST(doc, { format: "json" });
    expect(() => JSON.parse(out)).not.toThrow();
  });

  it("JSON root has type 'root'", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    expect(parsed.type).toBe("root");
  });

  it("JSON root includes uri", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    expect(parsed.uri).toBe(uri);
  });

  it("JSON children contain element names", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    const root = parsed.children?.[0];
    expect(root?.name).toBe("root");
  });

  it("JSON includes attributes array", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    const item = parsed.children?.[0]?.children?.[0];
    expect(Array.isArray(item?.attributes)).toBe(true);
    expect(item.attributes[0].name).toBe("id");
    expect(item.attributes[0].value).toBe("1");
  });

  it("JSON includes offsets when includePositions is true", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printAST(doc, { format: "json", includePositions: true }));
    expect(typeof parsed.startOffset).toBe("number");
    expect(typeof parsed.endOffset).toBe("number");
  });

  it("JSON omits offsets by default", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    expect(parsed.startOffset).toBeUndefined();
  });

  it("JSON marks self-closing elements", () => {
    const doc = parseXMLDocument(uri, selfClosingXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    const br = parsed.children?.[0]?.children?.[0];
    expect(br?.isSelfClosing).toBe(true);
  });

  it("does not crash on broken XML", () => {
    const doc = parseXMLDocument(uri, brokenXML);
    expect(() => printAST(doc, { format: "json" })).not.toThrow();
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    expect(parsed.type).toBe("root");
  });

  it("JSON includes syntaxErrors on broken XML", () => {
    const doc = parseXMLDocument(uri, brokenXML);
    const parsed = JSON.parse(printAST(doc, { format: "json" }));
    expect(Array.isArray(parsed.syntaxErrors)).toBe(true);
    expect(parsed.syntaxErrors.length).toBeGreaterThan(0);
  });
});

describe("printAST — custom indent", () => {
  it("respects custom indent value", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out4 = printAST(doc, { indent: 4 });
    const out2 = printAST(doc, { indent: 2 });
    // With indent 4 the root element line has more leading spaces
    const rootLine4 = out4.split("\n").find((l) => l.includes("Element <root>"))!;
    const rootLine2 = out2.split("\n").find((l) => l.includes("Element <root>"))!;
    expect(rootLine4.length).toBeGreaterThan(rootLine2.length);
  });
});

// ── printCST ─────────────────────────────────────────────────────────────────

describe("printCST — tree format (default)", () => {
  it("returns a non-empty string for valid XML", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc);
    expect(out).toBeTruthy();
  });

  it("starts with a rule node (parenthesised name)", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc);
    expect(out).toMatch(/^\(/);
  });

  it("contains the element tag name as a token", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc);
    expect(out).toContain('"root"');
  });

  it("contains attribute names as tokens", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc);
    expect(out).toContain('"id"');
  });

  it("does not crash on broken XML", () => {
    const doc = parseXMLDocument(uri, brokenXML);
    expect(() => printCST(doc)).not.toThrow();
  });

  it("does not crash on empty XML", () => {
    const doc = parseXMLDocument(uri, emptyXML);
    expect(() => printCST(doc)).not.toThrow();
  });

  it("omits tokens when includeTokens is false", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc, { includeTokens: false });
    // Tokens look like:  TokenName: "image"
    expect(out).not.toMatch(/:\s*"/);
  });

  it("includes positions when includePositions is true", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc, { includePositions: true });
    expect(out).toMatch(/\[\d+\.\.\d+\]/);
  });
});

describe("printCST — JSON format", () => {
  it("produces valid JSON", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printCST(doc, { format: "json" });
    expect(() => JSON.parse(out)).not.toThrow();
  });

  it("JSON root has a 'rule' field", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const parsed = JSON.parse(printCST(doc, { format: "json" }));
    expect(typeof parsed.rule).toBe("string");
  });

  it("does not crash on broken XML", () => {
    const doc = parseXMLDocument(uri, brokenXML);
    expect(() => printCST(doc, { format: "json" })).not.toThrow();
  });
});

// ── printTreeAST ──────────────────────────────────────────────────────────────

describe("printTreeAST", () => {
  it("starts with Document", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    expect(printTreeAST(doc)).toMatch(/^Document/);
  });

  it("contains element names", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    const out = printTreeAST(doc);
    expect(out).toContain("root");
    expect(out).toContain("item");
  });

  it("uses └── for the last child", () => {
    const doc = parseXMLDocument(uri, simpleXML);
    expect(printTreeAST(doc)).toContain("└──");
  });

  it("uses ├── for non-last siblings", () => {
    const doc = parseXMLDocument(uri, `<root><a/><b/><c/></root>`);
    const out = printTreeAST(doc);
    expect(out).toContain("├──");
    expect(out).toContain("└──");
  });

  it("uses │ continuation prefix for nested children", () => {
    const doc = parseXMLDocument(uri, `<root><a><x/></a><b/></root>`);
    expect(printTreeAST(doc)).toContain("│");
  });

  it("does not crash on empty XML", () => {
    const doc = parseXMLDocument(uri, emptyXML);
    expect(() => printTreeAST(doc)).not.toThrow();
    expect(printTreeAST(doc)).toMatch(/^Document/);
  });

  it("does not crash on broken XML", () => {
    const doc = parseXMLDocument(uri, brokenXML);
    expect(() => printTreeAST(doc)).not.toThrow();
  });

  it("handles deeply nested XML", () => {
    const doc = parseXMLDocument(uri, deepXML);
    const out = printTreeAST(doc);
    expect(out).toContain("a");
    expect(out).toContain("b");
    expect(out).toContain("d");
  });

  it("exposes printTreeAST via getLanguageService()", async () => {
    const { getLanguageService } = await import("../../src/xmlLanguageService.js");
    const svc = getLanguageService();
    const doc = svc.parseXMLDocument(uri, simpleXML);
    const out = svc.printTreeAST(doc);
    expect(out).toMatch(/^Document/);
    expect(out).toContain("└──");
  });
});

// ── language service integration ─────────────────────────────────────────────

describe("language service — printAST / printCST", () => {
  it("exposes printAST via getLanguageService()", async () => {
    const { getLanguageService } = await import("../../src/xmlLanguageService.js");
    const svc = getLanguageService();
    const doc = svc.parseXMLDocument(uri, simpleXML);
    const out = svc.printAST(doc);
    expect(out).toContain("Document");
    expect(out).toContain("Element <root>");
  });

  it("exposes printCST via getLanguageService()", async () => {
    const { getLanguageService } = await import("../../src/xmlLanguageService.js");
    const svc = getLanguageService();
    const doc = svc.parseXMLDocument(uri, simpleXML);
    const out = svc.printCST(doc);
    expect(out).toBeTruthy();
    expect(out).not.toBe("(CST not available — document was not created via parseXMLDocument)");
  });
});
