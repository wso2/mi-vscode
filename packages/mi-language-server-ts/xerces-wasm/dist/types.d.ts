export interface Diagnostic {
    message: string;
    line: number;
    column: number;
    severity: "warning" | "error" | "fatal";
}
export interface ValidationResult {
    valid: boolean;
    parseErrors: Diagnostic[];
    schemaErrors: Diagnostic[];
}
export type XmlInput = string | Buffer | Blob | File;
export interface SchemaBundle {
    entry: XmlInput;
    imports?: Record<string, XmlInput>;
}
export type XsdInput = XmlInput | SchemaBundle;
export interface ProjectFiles {
    [filename: string]: XmlInput;
}
export interface ProjectValidatorOptions {
    entry: string;
    files: ProjectFiles;
    targetNamespace?: string;
}
export interface ProjectValidator {
    validate(xml: XmlInput): Promise<ValidationResult>;
    reload(files: ProjectFiles): Promise<void>;
    destroy(): void;
}
//# sourceMappingURL=types.d.ts.map