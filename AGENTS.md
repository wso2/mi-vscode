# Repository Notes

This repository is MI-only.

## Layout

- `packages/mi-extension`: VS Code extension source.
- `packages/mi-language-server`: language server build project.
- `packages/*`: local MI packages used by the extension.
- `submodules/vscode-extensions`: shared packages consumed through the git submodule.

Use `packages/...` paths for local source changes. Do not reintroduce the old `workspaces/mi/...` structure.

## Commands

Run these from the repository root:

```bash
pnpm run init-submodules
rush install
rush build
rush build --to language-server
rush build --to micro-integrator
pnpm run build
pnpm run build:ls
pnpm run build:mi
```

## Build Behavior

- `rush build --to language-server` builds the local language server from `packages/mi-language-server`.
- The default language server build copies artifacts into `packages/mi-extension/ls`.
- `rush build --to micro-integrator` builds the language server first, then the extension flow.
- Set `MI_DOWNLOAD_LS=true` to use the download flow instead of the local language server build.
