// @ts-ignore
import { validate } from "../../xerces-wasm/dist/index.js";
import type { ValidationResult, Diagnostic as XercesDiagnostic, SchemaBundle as XercesSchemaBundle, XmlInput as XercesXmlInput, XsdInput as XercesXsdInput } from "../../xerces-wasm/dist/types.js";
import { Range } from "../utils/rangeUtils.js";
import { Position } from "../utils/positionUtils.js";

// ── Public types ──────────────────────────────────────────────────────────────

/** A validation diagnostic used throughout the language service. */
export interface Diagnostic {
  range: Range;
  message: string;
  severity: "error" | "warning" | "info";
  source: "xsd" | "syntax";
}

export type XmlInput = XercesXmlInput;

/**
 * A bundle of schemas for xs:import / xs:include support.
 * `entry` is the root XSD content.
 * `imports` maps relative filenames (matching schemaLocation values) to their XSD content.
 */
export type SchemaBundle = XercesSchemaBundle;

export type XsdInput = XercesXsdInput;

// ── Helpers ───────────────────────────────────────────────────────────────────

async function toText(input: XmlInput): Promise<string> {
  if (typeof input === "string") return input;
  if (Buffer.isBuffer(input)) return input.toString("utf8");
  if (typeof Blob !== "undefined" && input instanceof Blob) return input.text();
  throw new TypeError("Unsupported input type");
}



function isSchemaBundle(xsd: XsdInput): xsd is SchemaBundle {
  return typeof xsd === "object" && !Buffer.isBuffer(xsd) && "entry" in xsd;
}

// Xerces reports the column at the closing '>' of the problematic tag.
// Walk backward on that line to find '<' so the full tag name is highlighted.
function toRange(line: number, column: number, xmlLines: string[]): Range {
  const l = line > 0 ? line - 1 : 0;
  const c = column > 0 ? column - 1 : 0;

  const lineText = xmlLines[l] ?? "";
  const tagStart = lineText.lastIndexOf("<", c);
  if (tagStart !== -1) {
    return {
      start: { line: l, character: tagStart },
      end: { line: l, character: c + 1 },
    };
  }

  const pos: Position = { line: l, character: c };
  return { start: pos, end: pos };
}

// Xerces embeds the mismatched start-tag name in messages like:
//   "The element type "sequnce" must be terminated by the matching end-tag"
// Extract it so we can locate the open tag in the source.
const MISMATCH_RE = /element type ["']([^"']+)["'] must be terminated/i;

// Xerces reports content-model violations at the parent's closing tag, e.g.:
//   "element 'propert' is not allowed for content model '(...)'"
// Extract the disallowed child name so we can remap the error to the child's open tag.
const CONTENT_MODEL_RE = /\belement ['"](?:[^'"]*})?([^'"]+)['"]\s+is not allowed/;

// Search backward from `beforeLine`/`beforeCol` for <tagName (open tag).
function findOpenTagRange(tagName: string, beforeLine: number, beforeCol: number, xmlLines: string[]): Range | null {
  const needle = `<${tagName}`;
  for (let l = beforeLine; l >= 0; l--) {
    const lineText = xmlLines[l] ?? "";
    const searchTo = l === beforeLine ? beforeCol : lineText.length;
    const idx = lineText.lastIndexOf(needle, searchTo);
    if (idx !== -1) {
      return {
        start: { line: l, character: idx },
        end: { line: l, character: idx + needle.length },
      };
    }
  }
  return null;
}

function mapResults(result: ValidationResult, xmlText: string): Diagnostic[] {
  const xmlLines = xmlText.split("\n");
  const diagnostics: Diagnostic[] = [];
  for (const d of result.parseErrors) {
    const closeRange = toRange(d.line, d.column, xmlLines);
    diagnostics.push({
      message: d.message,
      severity: "error",
      source: "syntax",
      range: closeRange,
    });

    // For mismatched-tag errors also mark the offending open tag.
    const m = MISMATCH_RE.exec(d.message);
    if (m) {
      const openTagName = m[1];
      const errLine = d.line > 0 ? d.line - 1 : 0;
      const errCol  = d.column > 0 ? d.column - 1 : 0;
      const openRange = findOpenTagRange(openTagName, errLine, errCol, xmlLines);
      if (openRange) {
        diagnostics.push({
          message: `'<${openTagName}>' has no matching end-tag`,
          severity: "error",
          source: "syntax",
          range: openRange,
        });
      }
    }
  }
  for (const d of result.schemaErrors) {
    const errLine = d.line > 0 ? d.line - 1 : 0;
    const errCol  = d.column > 0 ? d.column - 1 : 0;
    let range = toRange(d.line, d.column, xmlLines);

    const cm = CONTENT_MODEL_RE.exec(d.message);
    if (cm) {
      const localName = cm[1];
      const pinned = findOpenTagRange(localName, errLine, errCol, xmlLines);
      if (pinned) range = pinned;
    }

    diagnostics.push({
      message: d.message,
      severity: d.severity === "warning" ? "warning" : "error",
      source: "xsd",
      range,
    });
  }
  return diagnostics;
}

// ── XsdValidatorService ───────────────────────────────────────────────────────

/**
 * Wraps the Xerces WASM validator.
 * Accepts a plain XSD string or a SchemaBundle for xs:include / xs:import support.
 * Xerces runs syntax parsing + schema validation in a single SAX pass, so both
 * syntax errors and schema errors are returned together even on malformed XML.
 */
export class XsdValidatorService {
  private xsd: XsdInput;

  private constructor(xsd: XsdInput) {
    this.xsd = xsd;
  }

  static async create(xsd: XsdInput): Promise<XsdValidatorService> {
    return new XsdValidatorService(xsd);
  }

  async validate(xmlText: string): Promise<Diagnostic[]> {
    let result: ValidationResult;

    // We do this if/else check simply because the Xerces validator requires the 
    // `targetNamespace` as the 3rd parameter. We need to extract it via Regex, and 
    // the text is located in a different property depending on if it's a Bundle or a String.
    // Notice that `this.xsd` is passed directly as the 2nd parameter in both cases.
    if (isSchemaBundle(this.xsd)) {
      const entryText = await toText(this.xsd.entry);
      const targetNs = entryText.match(/\btargetNamespace\s*=\s*(["'])(.*?)\1/)?.[2] ?? "";
      result = await validate(xmlText, this.xsd, targetNs);
    } else {
      const xsdText = await toText(this.xsd);
      const targetNs = xsdText.match(/\btargetNamespace\s*=\s*(["'])(.*?)\1/)?.[2] ?? "";
      result = await validate(xmlText, this.xsd, targetNs);
    }

    console.error(`[xsdValidator] raw result: valid=${result.valid} parseErrors=${result.parseErrors.length} schemaErrors=${result.schemaErrors.length}`);
    if (result.schemaErrors.length > 0) {
      console.error(`[xsdValidator] schemaErrors[0]: ${JSON.stringify(result.schemaErrors[0])}`);
    }

    return mapResults(result, xmlText);
  }

  dispose(): void {
    // WASM module is shared — nothing to release per instance
  }
}
