# mi-migrate CLI

Standalone command-line tool for migrating WSO2 Integration Studio projects to the MI VS Code extension project structure.

> **Scope:** `migrate` converts the *project structure* from Integration Studio format to VS Code format. It does **not** modify artifact configuration — sequences, APIs, proxies, endpoints, and other synapse artifacts are carried over as-is. Any configuration changes required for a newer MI runtime (connector upgrades, script engine changes, etc.) must be applied manually in VS Code after migration.

## Prerequisites

- **Java** — required by Maven for POM resolution
- **Maven** (`mvn`) — must be on `PATH`

## Usage

```
mi-migrate <command> [options] <source>
```

### Commands

#### `analyze` — inspect a project without migrating

Scans the source project or workspace and reports compatibility issues. Supports both **Integration Studio project structure** and **VS Code MI project structure** — useful for pre-migration planning or for reviewing an already-migrated VS Code project.

Does **not** change project sources. After the scan, an `analysis-summary.md` is written into each analyzed project directory with a full breakdown of findings.

```
mi-migrate analyze [options] <source>
```

| Option | Description |
|--------|-------------|
| `--target-version <version>` | Target MI runtime version (default: `4.7.0`) |
| `--format <fmt>` | Output format: `text` or `json` (default: `text`) |
| `--fail-on-breaking` | Exit with code `2` if breaking changes are found |
| `--java-home <path>` | Override `JAVA_HOME` for Maven invocations |
| `--log-file <path>` | Append all log output to a file |

**Examples**

```bash
# Analyze an Integration Studio project before migrating
mi-migrate analyze ./MyIntegrationProject

# Analyze an already-migrated VS Code project
mi-migrate analyze ./MyIntegrationProject-migrated

# Analyze a workspace, output JSON, fail on breaking changes
mi-migrate analyze ./workspace --format json --fail-on-breaking

# Target a specific MI version
mi-migrate analyze ./MyProject --target-version 4.3.0
```

**Output**

Each analyzed project directory receives an `analysis-summary.md` file containing:
- **Breaking changes** — issues that will cause runtime failures on the target MI version (e.g. incompatible connector versions, removed transport support)
- **Warnings** — issues that need attention but are not hard failures (e.g. script engine changes, class mediators)

The console prints a short summary and lists the paths to all written files.

**Exit codes**

| Code | Meaning |
|------|---------|
| `0` | Analysis complete, no breaking changes (or `--fail-on-breaking` not set) |
| `1` | Unexpected error |
| `2` | Breaking changes found (only when `--fail-on-breaking` is set) |

---

#### `migrate` — migrate an Integration Studio project to VS Code structure

> **Integration Studio projects only.** `migrate` expects a WSO2 Integration Studio project or workspace as input. It is not intended for projects already in VS Code MI format.

Copies the source to an output directory and converts it from Integration Studio project structure to the MI VS Code extension format. After migration completes, an `analysis-summary.md` is written to the output project listing any items that need attention in VS Code (connector upgrades, script engine changes, etc.).

```
mi-migrate migrate [options] <source>
```

| Option | Description |
|--------|-------------|
| `--target-version <version>` | Target MI runtime version (default: `4.7.0`) |
| `-o, --output <path>` | Output directory (default: `<source>-migrated`) |
| `-y, --yes` | Non-interactive: skip all confirmation prompts |
| `--java-home <path>` | Override `JAVA_HOME` for Maven invocations |
| `--log-file <path>` | Append all log output to a file (see also [Migration logs](#migration-logs)) |

**Examples**

```bash
# Migrate a single Integration Studio project
mi-migrate migrate ./MyIntegrationProject

# Migrate a workspace to a custom output path
mi-migrate migrate ./workspace --output ./workspace-migrated

# Non-interactive migration targeting a specific version (for CI/CD)
mi-migrate migrate ./MyProject --yes --target-version 4.3.0 --log-file migration.log
```

### Migration logs

After a successful `migrate` run, the output directory contains a `.backup/` folder alongside each migrated project with two files:

| File | Contents |
|------|----------|
| `.backup/migration.log` | Timestamped log of everything that happened during migration |
| `.backup/skipped-files-during-migration.txt` | Files that could not be migrated and were left unchanged |

These are always written automatically — `--log-file` is only needed if you want an additional copy of the console output (e.g. for CI pipelines).

---

### Workspace support

Both commands automatically detect whether `<source>` is a single project or a workspace containing multiple projects. No flags are needed — the tool adapts accordingly.

### Environment variables

| Variable | Description |
|----------|-------------|
| `JAVA_HOME` | Java installation path (can also be set via `--java-home`) |

> **Development only:** set `MI_MIGRATE_RESOURCES_DIR=/path/to/resources` to override the bundled resources without rebuilding the binary.
