# mi-migrate CLI

Standalone command-line tool for migrating WSO2 Integration Studio projects to the MI VS Code extension format.

## Prerequisites

- **Java** — required by Maven for POM resolution
- **Maven** (`mvn`) — must be on `PATH`

## Usage

```
mi-migrate <command> [options] <source>
```

### Commands

#### `analyze` — inspect a project without migrating

Scans the source project or workspace and reports issues that need attention before or after migration. Does **not** modify any files.

```
mi-migrate analyze [options] <source>
```

| Option | Description |
|--------|-------------|
| `--target-version <version>` | Target MI runtime version (default: `4.6.0`) |
| `--format <fmt>` | Output format: `text` or `json` (default: `text`) |
| `--fail-on-breaking` | Exit with code `2` if breaking changes are found |
| `--java-home <path>` | Override `JAVA_HOME` for Maven invocations |
| `--log-file <path>` | Append all log output to a file |

**Examples**

```bash
# Analyze a single project
mi-migrate analyze ./MyIntegrationProject

# Analyze a workspace, output JSON, fail on breaking changes
mi-migrate analyze ./workspace --format json --fail-on-breaking

# Target a specific MI version
mi-migrate analyze ./MyProject --target-version 4.3.0
```

**Exit codes**

| Code | Meaning |
|------|---------|
| `0` | Analysis complete, no breaking changes (or `--fail-on-breaking` not set) |
| `1` | Unexpected error |
| `2` | Breaking changes found (only when `--fail-on-breaking` is set) |

---

#### `migrate` — migrate a project

Copies the source to an output directory and converts it to the MI VS Code project format. Runs a pre-migration analysis and prompts for confirmation if issues are found.

```
mi-migrate migrate [options] <source>
```

| Option | Description |
|--------|-------------|
| `--target-version <version>` | Target MI runtime version (default: `4.6.0`) |
| `-o, --output <path>` | Output directory (default: `<source>-migrated`) |
| `-y, --yes` | Non-interactive: skip all confirmation prompts |
| `--java-home <path>` | Override `JAVA_HOME` for Maven invocations |
| `--log-file <path>` | Append all log output to a file (see also [Migration logs](#migration-logs)) |

**Examples**

```bash
# Migrate a single project
mi-migrate migrate ./MyIntegrationProject

# Migrate a workspace to a custom output path
mi-migrate migrate ./workspace --output ./workspace-migrated

# Non-interactive migration targeting a specific version (for CI/CD)
mi-migrate migrate ./MyProject --yes --target-version 4.3.0 --log-file migration.log
```

---

### Migration logs

After a successful `migrate` run, the output directory contains a `.backup/` folder alongside each migrated project with two files:

| File | Contents |
|------|----------|
| `.backup/migration.log` | Timestamped log of everything that happened during migration |
| `.backup/skipped-files-during-migration.txt` | Files that could not be migrated and were left unchanged |

These are always written automatically — `--log-file` is only needed if you want an additional copy of the console output (e.g. for CI pipelines).

---

### Workspace support

Both commands automatically detect whether `<source>` is a single Integration Studio project or an Eclipse workspace containing multiple projects. No flags are needed — the tool adapts accordingly.

### Environment variables

| Variable | Description |
|----------|-------------|
| `JAVA_HOME` | Java installation path (can also be set via `--java-home`) |

> **Development only:** set `MI_MIGRATE_RESOURCES_DIR=/path/to/resources` to override the bundled resources without rebuilding the binary.
