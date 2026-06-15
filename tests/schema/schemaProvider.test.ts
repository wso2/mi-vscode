import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { SchemaProvider } from "../../src/schema/schemaProvider.js";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";

const simpleXsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="root">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="child" type="xs:string" minOccurs="0"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>`;

const validXml   = `<?xml version="1.0"?><root><child>hello</child></root>`;
const invalidXml = `<?xml version="1.0"?><root><unknown>hello</unknown></root>`;
const brokenXml  = `<root><unclosed>`;
const schemaUri  = "file:///schema/simple.xsd";

describe("SchemaProvider", () => {
  let provider: SchemaProvider;

  beforeAll(() => {
    provider = new SchemaProvider();
  });

  afterAll(() => {
    provider.dispose();
  });

  it("creates an instance without throwing", () => {
    expect(provider).toBeDefined();
  });

  it("hasSchema() returns false before any schema is registered", () => {
    expect(provider.hasSchema(schemaUri)).toBe(false);
  });

  it("buildAndCacheCompletionProvider() resolves without throwing", async () => {
    await expect(provider.buildAndCacheCompletionProvider({ uri: schemaUri, xsdText: simpleXsd })).resolves.toBeUndefined();
  });

  it("hasSchema() returns true after registering", () => {
    expect(provider.hasSchema(schemaUri)).toBe(true);
  });

  it("validate() returns empty array for a valid document", async () => {
    const doc = parseXMLDocument("file:///test.xml", validXml);
    const result = await provider.validate(schemaUri, doc);
    expect(result).toEqual([]);
  });

  it("validate() returns diagnostics for an invalid document", async () => {
    const doc = parseXMLDocument("file:///test.xml", invalidXml);
    const result = await provider.validate(schemaUri, doc);
    expect(result.length).toBeGreaterThan(0);
  });

  it("validate() returns empty array for an unknown schemaUri with valid XML", async () => {
    const doc = parseXMLDocument("file:///test.xml", validXml);
    const result = await provider.validate("file:///unknown.xsd", doc);
    expect(result).toHaveLength(0);
  });

  it("validate() returns syntax diagnostics for malformed XML", async () => {
    const doc = parseXMLDocument("file:///test.xml", brokenXml);
    const result = await provider.validate(schemaUri, doc);
    const syntaxErrors = result.filter((d) => d.source === "syntax");
    expect(syntaxErrors.length).toBeGreaterThan(0);
  });

  it("registering the same uri twice replaces the old schema without throwing", async () => {
    await expect(
      provider.buildAndCacheCompletionProvider({ uri: schemaUri, xsdText: simpleXsd })
    ).resolves.toBeUndefined();
    expect(provider.hasSchema(schemaUri)).toBe(true);
  });

  it("dispose() cleans up without throwing", () => {
    const tempProvider = new SchemaProvider();
    expect(() => tempProvider.dispose()).not.toThrow();
  });
});
