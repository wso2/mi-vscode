# WASM XML Validator & C++ Bridge

This directory contains the C++ bridge source code, Emscripten build tooling, and compilation scripts used to build the WebAssembly (WASM) XML validation engine powered by Apache Xerces-C++.

---

## 🛠️ How to Build WASM Binaries

### Prerequisites
- Bash shell (macOS or Linux)
- CMake
- Git

### Build Steps

1. Navigate to this directory:
   ```bash
   cd packages/mi-language-server-ts/assets/wasm-and-bridge
   ```

2. Run the WebAssembly build script:
   ```bash
   ./scripts/build-wasm.sh
   ```

### What the build script does:
1. Automatically sets up the pinned Emscripten SDK (`emsdk`).
2. Configures and compiles Apache Xerces-C++ static library (`libxerces-c.a`) targeting WebAssembly.
3. Compiles `native/xerces_bridge.cpp` with Emscripten bindings (`--bind`) into WebAssembly modules.

### Output Artifacts
The compiled WebAssembly artifacts will be output to:
- `wasm/xerces_validator.js`
- `wasm/xerces_validator.wasm`

---

## 📌 Additional References & Releases

For external source releases and standalone package distributions, refer to:

- **GitHub Repository (Released Source)**: [https://github.com/harshanacz/xerces-wasm-validator](https://github.com/harshanacz/xerces-wasm-validator)
- **NPM Package (Published Package)**: [https://www.npmjs.com/package/xerces-wasm](https://www.npmjs.com/package/xerces-wasm)
