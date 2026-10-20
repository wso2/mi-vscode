"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createProjectValidator = createProjectValidator;
const index_1 = require("./index");
async function filesToTextMap(files) {
    const out = {};
    await Promise.all(Object.entries(files).map(async ([name, val]) => {
        out[name] = await (0, index_1.toText)(val);
    }));
    return out;
}
async function createProjectValidator(options) {
    const mod = await (0, index_1.getModule)();
    const filesText = await filesToTextMap(options.files);
    if (!(options.entry in filesText)) {
        throw new Error(`ProjectValidator: entry "${options.entry}" not found in files`);
    }
    const instance = new mod.ProjectValidator();
    const ok = instance.init(options.entry, filesText, options.targetNamespace ?? null);
    if (!ok) {
        instance.delete();
        throw new Error(`ProjectValidator: failed to compile schema for entry "${options.entry}"`);
    }
    let destroyed = false;
    return {
        async validate(xml) {
            if (destroyed)
                throw new Error("ProjectValidator: already destroyed");
            return instance.validate(await (0, index_1.toText)(xml));
        },
        async reload(files) {
            if (destroyed)
                throw new Error("ProjectValidator: already destroyed");
            const filesText = await filesToTextMap(files);
            const ok = instance.init(options.entry, filesText, options.targetNamespace ?? null);
            if (!ok)
                throw new Error(`ProjectValidator: failed to recompile schema on reload`);
        },
        destroy() {
            if (destroyed)
                return;
            destroyed = true;
            instance.delete();
        },
    };
}
//# sourceMappingURL=project-validator.js.map