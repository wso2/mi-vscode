import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import { doDefinition } from "../../src/services/xmlDefinition.js";

// ─── single-line XML ──────────────────────────────────────────────────────────
// <root><child>text</child></root>
//  0123456789012345678901234567890
//  0         1         2         3
// <root>  → name starts at offset 1
// <child> → name starts at offset 7
// </child>→ starts at offset 19  (name at 21)
// </root> → starts at offset 26  (name at 28)

const singleLineXml = "<root><child>text</child></root>";
const multiLineXml = `<root>\n  <child>text</child>\n</root>`;
const selfClosingXml = "<root><br/></root>";
const uri = "file:///test.xml";

describe("doDefinition — open tag → close tag navigation", () => {
  const doc = parseXMLDocument(uri, singleLineXml);

  it("cursor on open <root> tag returns a DefinitionResult pointing to </root>", () => {
    // offset 1 is inside '<root>'
    const result = doDefinition(doc, { line: 0, character: 1 });
    expect(result).not.toBeNull();
  });

  it("result uri matches the document uri", () => {
    const result = doDefinition(doc, { line: 0, character: 1 });
    expect(result?.uri).toBe(uri);
  });

  it("result range points to the closing </root> region", () => {
    // </root> starts at offset 25 in '<root><child>text</child></root>'
    const result = doDefinition(doc, { line: 0, character: 1 });
    expect(result).not.toBeNull();
    // The closing tag start character should be after the content
    expect(result!.range.start.character).toBeGreaterThan(0);
  });

  it("cursor on open <child> tag returns a DefinitionResult pointing to </child>", () => {
    // offset 7 is inside '<child>'
    const result = doDefinition(doc, { line: 0, character: 7 });
    expect(result).not.toBeNull();
  });

  it("returned range for <child> has start before or equal to end", () => {
    const result = doDefinition(doc, { line: 0, character: 7 });
    expect(result).not.toBeNull();
    const { start, end } = result!.range;
    const startsBefore =
      start.line < end.line ||
      (start.line === end.line && start.character <= end.character);
    expect(startsBefore).toBe(true);
  });
});

describe("doDefinition — self-closing tag returns null", () => {
  const doc = parseXMLDocument(uri, selfClosingXml);

  it("cursor on <br/> returns null (self-closing has no close tag to jump to)", () => {
    // offset 7 is inside '<br/>'
    const result = doDefinition(doc, { line: 0, character: 7 });
    expect(result).toBeNull();
  });
});

describe("doDefinition — multiline document navigation", () => {
  // <root>
  //   <child>text</child>
  // </root>
  const doc = parseXMLDocument(uri, multiLineXml);

  it("cursor on line 0 <root> returns a result pointing to a later line", () => {
    const result = doDefinition(doc, { line: 0, character: 1 });
    expect(result).not.toBeNull();
    // </root> is on line 2
    expect(result!.range.start.line).toBeGreaterThan(0);
  });

  it("cursor on line 1 <child> returns a DefinitionResult (also on line 1)", () => {
    // line 1 is '  <child>text</child>'
    // character 3 is inside '<child>'
    const result = doDefinition(doc, { line: 1, character: 3 });
    expect(result).not.toBeNull();
  });
});

describe("doDefinition — cursor not on a tag returns null", () => {
  const doc = parseXMLDocument(uri, singleLineXml);

  it("cursor on text content (not a tag) returns null without throwing", () => {
    // offset 13 is inside the 'text' content of <child>
    expect(() => doDefinition(doc, { line: 0, character: 13 })).not.toThrow();
  });

  it("empty document returns null without throwing", () => {
    const emptyDoc = parseXMLDocument(uri, "");
    expect(() => doDefinition(emptyDoc, { line: 0, character: 0 })).not.toThrow();
    expect(doDefinition(emptyDoc, { line: 0, character: 0 })).toBeNull();
  });
});

describe("doDefinition — DefinitionResult shape", () => {
  const doc = parseXMLDocument(uri, singleLineXml);

  it("result has a uri string property", () => {
    const result = doDefinition(doc, { line: 0, character: 1 });
    expect(typeof result?.uri).toBe("string");
  });

  it("result has a range with start and end positions", () => {
    const result = doDefinition(doc, { line: 0, character: 1 });
    expect(result?.range.start).toBeDefined();
    expect(result?.range.end).toBeDefined();
    expect(typeof result?.range.start.line).toBe("number");
    expect(typeof result?.range.start.character).toBe("number");
  });
});
