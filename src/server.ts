import {
  createConnection,
  TextDocuments,
  ProposedFeatures,
  InitializeParams,
  InitializeResult,
  TextDocumentSyncKind,
  DidChangeConfigurationParams,
} from "vscode-languageserver/node.js";
import { TextDocument } from "vscode-languageserver-textdocument";
import * as path from "path";
import * as fs from "fs";
import { fileURLToPath } from "url";
import { getLanguageService } from "./xmlLanguageService.js";
import { DiagnosticsHandler } from "./diagnosticsHandler.js";
import { SchemaConfig, applySchemaSettings } from "./configuration.js";
import { registerRequestHandlers } from "./requestHandlers.js";
import { formatError } from "./lspUtils.js";

const connection = createConnection(ProposedFeatures.all);
const documents = new TextDocuments(TextDocument);
const service = getLanguageService();
const diagnosticsHandler = new DiagnosticsHandler(connection, service);

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SCHEMAS_ROOT = path.join(__dirname, "..", "resources", "schemas");

const SCHEMA_FOLDER_MAP: Record<string, string> = {
  "430": path.join(SCHEMAS_ROOT, "430"),
  "440": path.join(SCHEMAS_ROOT, "440"),
};

let workspaceRoots: string[] = [];
let initialConfigurationLoaded = false;

// ── Validation helpers ───────────────────────────────────────────────────────

async function validateAndSendSafely(document: TextDocument, reason: string): Promise<void> {
  try {
    await diagnosticsHandler.validateAndSend(document);
  } catch (error) {
    connection.console.error(
      `[diagnostics] Validation failed during ${reason} for ${document.uri}: ${formatError(error)}`
    );
    diagnosticsHandler.clearDiagnostics(document.uri);
  }
}

async function validateOpenDocumentsSafely(reason: string): Promise<void> {
  await Promise.all(documents.all().map((doc) => validateAndSendSafely(doc, reason)));
}

// ── Schema registration ──────────────────────────────────────────────────────

function registerSchemas(schemas: any[]): void {
  for (const schema of schemas) {
    const pattern: string = schema.pattern;
    const schemaFolder: string = schema.schema;

    const folderPath = SCHEMA_FOLDER_MAP[schemaFolder];
    if (!folderPath) {
      connection.console.warn(`[server] Unknown schema folder: ${schemaFolder}`);
      continue;
    }

    let xsdFile: string | undefined;
    try {
      // prefer synapse_config.xsd as the main entry point, fall back to first .xsd
      const files = fs.readdirSync(folderPath);
      const main = files.find((f) => f === "synapse_config.xsd");
      const first = files.find((f) => f.endsWith(".xsd"));
      xsdFile = main ?? first;
    } catch (e) {
      connection.console.warn(`[server] Cannot read schema folder ${folderPath}: ${e}`);
      continue;
    }

    if (!xsdFile) {
      connection.console.warn(`[server] No XSD found in: ${folderPath}`);
      continue;
    }

    const xsdPath = path.join(folderPath, xsdFile);
    service.addUserAssociation({ pattern, xsdPath, isBuiltIn: false, namespace: "" });
    connection.console.log(`[server] Registered: ${pattern} → ${schemaFolder}`);
  }
}

// ── LSP lifecycle ────────────────────────────────────────────────────────────

connection.onInitialize((params: InitializeParams): InitializeResult => {
  workspaceRoots = (params.workspaceFolders ?? []).map((f) =>
    decodeURIComponent(f.uri.replace("file://", ""))
  );
  connection.console.log("=== STARTUP WAS TRIGGERED!===");
  connection.console.log("==================");
  connection.console.log(`intalize params:\n${JSON.stringify(params, null, 2)}`);
  connection.console.log(`initalize options:\n${JSON.stringify(params.initializationOptions, null, 2)}`);
  connection.console.log("======");

  const options = params.initializationOptions ?? {};
  const initialSchemas = options.schemas ?? [];

  connection.console.log(`[server] Received ${initialSchemas.length} initial schema(s)`);

  if (initialSchemas.length > 0) {
    registerSchemas(initialSchemas);
  }

  return {
    capabilities: {
      textDocumentSync: TextDocumentSyncKind.Incremental,
      completionProvider: { resolveProvider: false, triggerCharacters: ["<", " ", '"', "/"] },
      hoverProvider: true,
      documentSymbolProvider: true,
      foldingRangeProvider: true,
      renameProvider: true,
      definitionProvider: true,
      referencesProvider: true,
      documentFormattingProvider: true,
      workspace: {
        workspaceFolders: { supported: true },
        fileOperations: {},
      },
    },
  };
});

connection.onInitialized(async () => {
  try {
    const config = await connection.workspace.getConfiguration("xmlLanguageServer");
    connection.console.log(`[config] Fetched initial config: ${JSON.stringify(config)}`);
    applySchemaSettings(config?.schemas ?? [], connection, service, workspaceRoots);
  } catch (e) {
    connection.console.log(`[config] Could not fetch initial config: ${e}`);
  } finally {
    initialConfigurationLoaded = true;
    await validateOpenDocumentsSafely("initial configuration");
  }
});

connection.onDidChangeConfiguration((params: DidChangeConfigurationParams) => {
  connection.console.log("[server] Configuration changed");

  const settings = params.settings?.xmlLanguageServer ?? params.settings ?? {};
  const schemas = settings.schemas ?? [];

  connection.console.log(`[server] Updating with ${schemas.length} schema(s)`);

  if (schemas.length > 0) {
    registerSchemas(schemas);
  }

  if (!initialConfigurationLoaded) {
    connection.console.log("[config] Deferring configuration-change validation until initial configuration is loaded");
    return;
  }
  void validateOpenDocumentsSafely("configuration change");
});

documents.onDidChangeContent(async (change) => {
  if (!initialConfigurationLoaded) {
    connection.console.log(
      `[onDidChangeContent] Deferring validation for ${change.document.uri} until initial configuration is loaded`
    );
    return;
  }
  connection.console.log(`[onDidChangeContent] Validating ${change.document.uri}`);
  await validateAndSendSafely(change.document, "document change");
});

connection.onShutdown(() => {
  diagnosticsHandler.dispose();
  service.dispose();
});

// ── Bootstrap ────────────────────────────────────────────────────────────────

registerRequestHandlers(connection, documents, service, diagnosticsHandler);
documents.listen(connection);
connection.listen();
