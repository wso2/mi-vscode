# Micro Integrator for Visual Studio Code

WSO2 Micro Integrator Visual Studio Code extension (MI for VSCode) is a comprehensive integration solution that simplifies your digital transformation journey. It streamlines connectivity among applications, services, data, and the cloud using a user-friendly low-code graphical designing experience and revolutionizes your integration development workflow. As an integration developer, you can execute all the development lifecycle phases using this tool. When your integration solutions are production-ready, you can easily push the artifacts to your continuous integration/continuous deployment pipeline.

## Installation Prerequisites

- Download and install Java SE Development Kit (JDK) version 11 or 17.

- Set the `JAVA_HOME` environment variable in the system settings.
- Download and install Apache Maven (version 3.6.0 onwards).

To learn more on installing the Micro Integrator extension for Visual Studio Code, go to the <a href="https://mi.docs.wso2.com/en/latest/develop/mi-for-vscode/install-wso2-mi-for-vscode/">Micro Integrator for VSCode documentation</a>.

## Overview

When the extension is installed properly, you can see the WSO2 icon in the Activity Bar of the VSCode editor.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/mi-vscode-extension.png?raw=true" width="100%" />

You can click the WSO2 icon to open the extension.

When you open the extension for the first time, you'll see the Design View panel on the right side and the Micro Integrator: Project Explorer view on the left.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/getting-started.png?raw=true" width="100%" />

To get started, you need to first create the required project directories. You can either open a folder containing a MI project or create a new project. Alternatively, you can use an integration sample provided under Explore Samples, which will generate the required projects and files for a specific use case.

To create a new project you can use the links on the MI Project Explorer or Design View.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/create-new-project.png?raw=true" width="100%" />

These project directories will be saved to your workspace and can be accessed later from Project Explorer.

## Samples

The Design View lists a set of sample projects and integration artifacts that represent common integration scenarios. You can use these to explore WSO2 Micro Integrator and to try out common integration use cases.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/samples.gif?raw=true" width="100%" />

Once you have created the required set of projects and artifacts, you can start working with the project directories and artifact editors.

Once you create an integration project, you will see the Add Artifact panel where you can start the integration with one of the following options:

- Describe your Integration to generate with AI
- Start with Entry Points and Other Artifacts

To start an integration, you need either API, Proxy, Task, or Inbound Endpoint as the entry points. You can add the other artifacts using the Add Artifacts panel or the Project Explorer.

## Project Explorer

The Micro Integrator: Project Explorer provides a view of all the project directories created for your integration solution. Shown below is the project explorer of a sample project.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/project-explorer.png?raw=true" width="100%" />

You can add the artifacts required for your integration using Project Explorer.

## AI Panel

WSO2 MI Copilot, a trained language model (LLM), demonstrates the capability to comprehend complex integration concepts and scenarios, allowing it to create tailored artifacts for different situations. This makes it versatile and useful for various integration tasks.

Clicking on the Open AI Panel icon located in the top right corner of VSCode will open the WSO2 MI Copilot interface.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/open-ai-panel.png?raw=true" width="100%" />

You can create any integration project by entering your integration scenario in natural language into the provided text box, allowing AI to generate the necessary artifacts.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/mi-copilot.png?raw=true" width="100%" />

## Reach Out

Reach out to us for assistance by creating [GitHub issues](https://github.com/wso2/mi-vscode/issues).

## Documentation

To learn more on the Micro Integrator extension for Visual Studio Code, go to the [Micro Integrator for VSCode documentation](https://mi.docs.wso2.com/en/latest/develop/mi-for-vscode/mi-for-vscode-overview/).
