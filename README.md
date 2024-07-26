# Micro Integrator for Visual Studio Code

WSO2 Micro Integrator Visual Studio Code extension (MI for VSCode) is a comprehensive integration solution that simplifies your digital transformation journey. It streamlines connectivity among applications, services, data, and the cloud using a user-friendly low-code graphical designing experience and revolutionizes your integration development workflow. As an integration developer, you can execute all the development lifecycle phases using this tool. When your integration solutions are production-ready, you can easily push the artifacts to your continuous integration/continuous deployment pipeline.

## Prerequisites

- **Java Development Kit (JDK):** Version 11 or 17 is required. Ensure the JDK is properly configured in your system's PATH environment variable.
- **Apache Maven:** Ensure Apache Maven is installed and its path is correctly set within the system's PATH environment variable.
- **WSO2 Micro Integrator 4.3.0 Runtime:** Set up WSO2 Micro Integrator 4.3.0 runtime on your machine.  
    1. Download the Micro Integrator 4.3.0 distribution as a ZIP file from [here](https://github.com/wso2/micro-integrator/releases/download/v4.3.0/wso2mi-4.3.0.zip).
    2. Extract the ZIP file. Hereafter, this extracted folder will be referred to as the `<MI_HOME>` folder.

After completing the steps above, follow the instructions below to set up the workspace:

1. Launch VS Code with the Micro Integrator extension installed. When the extension is installed properly, you can see the Micro Integrator icon in the Activity Bar of the VSCode editor.

2. Click on the Micro Integrator icon on the Activity Bar of the VS Code editor.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/mi-vscode-extension.png?raw=true" width="100%" />

3. Click on the **Command Palette** on the top of the VS Code.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/qsg/command-palette.png?raw=true" width="100%" />

4. Type `>` to show the available commands. Alternatively, you can open the command palette in VS Code by entering `Command`+`Shift`+`P` on macOS and `Ctrl`+`Shift`+`P` on Windows.

5. Select **MI: Add MI server** from the list of available commands.

6. Click **Add MI server** to add a Micro Integrator server.

7. Select the folder where `<MI_HOME>` is located. This wll be set as the **current server path**.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/qsg/current-server-path.png?raw=true" width="100%" />

## Overview

When you open the extension for the first time, you'll see the Design View panel on the right side and the Micro Integrator: Project Explorer view on the left.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/getting-started.png?raw=true" width="100%" />

To get started, you need to first create the required project directories. You can either open a folder containing a MI project or create a new project. Alternatively, you can use an integration sample provided under Explore Samples, which will generate the required projects and files for a specific use case.

To create a new project you can use the links on the MI Project Explorer or Design View.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/create-new-project.png?raw=true" width="100%" />

These project directories will be saved to your workspace and can be accessed later from Project Explorer.

## Samples

The Design View lists a set of sample projects and integration artifacts that represent common integration scenarios. You can use these to explore WSO2 Micro Integrator and to try out common integration use cases.

<img src="https://github.com/wso2/docs-mi/blob/4.2.0/en/docs/assets/img/develop/mi-for-vscode/samples.gif?raw=true" width="100%" />

Once you create an integration project, you will see the Add Artifact panel where you can start the integration with one of the following options:

- Describe your Integration to generate with AI
- Start with Entry Points and Other Artifacts

To start an integration, you need either API, Proxy, Task, or Inbound Endpoint as the entry points. You can add the other artifacts using the Add Artifacts panel or the Project Explorer.

## Micro Integrator Project Explorer

The Micro Integrator Project Explorer provides a view of all the project directories created for your integration solution. Shown below is the project explorer of a sample project.

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

To learn more about the Micro Integrator extension for Visual Studio Code, go to the [Micro Integrator for VSCode documentation](https://mi.docs.wso2.com/en/latest/develop/mi-for-vscode/mi-for-vscode-overview/).
