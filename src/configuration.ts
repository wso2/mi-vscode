import { Connection } from "vscode-languageserver/node.js";
import { getLanguageService } from "xml-language-service";
import * as fs from "fs";
import * as path from "path";

type LanguageService = ReturnType<typeof getLanguageService>;

export interface SchemaConfig {
  pattern: string;
  xsdPath: string;
}

export function applySchemaSettings(
  schemas: SchemaConfig[],
  connection: Connection,
  service: LanguageService,
  workspaceRoots: string[],
  invalidate = false
): void {
  if (invalidate) service.invalidateAutoSchemas();
  for (const entry of schemas) {
    const resolved = resolveXsdPath(entry.xsdPath, workspaceRoots);

    if (!resolved) {
      connection.console.warn(
        `[config] Cannot resolve xsdPath '${entry.xsdPath}' — file not found in any workspace root`
      );
      continue;
    }

    service.addUserAssociation({ pattern: entry.pattern, xsdPath: resolved, isBuiltIn: false });
    connection.console.log(`[config] Registered schema: ${entry.pattern} → ${resolved}`);
  }
}

function resolveXsdPath(xsdPath: string, workspaceRoots: string[]): string | null {
  if (path.isAbsolute(xsdPath)) {
    return fs.existsSync(xsdPath) ? xsdPath : null;
  }
  for (const root of workspaceRoots) {
    const candidate = path.join(root, xsdPath);
    if (fs.existsSync(candidate)) return candidate;
  }
  return null;
}
