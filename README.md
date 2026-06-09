# Micro Integrator for Visual Studio Code (MI for VS Code) 

WSO2 Micro Integrator (MI) offers an extension for Visual Studio Code (VS Code) that simplifies the development of integration solutions.

## Repository Development

This repository is focused on MI only. Local source packages live under `packages/`, and shared libraries are consumed directly from the `submodules/vscode-extensions` git submodule.

Important locations:

- `packages/mi-extension`: VS Code extension source.
- `packages/mi-language-server`: MI language server build project.
- `packages/*`: local MI packages used by the extension.
- `submodules/vscode-extensions`: shared packages used during Rush builds.

First-time setup from the repository root:

```bash
pnpm run init-submodules
rush install
```

`pnpm run init-submodules` updates the shared `vscode-extensions` submodule to the latest commit on its configured branch for local development.

CI intentionally runs the same script without `--remote`, so pipeline builds stay pinned to the submodule commit recorded in this repository.

If updating the submodule changes shared package metadata, run:

```bash
pnpm run init-submodules
rush update
```

Commit the updated `submodules/vscode-extensions` gitlink and `common/config/rush/pnpm-lock.yaml` when they change.

Common build commands:

```bash
rush build
rush build --to language-server
rush build --to micro-integrator
```

Helpful shortcuts:

```bash
pnpm run build
pnpm run build:ls
pnpm run build:mi
```

By default, the language server is built from `packages/mi-language-server`, and the generated artifacts are copied into `packages/mi-extension/ls` before the extension build runs. To use the download flow instead of the local language server build, set `MI_DOWNLOAD_LS=true`.

## Prerequisites

You need the following to work with the MI for VS Code extension.

- Java Development Kit (JDK)
- WSO2 Micro Integrator (MI) runtime

If these are not installed on your local machine, the Micro Integrator for VS Code extension will automatically prompt you to download and configure them during the project creation step, depending on the project runtime version.

If a different JDK or WSO2 MI version is installed on your local machine, you'll be prompted to download the required versions.

If the required JDK and WSO2 MI versions are already installed, you can directly configure the Java Home and MI Home paths in this step.

## Get Started

1. Launch VS Code with the Micro Integrator for Visual Studio Code (MI for VS Code) extension installed. When the extension is installed properly, you can see the Micro Integrator icon in the Activity Bar of the VS Code editor.

2. Click on the Micro Integrator icon on the Activity Bar of the VS Code editor to open the extension and get started.

    <img src="https://github.com/wso2/docs-mi/blob/main/en/docs/assets/img/develop/mi-for-vscode/mi-vscode-extension.png?raw=true" width="100%" />

When you open the extension for the first time, you'll see the **Design View** panel on the right side and the **Micro Integrator: Project Explorer** view on the left.

<img src="https://github.com/wso2/docs-mi/blob/main/en/docs/assets/img/develop/mi-for-vscode/getting-started.png?raw=true" width="100%" />

To get started, you need to first create the integration project. You can either open a folder containing an integration project or create a new project. Alternatively, you can use an integration sample provided under Explore Samples, which will generate the required projects and files for a specific use case.

## Micro Integrator Project Explorer

Micro Integrator (MI) Project Explorer provides a view of all the project directories created for your integration solution. Shown below is the project explorer of a sample project.

<img src="https://github.com/wso2/docs-mi/blob/main/en/docs/assets/img/develop/mi-for-vscode/project-explorer.png?raw=true" width="100%" />

You can add the artifacts required for your integration using MI Project Explorer.

## WSO2 MI Copilot

The WSO2 Micro Integrator (MI) Copilot is an AI-powered tool that simplifies the process of creating integration scenarios. It allows you to specify integration requirements using natural language or by providing relevant files, such as OpenAPI specifications. MI Copilot generates the necessary integration artifacts, which can be seamlessly incorporated into your projects. You can iteratively refine your projects through conversational prompts, enabling the addition of features or modifications with ease. This approach supports incremental development, allowing you to build and enhance your integration projects over time.

<img src="https://github.com/wso2/docs-mi/blob/main/en/docs/assets/img/develop/mi-for-vscode/open-ai-panel.png?raw=true" width="100%" />

You can create any integration project by entering your integration scenario in natural language into the provided text box, allowing AI to generate the necessary artifacts.

You can provide integration requirements as:

- Text prompts: Describe your integration scenario in natural language.
- Files: Upload relevant files, such as OpenAPI specifications, that provide additional context for the integration.

<img src="https://github.com/wso2/docs-mi/blob/main/en/docs/assets/img/develop/mi-for-vscode/mi-copilot.png?raw=true" width="100%" />

## Samples

The **Design View** lists a set of sample projects and integration artifacts that represent common integration scenarios. You can use these to explore WSO2 Micro Integrator and to try out common integration use cases.

<img src="https://github.com/wso2/docs-mi/blob/main/en/docs/assets/img/develop/mi-for-vscode/samples.png?raw=true" width="100%" />

## Documentation

To learn more about the Micro Integrator for Visual Studio Code extension, go to the [Micro Integrator for VS Code](https://mi.docs.wso2.com/en/latest/develop/mi-for-vscode/mi-for-vscode-overview/) documentation.

## Reach Out

For further assistance, create a [GitHub issue](https://github.com/wso2/mi-vscode/issues). Our team will review and respond promptly to address your concerns.
