import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

export interface SchemaAssociation {
  pattern: string;     // glob-like: 'project-430/**/*.xml' or '**/*.xml'
  namespace?: string;  // xmlns namespace URI
  xsdPath: string;     // absolute path to XSD file
  isBuiltIn: boolean;  // true for bundled schemas
}

export interface ResolvedSchema {
  xsdText: string;
  xsdPath: string;
  source: "builtin" | "custom";
}

// Resolve schemas root for both production bundle (dist/server.js) and test environment.
// In the production esbuild bundle import.meta.url points to dist/server.js, so one
// level up reaches the project root.  In tests import.meta.url points to the TypeScript
// source file (src/schema/schemaAssociator.ts), so two levels up are needed.
function resolveSchemaRoot(): string {
  try {
    const dir = path.dirname(fileURLToPath(import.meta.url));
    const prod = path.join(dir, "..", "resources", "schemas");
    if (fs.existsSync(prod)) return prod;
    return path.join(dir, "..", "..", "resources", "schemas");
  } catch {
    return path.join(process.cwd(), "resources", "schemas");
  }
}

const SCHEMAS_ROOT = resolveSchemaRoot();

export class SchemaAssociator {
  private builtInAssociations: SchemaAssociation[];
  private userAssociations: SchemaAssociation[];

  constructor() {
    this.builtInAssociations = [
      {
        namespace: "http://ws.apache.org/ns/synapse",
        xsdPath: path.join(SCHEMAS_ROOT, "440", "synapse_config.xsd"),
        pattern: "**/*.xml",
        isBuiltIn: true,
      },
    ];
    this.userAssociations = [];
  }

  /**
   * Registers a custom schema association.
   * Custom associations take priority over built-in schemas when patterns overlap.
   */
  addUserAssociation(association: SchemaAssociation): void {
    this.userAssociations.push(association);
  }

  /**
   * Finds and reads the XSD schema for the given file name and optional xmlns namespace.
   * Priority: user associations (pattern match) > built-in associations (namespace match).
   * Returns null if no matching schema is found.
   */
  findSchema(fileName: string, xmlns?: string, documentPath?: string): ResolvedSchema | null {

    // user associations checked FIRST (pattern match)
    for (const assoc of this.userAssociations) {
      if (this.matchesPattern(fileName, assoc.pattern, documentPath)) {
        const xsdText = this.readXsdFile(assoc.xsdPath);
        if (xsdText === null) return null;
        return { xsdText, xsdPath: assoc.xsdPath, source: "custom" };
      }
    }

    // built-ins matched by namespace only
    for (const assoc of this.builtInAssociations) {
      if (xmlns !== undefined && assoc.namespace === xmlns) {
        const xsdText = this.readXsdFile(assoc.xsdPath);
        if (xsdText === null) return null;
        return { xsdText, xsdPath: assoc.xsdPath, source: "builtin" };
      }
    }

    return null;
  }

  private matchesPattern(fileName: string, pattern: string, documentPath?: string): boolean {
    if (fileName === pattern) return true;
    if (documentPath && this.globMatches(pattern, documentPath)) return true;
    // Fallback for simple **/*.ext patterns — match by filename suffix alone.
    const stripped = pattern.replace(/^\*\*\//, "");
    return !stripped.includes("/") && fileName.endsWith(stripped);
  }

  // Converts a glob pattern to a RegExp and tests it against filePath.
  // Supports * (single-segment wildcard) and ** (multi-segment wildcard).
  private globMatches(glob: string, filePath: string): boolean {
    let re = "";
    let i = 0;
    while (i < glob.length) {
      const ch = glob[i];
      if (ch === "*" && glob[i + 1] === "*") {
        re += ".*";
        i += 2;
        if (glob[i] === "/") i++;
      } else if (ch === "*") {
        re += "[^/]*";
        i++;
      } else if (ch === "?") {
        re += "[^/]";
        i++;
      } else {
        re += ch.replace(/[.+^${}()|[\]\\]/g, "\\$&");
        i++;
      }
    }
    return new RegExp(`(^|/)${re}$`).test(filePath);
  }

  /** Reads and returns the content of an XSD file, or null if not found. */
  private readXsdFile(xsdPath: string): string | null {
    try {
      return fs.readFileSync(xsdPath, "utf-8");
    } catch (err) {
      console.warn(`Schema file not found at "${xsdPath}": ${(err as Error).message}`);
      return null;
    }
  }
}
