"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.createProjectValidator = void 0;
exports.getModule = getModule;
exports.toText = toText;
exports.validate = validate;
exports.validateFiles = validateFiles;
// @ts-ignore
const xerces_validator_js_1 = __importDefault(require("../wasm/xerces_validator.js"));
const promises_1 = require("fs/promises");
// ── WASM module singleton ─────────────────────────────────────────────────────
let _module = null;
async function getModule() {
    if (!_module)
        _module = await (0, xerces_validator_js_1.default)();
    return _module;
}
async function toText(input) {
    if (typeof input === "string")
        return input;
    if (Buffer.isBuffer(input))
        return input.toString("utf8");
    if (typeof Blob !== "undefined" && input instanceof Blob)
        return input.text();
    throw new TypeError("Unsupported input type");
}
// ── One-off validation ────────────────────────────────────────────────────────
function isSchemaBundle(xsd) {
    return typeof xsd === "object" && !Buffer.isBuffer(xsd) && "entry" in xsd;
}
async function validate(xml, xsd, targetNamespace) {
    const mod = await getModule();
    const xmlText = await toText(xml);
    const ns = targetNamespace ?? null;
    if (isSchemaBundle(xsd)) {
        const entryText = await toText(xsd.entry);
        const imports = {};
        if (xsd.imports) {
            await Promise.all(Object.entries(xsd.imports).map(async ([key, val]) => {
                imports[key] = await toText(val);
            }));
        }
        return mod.validate(xmlText, { entry: entryText, imports }, ns);
    }
    return mod.validate(xmlText, await toText(xsd), ns);
}
async function validateFiles(xmlPath, xsd) {
    const mod = await getModule();
    if (typeof xsd === "string") {
        const [xml, xsdText] = await Promise.all([
            (0, promises_1.readFile)(xmlPath, "utf8"),
            (0, promises_1.readFile)(xsd, "utf8"),
        ]);
        return mod.validate(xml, xsdText);
    }
    const [xml, entryText] = await Promise.all([
        (0, promises_1.readFile)(xmlPath, "utf8"),
        (0, promises_1.readFile)(xsd.entry, "utf8"),
    ]);
    const imports = {};
    if (xsd.imports) {
        await Promise.all(Object.entries(xsd.imports).map(async ([key, filePath]) => {
            imports[key] = await (0, promises_1.readFile)(filePath, "utf8");
        }));
    }
    return mod.validate(xml, { entry: entryText, imports });
}
var project_validator_1 = require("./project-validator");
Object.defineProperty(exports, "createProjectValidator", { enumerable: true, get: function () { return project_validator_1.createProjectValidator; } });
//# sourceMappingURL=index.js.map