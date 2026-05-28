import { afterEach, expect } from "vitest";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import { TextDocument } from "vscode-languageserver-textdocument";
import { getLanguageService } from "xml-language-service";
import { DiagnosticsHandler } from "../../src/diagnosticsHandler.js";

const tempDirs: string[] = [];

export function makeTempDir(): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "xml-ls-schema-"));
  tempDirs.push(dir);
  return dir;
}

export function writeFile(filePath: string, text: string): void {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, text);
}

export function createConnection() {
  const warnings: string[] = [];
  const sentDiagnostics: unknown[] = [];
  return {
    warnings,
    sentDiagnostics,
    connection: {
      console: {
        log: () => undefined,
        warn: (message: string) => warnings.push(message),
        error: () => undefined,
      },
      sendDiagnostics: (params: unknown) => sentDiagnostics.push(params),
    },
  };
}

export function createService(schemaPath: string, schemaText: string, registered: unknown[]) {
  return {
    parseXMLDocument: (uri: string, text: string) => ({
      uri,
      text,
      getNamespace: () => undefined,
    }),
    resolveSchemaForDocument: () => ({
      xsdPath: schemaPath,
      xsdText: schemaText,
    }),
    hasSchema: () => false,
    registerSchema: async (info: unknown) => {
      registered.push(info);
    },
    validate: async () => [],
  };
}

export function readFixture(relativePath: string): string {
  return fs.readFileSync(path.join(process.cwd(), relativePath), "utf-8");
}

export async function validateSynapseFixtureXml(xml: string) {
  const schemaPath = path.join(process.cwd(), "tests/resources/schemas/430/synapse_config.xsd");
  const documentPath = path.join(process.cwd(), "tests/resources/synapse.xml");
  const { connection, sentDiagnostics, warnings } = createConnection();
  const service = getLanguageService();
  service.addUserAssociation({
    pattern: "synapse.xml",
    xsdPath: schemaPath,
    isBuiltIn: false,
  });
  const handler = new DiagnosticsHandler(connection as any, service);
  const document = TextDocument.create(`file://${documentPath}`, "xml", 1, xml);

  await handler.validateAndSend(document);

  const diagnostics = (sentDiagnostics.at(-1) as any)?.diagnostics ?? [];
  return { diagnostics, warnings };
}

export function expectSchemaResolved(diagnostics: any[], warnings: string[]): void {
  const messages = [
    ...diagnostics.map((diagnostic) => String(diagnostic.message)),
    ...warnings,
  ];
  expect(messages).not.toEqual(expect.arrayContaining([
    expect.stringContaining("No matching global declaration"),
    expect.stringContaining("Cannot find the declaration of element"),
    expect.stringContaining("unable to open"),
    expect.stringContaining("No schema registered"),
  ]));
}

afterEach(() => {
  for (const dir of tempDirs.splice(0)) {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
