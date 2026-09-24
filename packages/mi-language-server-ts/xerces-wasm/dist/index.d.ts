import type { XmlInput, XsdInput, ValidationResult } from "./types";
export declare function getModule(): Promise<any>;
export declare function toText(input: XmlInput): Promise<string>;
export declare function validate(xml: XmlInput, xsd: XsdInput, targetNamespace?: string): Promise<ValidationResult>;
export declare function validateFiles(xmlPath: string, xsd: string | {
    entry: string;
    imports?: Record<string, string>;
}): Promise<ValidationResult>;
export type { Diagnostic, ValidationResult, XmlInput, XsdInput, SchemaBundle, ProjectFiles, ProjectValidatorOptions, ProjectValidator, } from "./types";
export { createProjectValidator } from "./project-validator";
//# sourceMappingURL=index.d.ts.map