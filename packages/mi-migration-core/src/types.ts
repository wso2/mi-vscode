/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Abstracts platform-specific behaviours so the migration core can run
 * both inside the VS Code extension and as a standalone CLI.
 *
 * VS Code implementation  → window.show*, extension.context.asAbsolutePath, workspace.getConfiguration
 * CLI implementation      → console.*, path.join(__dirname, ...), commander flags
 */
export interface MigrationHost {
    /** Emit an informational, warning, or error message to the user. */
    notify(level: 'info' | 'warn' | 'error', message: string): void;

    /**
     * Resolve a path relative to the package's bundled resources directory
     * (docker-resources/, maven-wrapper/, …).
     */
    getResourcePath(relative: string): string;

    /** Read a boolean configuration value by key. */
    getConfig(key: MigrationConfigKey): boolean;

    /**
     * The MI runtime version to write into the migrated project's pom.xml.
     * `sourceVersion` is the value from the original project (may be undefined).
     *
     * VS Code implementation: returns `sourceVersion ?? LATEST_MI_VERSION`
     * (preserves the existing version; only falls back to latest when absent).
     * CLI implementation: always returns the --target-version flag value,
     * ignoring the source (allows explicit version targeting).
     */
    getTargetVersion(sourceVersion?: string): string;

    /**
     * Ask the user a yes/no question. Returns true to proceed, false to abort.
     * In non-interactive mode (e.g. --yes flag) this should always return true.
     */
    confirmProceed(message: string): Promise<boolean>;
}

export type MigrationConfigKey =
    | 'useLocalMaven'
    | 'useDefaultMavenForMigration'
    | 'convertToConsolidated'
    | 'runAnalysis';

export interface MigrationRequest {
    /** Absolute path to the Integration Studio project. */
    source: string;
    /** Target directory for the migrated project (defaults to source). */
    directory: string;
}

export interface MigrationResult {
    /** Absolute path to each created project directory. */
    filePaths: string[];
}
