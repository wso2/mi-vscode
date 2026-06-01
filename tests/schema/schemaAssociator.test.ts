import { describe, it, expect } from "vitest";
import path from "path";
import { fileURLToPath } from "url";
import { SchemaAssociator } from "../../src/schema/schemaAssociator.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const schemasRoot = path.resolve(__dirname, "../../resources/schemas");
const synapse440Xsd = path.join(schemasRoot, "440", "synapse_config.xsd");
const synapse430Xsd = path.join(schemasRoot, "430", "synapse_config.xsd");

describe("SchemaAssociator", () => {
  it("creates without throwing", () => {
    expect(() => new SchemaAssociator()).not.toThrow();
  });

  it("findSchema('unknown.xml') returns null", () => {
    const assoc = new SchemaAssociator();
    expect(assoc.findSchema("unknown.xml")).toBeNull();
  });

  it("findSchema with synapse xmlns returns built-in 440 fallback", () => {
    const assoc = new SchemaAssociator();
    const result = assoc.findSchema(
      "api.xml",
      "http://ws.apache.org/ns/synapse",
      "api.xml"
    );
    expect(result).not.toBeNull();
    expect(result?.source).toBe("builtin");
    expect(result?.xsdPath).toContain("synapse_config.xsd");
    expect(result?.xsdPath).toContain("440");
    expect(result?.xsdText.length).toBeGreaterThan(0);
  });

  it("findSchema without xmlns returns null even for xml file", () => {
    const assoc = new SchemaAssociator();
    expect(assoc.findSchema("something.xml")).toBeNull();
  });

  it("addUserAssociation with glob pattern matches files in that project", () => {
    const assoc = new SchemaAssociator();
    assoc.addUserAssociation({
      pattern: "project-430/**/*.xml",
      xsdPath: synapse430Xsd,
      isBuiltIn: false,
    });
    const result = assoc.findSchema("api.xml", undefined, "project-430/src/api.xml");
    expect(result).not.toBeNull();
    expect(result?.source).toBe("custom");
    expect(result?.xsdPath).toContain("430");
    expect(result?.xsdText.length).toBeGreaterThan(0);
  });

  it("user association overrides built-in for synapse xmlns when pattern matches", () => {
    const assoc = new SchemaAssociator();
    assoc.addUserAssociation({
      pattern: "**/*.xml",
      xsdPath: synapse430Xsd,
      isBuiltIn: false,
    });
    // documentPath enables globMatches so '**/*.xml' matches
    const result = assoc.findSchema(
      "api.xml",
      "http://ws.apache.org/ns/synapse",
      "api.xml"
    );
    expect(result?.source).toBe("custom");
    expect(result?.xsdPath).toContain("430");
  });

  it("addUserAssociation with exact filename pattern returns source 'custom'", () => {
    const assoc = new SchemaAssociator();
    assoc.addUserAssociation({
      pattern: "custom.xml",
      xsdPath: synapse440Xsd,
      isBuiltIn: false,
    });
    const result = assoc.findSchema("custom.xml");
    expect(result).not.toBeNull();
    expect(result?.source).toBe("custom");
  });
});
