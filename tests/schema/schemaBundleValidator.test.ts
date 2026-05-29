import { describe, it, expect, beforeAll } from "vitest";
import { readFile, readdir } from "fs/promises";
import { fileURLToPath } from "url";
import { join, dirname } from "path";
import { XsdValidatorService, SchemaBundle } from "../../src/schema/xsdValidator.js";
import { SchemaProvider } from "../../src/schema/schemaProvider.js";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const fixtures  = join(__dirname, "fixtures");

async function xsd(name: string): Promise<string> {
  return readFile(join(fixtures, name), "utf8");
}

async function loadSiblingSchemaFiles(
  root: string,
  entryName: string,
  relBase = "",
): Promise<Record<string, string>> {
  const imports: Record<string, string> = {};
  const entries = await readdir(join(root, relBase), { withFileTypes: true });
  for (const entry of entries) {
    const rel = relBase ? `${relBase}/${entry.name}` : entry.name;
    if (entry.isDirectory()) {
      Object.assign(imports, await loadSiblingSchemaFiles(root, entryName, rel));
    } else if (
      (entry.name.toLowerCase().endsWith(".xsd") || entry.name.toLowerCase().endsWith(".dtd")) &&
      rel !== entryName
    ) {
      imports[rel] = await readFile(join(root, rel), "utf8");
    }
  }
  return imports;
}

// ─────────────────────────────────────────────────────────────────────────────
// Fixtures
//
// root.xsd  <xs:include schemaLocation="types.xsd"/>
//           <xs:element name="people">
//             <xs:element name="person" type="PersonType" .../>
//
// types.xsd <xs:complexType name="PersonType">
//             name (xs:string), age (xs:integer)
//
// company.xsd  <xs:import namespace="http://example.com/address"
//                         schemaLocation="address.xsd"/>
//              <xs:element name="company">
//                name (xs:string), headquarter (addr:AddressType)
//
// address.xsd  targetNamespace="http://example.com/address"
//              <xs:complexType name="AddressType">
//                street (xs:string), city (xs:string)
// ─────────────────────────────────────────────────────────────────────────────

const validPeople = `<?xml version="1.0"?>
<people>
  <person><name>Alice</name><age>30</age></person>
  <person><name>Bob</name><age>25</age></person>
</people>`;

// <unknown> is not defined in PersonType — should produce XSD error
const invalidPeople = `<?xml version="1.0"?>
<people>
  <person><name>Alice</name><unknown>bad</unknown></person>
</people>`;

// Valid syntax, valid against root element — but <age> is missing (required)
const missingFieldPeople = `<?xml version="1.0"?>
<people>
  <person><name>Alice</name></person>
</people>`;

const brokenPeople = `<?xml version="1.0"?>
<people>
  <person><name>Alice</name><age>30</age></person>
  <unclosed>`;

const validCompany = `<?xml version="1.0"?>
<company xmlns:addr="http://example.com/address">
  <name>Acme</name>
  <headquarter><street>123 Main St</street><city>Springfield</city></headquarter>
</company>`;

const invalidCompany = `<?xml version="1.0"?>
<company>
  <name>Acme</name>
  <headquarter><unknown>bad</unknown></headquarter>
</company>`;

// ── xs:include ────────────────────────────────────────────────────────────────

describe("xs:include — schema WITHOUT imports fails to resolve PersonType", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    // No imports map — types.xsd is not provided
    validator = await XsdValidatorService.create(await xsd("root.xsd"));
  });

  it("even valid XML produces errors because PersonType is unresolved", async () => {
    // Without types.xsd, Xerces cannot resolve PersonType → schema/parse errors
    const result = await validator.validate(validPeople);
    expect(result.length).toBeGreaterThan(0);
  });
});

describe("xs:include — schema WITH imports resolves PersonType correctly", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    const bundle: SchemaBundle = {
      entry:   await xsd("root.xsd"),
      imports: { "types.xsd": await xsd("types.xsd") },
    };
    validator = await XsdValidatorService.create(bundle);
  });

  it("valid XML returns no diagnostics", async () => {
    expect(await validator.validate(validPeople)).toEqual([]);
  });

  it("unknown element inside PersonType produces an XSD error", async () => {
    const result = await validator.validate(invalidPeople);
    const xsdErrors = result.filter((d) => d.source === "xsd");
    expect(xsdErrors.length).toBeGreaterThan(0);
  });

  it("XSD error message references the unexpected element", async () => {
    const result = await validator.validate(invalidPeople);
    const messages = result.map((d) => d.message).join(" ");
    expect(messages.toLowerCase()).toMatch(/unknown|invalid|unexpected|not expected/);
  });

  it("missing required field (age) produces an XSD error", async () => {
    const result = await validator.validate(missingFieldPeople);
    const xsdErrors = result.filter((d) => d.source === "xsd");
    expect(xsdErrors.length).toBeGreaterThan(0);
  });

  it("malformed XML produces syntax diagnostics", async () => {
    const result = await validator.validate(brokenPeople);
    const syntaxErrors = result.filter((d) => d.source === "syntax");
    expect(syntaxErrors.length).toBeGreaterThan(0);
  });

  it("all diagnostics have a range with valid line numbers", async () => {
    const result = await validator.validate(invalidPeople);
    for (const d of result) {
      expect(d.range.start.line).toBeGreaterThanOrEqual(0);
      expect(d.range.start.character).toBeGreaterThanOrEqual(0);
    }
  });

  it("never throws — always resolves to an array", async () => {
    await expect(validator.validate(brokenPeople)).resolves.toBeInstanceOf(Array);
  });
});

// ── xs:import ─────────────────────────────────────────────────────────────────

describe("xs:import — schema WITHOUT imports fails to resolve AddressType", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    validator = await XsdValidatorService.create(await xsd("company.xsd"));
  });

  it("even valid XML produces errors because AddressType is unresolved", async () => {
    const result = await validator.validate(validCompany);
    expect(result.length).toBeGreaterThan(0);
  });
});

describe("xs:import — schema WITH imports resolves AddressType correctly", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    const bundle: SchemaBundle = {
      entry:   await xsd("company.xsd"),
      imports: { "address.xsd": await xsd("address.xsd") },
    };
    validator = await XsdValidatorService.create(bundle);
  });

  it("valid XML returns no diagnostics", async () => {
    expect(await validator.validate(validCompany)).toEqual([]);
  });

  it("invalid XML (wrong element in AddressType) produces XSD errors", async () => {
    const result = await validator.validate(invalidCompany);
    const xsdErrors = result.filter((d) => d.source === "xsd");
    expect(xsdErrors.length).toBeGreaterThan(0);
  });

  it("XSD error message references the unexpected element", async () => {
    const result = await validator.validate(invalidCompany);
    const messages = result.map((d) => d.message).join(" ");
    expect(messages.toLowerCase()).toMatch(/unknown|invalid|unexpected|not expected/);
  });
});

// ── End-to-end through SchemaProvider ────────────────────────────────────────

describe("xs:include — end-to-end through SchemaProvider", () => {
  let provider: SchemaProvider;
  const uri = "file:///fixtures/root.xsd";

  beforeAll(async () => {
    provider = new SchemaProvider();
    await provider.registerSchema({
      uri,
      xsdText: await xsd("root.xsd"),
      imports: { "types.xsd": await xsd("types.xsd") },
    });
  });

  it("hasSchema() returns true after registering bundle", () => {
    expect(provider.hasSchema(uri)).toBe(true);
  });

  it("valid XML returns []", async () => {
    const doc = parseXMLDocument("file:///test.xml", validPeople);
    expect(await provider.validate(uri, doc)).toEqual([]);
  });

  it("schema violation returns XSD errors", async () => {
    const doc = parseXMLDocument("file:///test.xml", invalidPeople);
    const result = await provider.validate(uri, doc);
    expect(result.filter((d) => d.source === "xsd").length).toBeGreaterThan(0);
  });

  it("malformed XML returns syntax errors", async () => {
    const doc = parseXMLDocument("file:///test.xml", brokenPeople);
    const result = await provider.validate(uri, doc);
    expect(result.filter((d) => d.source === "syntax").length).toBeGreaterThan(0);
  });

  it("unknown schemaUri returns empty array for valid XML", async () => {
    const doc = parseXMLDocument("file:///test.xml", validPeople);
    const result = await provider.validate("file:///unknown.xsd", doc);
    expect(result).toHaveLength(0);
  });
});

describe("WSO2 schemas with W3C XMLSchema.dtd", () => {
  it("does not fail validation on the XMLSchema.dtd xmlns warning", async () => {
    const schemaRoot = join(__dirname, "../wso2/schemas/440");
    const entry = await readFile(join(schemaRoot, "api.xsd"), "utf8");
    const imports = await loadSiblingSchemaFiles(schemaRoot, "api.xsd");
    const validator = await XsdValidatorService.create({ entry, imports });
    const xml = `<api xmlns="http://ws.apache.org/ns/synapse" name="a" context="/a"><resource methods="GET" uri-template="/x"/></api>`;

    const result = await validator.validate(xml);

    expect(result.map((d) => d.message)).not.toContain(
      "attribute 'xmlns' has already been declared for element 'schema'",
    );
  });
});
