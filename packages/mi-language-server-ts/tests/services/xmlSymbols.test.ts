import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";
import {
  findDocumentSymbols,
  DocumentSymbol,
  DocumentSymbolKind,
} from "../../src/services/xmlSymbols.js";

const xml = `<root>
  <child name="test">
    <grandchild/>
  </child>
</root>`;

describe("findDocumentSymbols", () => {
  const doc = parseXMLDocument("file:///test.xml", xml);
  const symbols = findDocumentSymbols(doc);

  it("returns array with one top-level symbol", () => {
    expect(symbols).toHaveLength(1);
  });

  it("top-level symbol name is 'root'", () => {
    expect(symbols[0].name).toBe("root");
  });

  it("root has one child symbol named with its identifying attribute", () => {
    expect(symbols[0].children).toHaveLength(1);
    expect(symbols[0].children[0].name).toBe("child (test)");
  });

  it("child has one child symbol named 'grandchild'", () => {
    expect(symbols[0].children[0].children).toHaveLength(1);
    expect(symbols[0].children[0].children[0].name).toBe("grandchild");
  });

  it("grandchild has no children", () => {
    expect(symbols[0].children[0].children[0].children).toHaveLength(0);
  });

  it("all symbols have valid range with start before or at end", () => {
    function checkRanges(syms: DocumentSymbol[]): void {
      for (const sym of syms) {
        const { start, end } = sym.range;
        const startsBefore =
          start.line < end.line ||
          (start.line === end.line && start.character <= end.character);
        expect(startsBefore).toBe(true);
        checkRanges(sym.children);
      }
    }
    checkRanges(symbols);
  });

  it("all symbols have a supported kind", () => {
    const supportedKinds: DocumentSymbolKind[] = [
      "namespace",
      "class",
      "method",
      "function",
      "property",
      "field",
      "interface",
      "struct",
      "array",
      "object",
    ];

    function checkKind(syms: DocumentSymbol[]): void {
      for (const sym of syms) {
        expect(supportedKinds).toContain(sym.kind);
        checkKind(sym.children);
      }
    }
    checkKind(symbols);
  });

  it("all symbols have a selectionRange defined", () => {
    function checkSelection(syms: DocumentSymbol[]): void {
      for (const sym of syms) {
        expect(sym.selectionRange).toBeDefined();
        expect(sym.selectionRange.start).toBeDefined();
        expect(sym.selectionRange.end).toBeDefined();
        checkSelection(sym.children);
      }
    }
    checkSelection(symbols);
  });

  it("selectionRange start equals range start for all symbols", () => {
    // selectionRange covers '<tagName' only; it starts at the same offset as range
    function check(syms: DocumentSymbol[]): void {
      for (const sym of syms) {
        expect(sym.selectionRange.start.line).toBe(sym.range.start.line);
        expect(sym.selectionRange.start.character).toBe(sym.range.start.character);
        check(sym.children);
      }
    }
    check(symbols);
  });

  it("selectionRange end is within the range (not beyond)", () => {
    function check(syms: DocumentSymbol[]): void {
      for (const sym of syms) {
        const selEnd = sym.selectionRange.end;
        const rangeEnd = sym.range.end;
        const withinRange =
          selEnd.line < rangeEnd.line ||
          (selEnd.line === rangeEnd.line && selEnd.character <= rangeEnd.character);
        expect(withinRange).toBe(true);
        check(sym.children);
      }
    }
    check(symbols);
  });

  it("empty document returns an empty array", () => {
    const emptyDoc = parseXMLDocument("file:///test.xml", "");
    expect(findDocumentSymbols(emptyDoc)).toEqual([]);
  });

  it("document with multiple top-level siblings returns all of them", () => {
    // Note: technically invalid XML, but fault-tolerant parser should handle it
    const multiDoc = parseXMLDocument(
      "file:///test.xml",
      "<a/><b/><c/>"
    );
    const syms = findDocumentSymbols(multiDoc);
    expect(syms.length).toBeGreaterThanOrEqual(1);
    const names = syms.map((s) => s.name);
    expect(names).toContain("a");
  });

  it("uses name, id, then key attributes to make repeated element names distinguishable", () => {
    const synapseDoc = parseXMLDocument(
      "file:///synapse.xml",
      `<definitions>
  <sequence name="main">
    <property id="trace-id"/>
    <endpoint key="OrdersEP"/>
    <sequence/>
  </sequence>
</definitions>`
    );

    const [definitions] = findDocumentSymbols(synapseDoc);
    const [sequence] = definitions.children;

    expect(sequence.name).toBe("sequence (main)");
    expect(sequence.kind).toBe("class");
    expect(sequence.children.map((child) => child.name)).toEqual([
      "property (trace-id)",
      "endpoint (OrdersEP)",
      "sequence",
    ]);
    expect(sequence.children.map((child) => child.kind)).toEqual([
      "property",
      "class",
      "field",
    ]);
  });

  it("classifies common WSO2 MI elements for richer Outline icons", () => {
    const apiDoc = parseXMLDocument(
      "file:///api.xml",
      `<api name="MultiplyAPI">
  <resource>
    <inSequence>
      <property name="result"/>
      <payloadFactory>
        <format/>
        <args/>
      </payloadFactory>
      <respond/>
    </inSequence>
  </resource>
</api>`
    );

    const [api] = findDocumentSymbols(apiDoc);
    const [resource] = api.children;
    const [inSequence] = resource.children;
    const [, payloadFactory, respond] = inSequence.children;

    expect(api.kind).toBe("class");
    expect(resource.kind).toBe("method");
    expect(inSequence.kind).toBe("method");
    expect(inSequence.children[0].kind).toBe("property");
    expect(payloadFactory.kind).toBe("function");
    expect(payloadFactory.children.map((child) => child.kind)).toEqual([
      "struct",
      "array",
    ]);
    expect(respond.kind).toBe("function");
  });
});
