import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import { getFoldingRanges } from "../../src/services/xmlFolding.js";

const xml = `<root>
  <child>
    <grandchild/>
  </child>
</root>`;

describe("getFoldingRanges", () => {
  const doc = parseXMLDocument("file:///test.xml", xml);
  const ranges = getFoldingRanges(doc);

  it("returns an array", () => {
    expect(Array.isArray(ranges)).toBe(true);
  });

  it("root element produces a folding range spanning multiple lines", () => {
    const rootRange = ranges.find((r) => r.startLine === 0);
    expect(rootRange).toBeDefined();
  });

  it("child element produces a folding range", () => {
    const childRange = ranges.find((r) => r.startLine === 1);
    expect(childRange).toBeDefined();
  });

  it("grandchild does NOT produce a folding range (self-closing, single line)", () => {
    // only root (line 0) and child (line 1) should appear
    expect(ranges).toHaveLength(2);
  });

  it("all ranges have startLine < endLine", () => {
    for (const range of ranges) {
      expect(range.startLine).toBeLessThan(range.endLine);
    }
  });

  it("all ranges have kind 'region'", () => {
    for (const range of ranges) {
      expect(range.kind).toBe("region");
    }
  });
});
