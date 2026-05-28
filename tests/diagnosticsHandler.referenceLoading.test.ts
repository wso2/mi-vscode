import { describe, expect, it } from "vitest";
import * as path from "path";
import { TextDocument } from "vscode-languageserver-textdocument";
import { getLanguageService } from "xml-language-service";
import { DiagnosticsHandler } from "../src/diagnosticsHandler.js";
import {
  createConnection,
  createService,
  makeTempDir,
  writeFile,
} from "./helpers/diagnosticsTestUtils.js";

describe("DiagnosticsHandler schema reference loading", () => {
  it("loads only referenced XSD files recursively", () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const mainText = `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:include schemaLocation="common.xsd"/>
        <xs:import namespace="urn:test" schemaLocation="nested/types.xsd"/>
        <xs:redefine schemaLocation="base.xsd"/>
        <xs:include schemaLocation="ignored.dtd"/>
      </xs:schema>
    `;

    writeFile(mainPath, mainText);
    writeFile(path.join(root, "common.xsd"), `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:include schemaLocation="nested/more.xsd"/>
      </xs:schema>
    `);
    writeFile(path.join(root, "nested", "types.xsd"), `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`);
    writeFile(path.join(root, "nested", "more.xsd"), `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`);
    writeFile(path.join(root, "base.xsd"), `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`);
    writeFile(path.join(root, "unreferenced.xsd"), `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`);
    writeFile(path.join(root, "ignored.dtd"), `<!ELEMENT root ANY>`);

    const { connection } = createConnection();
    const handler = new DiagnosticsHandler(connection as any, {} as any) as any;
    const imports = handler.loadReferencedXsds(mainPath, mainText);

    expect(Object.keys(imports).sort()).toEqual([
      "base.xsd",
      "common.xsd",
      "nested/more.xsd",
      "nested/types.xsd",
    ]);
    expect(imports["unreferenced.xsd"]).toBeUndefined();
    expect(imports["ignored.dtd"]).toBeUndefined();
  });

  it("loads DTD files referenced by loaded XSD and DTD files", () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const mainText = `
      <!DOCTYPE xs:schema SYSTEM "misc/xsd/XMLSchema.dtd">
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>
    `;

    writeFile(mainPath, mainText);
    writeFile(path.join(root, "misc", "xsd", "XMLSchema.dtd"), `
      <!ENTITY % datatypes SYSTEM "datatypes.dtd">
      %datatypes;
    `);
    writeFile(path.join(root, "misc", "xsd", "datatypes.dtd"), `<!ENTITY example "ok">`);
    writeFile(path.join(root, "unreferenced.dtd"), `<!ENTITY unused "no">`);

    const { connection } = createConnection();
    const handler = new DiagnosticsHandler(connection as any, {} as any) as any;
    const imports = handler.loadReferencedXsds(mainPath, mainText);

    expect(Object.keys(imports).sort()).toEqual([
      "misc/xsd/XMLSchema.dtd",
      "misc/xsd/datatypes.dtd",
    ]);
    expect(imports["unreferenced.dtd"]).toBeUndefined();
  });

  it("registers only referenced XSD imports during validation", async () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const mainText = `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:include schemaLocation="common.xsd"/>
      </xs:schema>
    `;

    writeFile(mainPath, mainText);
    writeFile(path.join(root, "common.xsd"), `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`);
    writeFile(path.join(root, "unused.xsd"), `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`);

    const registered: any[] = [];
    const { connection, sentDiagnostics } = createConnection();
    const service = createService(mainPath, mainText, registered);
    const handler = new DiagnosticsHandler(connection as any, service as any);
    const document = TextDocument.create("file:///project/test.xml", "xml", 1, "<root/>");

    await handler.validateAndSend(document);

    expect(registered).toHaveLength(1);
    expect(registered[0].imports).toEqual({
      "common.xsd": `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>`,
    });
    expect(sentDiagnostics).toEqual([{ uri: "file:///project/test.xml", diagnostics: [] }]);
  });

  it("skips missing and non-XSD schema references without throwing", async () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const mainText = `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:include schemaLocation="missing.xsd"/>
        <xs:include schemaLocation="ignored.dtd"/>
      </xs:schema>
    `;

    writeFile(mainPath, mainText);
    writeFile(path.join(root, "ignored.dtd"), `<!ELEMENT root ANY>`);

    const registered: any[] = [];
    const { connection, warnings } = createConnection();
    const service = createService(mainPath, mainText, registered);
    const handler = new DiagnosticsHandler(connection as any, service as any);
    const document = TextDocument.create("file:///project/test.xml", "xml", 1, "<root/>");

    await expect(handler.validateAndSend(document)).resolves.toBeUndefined();

    expect(registered).toHaveLength(1);
    expect(registered[0].imports).toBeUndefined();
    expect(warnings.some((message) => message.includes("missing.xsd"))).toBe(true);
    expect(warnings.some((message) => message.includes("ignored.dtd"))).toBe(true);
  });

  it("skips oversized schema references without throwing", async () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const bigPath = path.join(root, "big.xsd");
    const mainText = `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:include schemaLocation="big.xsd"/>
      </xs:schema>
    `;

    writeFile(mainPath, mainText);
    writeFile(bigPath, " ".repeat(2 * 1024 * 1024 + 1));

    const registered: any[] = [];
    const { connection, warnings } = createConnection();
    const service = createService(mainPath, mainText, registered);
    const handler = new DiagnosticsHandler(connection as any, service as any);
    const document = TextDocument.create("file:///project/test.xml", "xml", 1, "<root/>");

    await expect(handler.validateAndSend(document)).resolves.toBeUndefined();

    expect(registered).toHaveLength(1);
    expect(registered[0].imports).toBeUndefined();
    expect(warnings.some((message) => message.includes("above the"))).toBe(true);
  });

  it("validates with the real language service when the main XSD includes another XSD", async () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const commonPath = path.join(root, "common.xsd");
    const documentPath = path.join(root, "test.xml");

    writeFile(mainPath, `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:include schemaLocation="common.xsd"/>
      </xs:schema>
    `);
    writeFile(commonPath, `
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:element name="root" type="xs:string"/>
      </xs:schema>
    `);

    const { connection, sentDiagnostics } = createConnection();
    const service = getLanguageService();
    service.addUserAssociation({
      pattern: "test.xml",
      xsdPath: mainPath,
      isBuiltIn: false,
    });
    const handler = new DiagnosticsHandler(connection as any, service);
    const document = TextDocument.create(`file://${documentPath}`, "xml", 1, "<root>ok</root>");

    await expect(handler.validateAndSend(document)).resolves.toBeUndefined();

    expect(sentDiagnostics).toHaveLength(1);
    expect((sentDiagnostics[0] as any).diagnostics).toEqual([]);
  });

  it("validates with the real language service when an XSD references a DTD", async () => {
    const root = makeTempDir();
    const mainPath = path.join(root, "main.xsd");
    const dtdPath = path.join(root, "misc", "xsd", "XMLSchema.dtd");
    const documentPath = path.join(root, "test.xml");

    writeFile(mainPath, `
      <!DOCTYPE xs:schema SYSTEM "misc/xsd/XMLSchema.dtd">
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:element name="root" type="xs:string"/>
      </xs:schema>
    `);
    writeFile(dtdPath, "");

    const { connection, sentDiagnostics } = createConnection();
    const service = getLanguageService();
    service.addUserAssociation({
      pattern: "test.xml",
      xsdPath: mainPath,
      isBuiltIn: false,
    });
    const handler = new DiagnosticsHandler(connection as any, service);
    const document = TextDocument.create(`file://${documentPath}`, "xml", 1, "<root>ok</root>");

    await expect(handler.validateAndSend(document)).resolves.toBeUndefined();

    expect(sentDiagnostics).toHaveLength(1);
    expect((sentDiagnostics[0] as any).diagnostics).toEqual([]);
  });
});
