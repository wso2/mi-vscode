# Source Organization

This document describes the structure and organization of the `mi-vscode` repository.

---

## Overview

This repository contains the Micro Integrator for VS Code extension, its supporting MI packages, and the MI language server build integration. It is managed using [Rush](https://rushjs.io/) for consistent dependency management and builds.

---

## Directory Structure

```text
mi-vscode/
|
+-- packages/
|   +-- mi-component-diagram/
|   +-- mi-core/
|   +-- mi-data-mapper/
|   +-- mi-data-mapper-utils/
|   +-- mi-diagram/
|   +-- mi-extension/
|   +-- mi-language-server/
|   +-- mi-rpc-client/
|   +-- mi-visualizer/
|   +-- syntax-tree/
|
+-- submodules/
|   +-- vscode-extensions/
|
+-- common/
+-- .github/
+-- rush.json
+-- pnpm-workspace.yaml
```

---

## Key Components

- **Local MI packages**: Each MI package resides in its own folder under `packages/`.
- **VS Code extension**: `packages/mi-extension` contains the MI extension source and packaging logic.
- **Language server build**: `packages/mi-language-server` builds the MI language server and copies artifacts into `packages/mi-extension/ls`.
- **Shared dependencies**: Shared packages are sourced from the `submodules/vscode-extensions` git submodule.
---

## Environment Variables

A `.env` file is **required** for the following extension:
- `packages/mi-extension`

Please refer to `packages/mi-extension/.env.example` for the required variables and structure.  
**You must copy `.env.example` to `.env` and fill in the necessary values before building or running the extension.**

---

## Node.js Version

- **Recommended Node.js version:** `v22`
- **Supported Node.js versions:** `v20` to `v22`

Make sure you are using a compatible Node.js version for development and builds.

---

## Development Workflow

- **Submodule Setup**: Run `pnpm run init-submodules` before the first install and whenever the shared submodule paths need to be refreshed.
- **Dependency Management**: Use Rush commands (`rush install`, `rush build`, etc.) for all dependency and build operations.
- **Environment Setup**: Ensure `packages/mi-extension/.env` exists when the extension requires environment variables.
- **Build Targets**: Use `rush build --to language-server` to build only the language server, or `rush build --to micro-integrator` to build the full MI extension flow.
- **Language Server Packaging**: The default build path compiles the language server locally and copies the generated artifacts into `packages/mi-extension/ls`. Set `MI_DOWNLOAD_LS=true` to use the download flow instead.
- **Code Review**: Submit pull requests for all changes and follow the repository’s contribution guidelines for code quality and review.

---

## Contribution Guidelines

- Fork the repository before making changes.
- Follow the structure and conventions outlined in this document.
- Submit pull requests with clear descriptions and reference related issues if applicable.

---

## Contact

For questions or support, please open an issue in the [GitHub repository](https://github.com/wso2/mi-vscode) or contact the WSO2 developer team.

---
