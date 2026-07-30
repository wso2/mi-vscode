import {
  createConnection,
  TextDocuments,
  ProposedFeatures,
  InitializeParams,
  InitializeResult,
  TextDocumentSyncKind,
} from "vscode-languageserver/node.js";
import { TextDocument } from "vscode-languageserver-textdocument";
import * as path from "path";
import * as fs from "fs";
import { fileURLToPath } from "url";
import { getLanguageService } from "./xmlLanguageService.js";
import { DiagnosticsHandler } from "./diagnosticsHandler.js";
import { registerRequestHandlers } from "./requestHandlers.js";
import { formatError } from "./lspUtils.js";

const connection = createConnection(ProposedFeatures.all);
const documents = new TextDocuments(TextDocument);
const service = getLanguageService();
const diagnosticsHandler = new DiagnosticsHandler(connection, service);

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SCHEMAS_ROOT = path.join(__dirname, "resources", "schemas");

const SCHEMA_FOLDER_MAP: Record<string, string> = {
  "430": path.join(SCHEMAS_ROOT, "430"),
  "440": path.join(SCHEMAS_ROOT, "440"),
};

let initialConfigurationLoaded = false;
let initializationSchemas: any[] = [];

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
  connection.console.log("=== STARTUP WAS TRIGGERED!===");
  connection.console.log("==================");
  connection.console.log(`intalize params:\n${JSON.stringify(params, null, 2)}`);
  connection.console.log(`initalize options:\n${JSON.stringify(params.initializationOptions, null, 2)}`);
  connection.console.log("==================");

  const options = params.initializationOptions ?? {};
  initializationSchemas = options.schemas ?? [];

  connection.console.log(`[server] Received ${initializationSchemas.length} initial schema(s)`);

  if (initializationSchemas.length > 0) {
    registerSchemas(initializationSchemas);
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
      documentRangeFormattingProvider: true,
      workspace: {
        workspaceFolders: { supported: true },
        fileOperations: {},
      },
    },
  };
});

connection.onInitialized(async () => {
  initialConfigurationLoaded = true;
  await validateOpenDocumentsSafely("initial configuration");
});

connection.onDidChangeConfiguration(() => {
  connection.console.log("[server] Configuration changed — rebuilding schema associations");

  service.invalidateAutoSchemas();
  void diagnosticsHandler.dispose();
  service.clearUserAssociations();

  if (initializationSchemas.length > 0) {
    registerSchemas(initializationSchemas);
  }

  if (!initialConfigurationLoaded) {
    connection.console.log("[config] Deferring validation until initial configuration is loaded");
    return;
  }
  void validateOpenDocumentsSafely("configuration change");
});

// Debounce validation per document so rapid keystrokes trigger a single
// validation after the user pauses, instead of one full Xerces pass per edit.
const VALIDATION_DEBOUNCE_MS = 300;
const pendingValidations = new Map<string, NodeJS.Timeout>();

documents.onDidChangeContent((change) => {
  if (!initialConfigurationLoaded) {
    connection.console.log(
      `[onDidChangeContent] Deferring validation for ${change.document.uri} until initial configuration is loaded`
    );
    return;
  }
  const uri = change.document.uri;
  clearTimeout(pendingValidations.get(uri));
  pendingValidations.set(uri, setTimeout(() => {
    pendingValidations.delete(uri);
    connection.console.log(`[onDidChangeContent] Validating ${uri}`);
    void validateAndSendSafely(change.document, "document change");
  }, VALIDATION_DEBOUNCE_MS));
});

documents.onDidClose((event) => {
  const timer = pendingValidations.get(event.document.uri);
  if (timer) {
    clearTimeout(timer);
    pendingValidations.delete(event.document.uri);
  }
});

connection.onShutdown(() => {
  void diagnosticsHandler.dispose();
  service.dispose();
});

// ── Bootstrap ────────────────────────────────────────────────────────────────

registerRequestHandlers(connection, documents, service, diagnosticsHandler);
documents.listen(connection);
connection.listen();
