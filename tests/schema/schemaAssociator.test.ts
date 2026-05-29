import { describe, it, expect } from "vitest";
import path from "path";
import { fileURLToPath } from "url";
import { SchemaAssociator } from "../../src/schema/schemaAssociator.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Absolute path to a real bundled XSD file, used for user-association tests
const mavenXsdPath = path.resolve(
  __dirname,
  "../../resources/default/maven-4.0.0.xsd"
);

describe("SchemaAssociator", () => {
  it("creates without throwing", () => {
    expect(() => new SchemaAssociator()).not.toThrow();
  });

  it("findSchema('pom.xml') returns a non-null ResolvedSchema", () => {
    const assoc = new SchemaAssociator();
    expect(assoc.findSchema("pom.xml")).not.toBeNull();
  });

  it("findSchema('pom.xml').source === 'builtin'", () => {
    const assoc = new SchemaAssociator();
    expect(assoc.findSchema("pom.xml")?.source).toBe("builtin");
  });

  it("findSchema('pom.xml') returns non-empty xsdText", () => {
    const assoc = new SchemaAssociator();
    const result = assoc.findSchema("pom.xml");
    expect(result?.xsdText.length).toBeGreaterThan(0);
  });

  it("findSchema('web.xml') returns a non-null ResolvedSchema", () => {
    const assoc = new SchemaAssociator();
    expect(assoc.findSchema("web.xml")).not.toBeNull();
  });

  it("findSchema('unknown.xml') returns null", () => {
    const assoc = new SchemaAssociator();
    expect(assoc.findSchema("unknown.xml")).toBeNull();
  });

  it("addUserAssociation with pattern 'custom.xml' makes findSchema return source 'custom'", () => {
    const assoc = new SchemaAssociator();
    assoc.addUserAssociation({
      pattern: "custom.xml",
      xsdPath: mavenXsdPath,
      isBuiltIn: false,
    });
    const result = assoc.findSchema("custom.xml");
    expect(result).not.toBeNull();
    expect(result?.source).toBe("custom");
  });

  it("user association overrides built-in for the same filename", () => {
    const assoc = new SchemaAssociator();
    assoc.addUserAssociation({
      pattern: "pom.xml",
      xsdPath: mavenXsdPath,
      isBuiltIn: false,
    });
    const result = assoc.findSchema("pom.xml");
    expect(result?.source).toBe("custom");
  });
});
