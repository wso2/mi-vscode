import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { SchemaProvider } from "../../src/schema/schemaProvider.js";

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
