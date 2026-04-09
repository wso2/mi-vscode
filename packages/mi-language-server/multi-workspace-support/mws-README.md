# Implementing Multi-Project Workspace Support for Current Language Server

## Problem in Current Language Server
The current WSO2 Micro Integrator (MI) Language Server typically serves each MI project with a separate language server instance when multiple projects exist in a workspace. This limits scalability, increases resource overhead, and breaks true multi-folder workspace experiences.

## Architecture Roadmap

### Stage 1: Core XML Validation & Schema Isolation **(Completed)**
Enable independent XML validation for multiple projects within a single Language Server instance using dynamic File Associations.

### Stage 2: Eliminating Global State, Singletons & Context-Aware
**Goal:** Transition legacy singletons (e.g., `ConnectorHolder`, `SynapseLanguageService`, Mediator Handlers) to be resolved per project context instead of a global state.

**Action Plan:** 

* Introduce `ProjectContext` and `WorkspaceManager` classes to manage memory scoped to individual projects.
    *   *Reference Files:* `multi-workspace-support/resources/ProjectContext.java`, `multi-workspace-support/resources/WorkspaceManager.java`

* Isolate Language Server features (Auto-Complete, Go-To-Definition) per workspace.


### Stage 3: Language Client (VS Code Extension) Integration
Update the frontend VS Code Extension to natively support the multi-project backend API configurations and event hooks.

---
---
---

## Stage 1 Completed: LemMinX File Associations for Workspace Schema Validation

### Technical Overview
Introduced **File Associations** to replace the legacy namespace-based `.catalog` implementation. Instead of relying on rigid catalogs, this approach maps specific file path patterns (e.g., glob matches for a project folder) to a specific target schema path. This enables different schemas (e.g., MI 4.3.0 and 4.4.0) to be applied to different projects simultaneously without conflict in a single LemMinX instance.

### Changelog & Implementation Details

#### 1. `Utils.java` (`org.eclipse.lemminx/customservice/synapse/utils/Utils.java`)
*   **Removed Catalog Dependencies:** Replaced `updateSynapseCatalogSettings` with `updateSynapseFileAssociationSettings`. The initialization parameters logic was modified to drop the `catalogs` array and successfully inject `fileAssociations` instead.
*   **URI Path Sanitation Patch:** Updated internal path extraction from:
    *   *Old:* `String version = getServerVersion(projectUri, Constant.DEFAULT_MI_VERSION);`
    *   *New:* `String version = getServerVersion(getAbsolutePath(projectUri), Constant.DEFAULT_MI_VERSION);`
    *   *Rationale:* Previously, the `rootPath` field provided raw OS paths. In a Multi-Root architecture utilizing `workspaceFolders`, the data received is formatted as URIs (`file:///...`). Adding `getAbsolutePath()` ensures the URI is cleanly scrubbed before Java's `Path.of()` tries to read the `pom.xml`.

#### 2. `XMLLanguageServer.java` (`org.eclipse.lemminx/XMLLanguageServer.java`)
*   Replaced the legacy initialization step `Utils.updateSynapseCatalogSettings(params)` with the new core standard: `Utils.updateSynapseFileAssociationSettings(params)`.
*   *Temporary Bridge/Hack:* Because `SynapseLanguageService` (Stage 2) is not yet fully isolated for multi-root awareness, a temporary bridge was established by setting its default Path to the first project in the collection: `synapseLanguageService.setSynapseXSDPath(workspaceSchemas.values().iterator().next());`

#### 3. `XMLWorkspaceService.java` (`org.eclipse.lemminx/XMLWorkspaceService.java`)
*   Enhanced `didChangeWorkspaceFolders` logic to capture dynamic workspace events correctly. If a user adds a new project to the workspace after server initialization, it accurately intercepts the action and generates/applies XSD schemas for the new folder.

#### 4. `CleanMultiRootValidationTest.java` (`.../extensions/contentmodel/CleanMultiRootValidationTest.java`)
Established three comprehensive multi-root tests demonstrating core functionality:
1.  **Multi-Root Isolation:** Verifies that a single Language Server successfully provides isolated validations to two independent projects governed by distinct XSD files.
2.  **Dynamic Connector Generation Test:** Ensures that dynamically generated connector schemas are instantly picked up by the validation engine logic, operating independently of server restarts.
3.  **Dynamic Workspace Handling:** Asserts that when an entirely new project is dynamically appended to the workspace context at runtime, the language server successfully triggers its standard MI validations for the newly tracked space.
