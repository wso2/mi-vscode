import { describe, it, expect, beforeAll } from "vitest";
import { XsdValidatorService, Diagnostic } from "../../src/schema/xsdValidator.js";

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

const validXml         = `<?xml version="1.0"?><root><child>hello</child></root>`;
const schemaViolation  = `<?xml version="1.0"?><root><unknown>hello</unknown></root>`;
const brokenXml        = `<root><unclosed>`;
// Schema violation on line 2, fatal syntax error on line 3
const mixedXml = `<?xml version="1.0"?>
<root><unknown>hello</unknown></root
`;

describe("XsdValidatorService.create()", () => {
  it("resolves without throwing for a valid XSD", async () => {
    await expect(XsdValidatorService.create(simpleXsd)).resolves.toBeDefined();
  });
});

describe("XsdValidatorService.validate() — valid XML", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    validator = await XsdValidatorService.create(simpleXsd);
  });

  it("returns an empty array for a valid document", async () => {
    const result = await validator.validate(validXml);
    expect(result).toEqual([]);
  });
});

describe("XsdValidatorService.validate() — schema violations", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    validator = await XsdValidatorService.create(simpleXsd);
  });

  it("returns at least one diagnostic for a schema-invalid document", async () => {
    const result = await validator.validate(schemaViolation);
    expect(result.length).toBeGreaterThan(0);
  });

  it("schema violation diagnostics have source === 'xsd'", async () => {
    const result = await validator.validate(schemaViolation);
    for (const d of result) {
      expect(d.source).toBe("xsd");
    }
  });

  it("schema violation diagnostics have severity 'error' or 'warning'", async () => {
    const result = await validator.validate(schemaViolation);
    for (const d of result) {
      expect(["error", "warning"]).toContain(d.severity);
    }
  });

  it("all diagnostics have a non-empty message string", async () => {
    const result = await validator.validate(schemaViolation);
    for (const d of result) {
      expect(typeof d.message).toBe("string");
      expect(d.message.length).toBeGreaterThan(0);
    }
  });
});

describe("XsdValidatorService.validate() — malformed XML", () => {
  let validator: XsdValidatorService;

  beforeAll(async () => {
    validator = await XsdValidatorService.create(simpleXsd);
  });

  it("returns an array (never throws) for broken XML", async () => {
    await expect(validator.validate(brokenXml)).resolves.toBeInstanceOf(Array);
  });

  it("broken XML produces syntax diagnostics (source === 'syntax')", async () => {
    const result = await validator.validate(brokenXml);
    const syntaxErrors = result.filter((d) => d.source === "syntax");
    expect(syntaxErrors.length).toBeGreaterThan(0);
  });

  /*
   * Xerces runs syntax parsing + XSD validation in one pass.
   * Behaviour on malformed XML:
   *
   *   Scenario                          parseErrors    schemaErrors
   *   ─────────────────────────────────────────────────────────────
   *   Valid XML                         []             []
   *   Schema violations                 []             all violations
   *   Syntax error                      fatal error    []
   *   Schema error before syntax error  fatal error    errors up to crash point
   *
   * For a document with a schema violation BEFORE the fatal syntax error,
   * both source === 'xsd' and source === 'syntax' diagnostics are expected.
   */
  it("schema errors before the fatal syntax error are also reported", async () => {
    const result = await validator.validate(mixedXml);
    const sources = result.map((d) => d.source);
    expect(sources).toContain("syntax");
    expect(sources).toContain("xsd");
  });
});

describe("XsdValidatorService.dispose()", () => {
  it("dispose() does not throw", async () => {
    const validator = await XsdValidatorService.create(simpleXsd);
    expect(() => validator.dispose()).not.toThrow();
  });
});
