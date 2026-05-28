import { describe, it, expect } from "vitest";
import * as path from "path";
import { applySchemaSettings } from "../src/configuration.js";
import { createConnection, makeTempDir, writeFile } from "./helpers/diagnosticsTestUtils.js";

function createService() {
  const registered: { pattern: string; xsdPath: string }[] = [];
  return {
    registered,
    service: {
      addUserAssociation: (assoc: { pattern: string; xsdPath: string }) => {
        registered.push(assoc);
      },
    } as any,
  };
}

// ── Single root ───────────────────────────────────────────────────────────────

describe("applySchemaSettings — single root", () => {
  it("resolves a relative xsdPath against the workspace root and registers it", () => {
    const root = makeTempDir();
    writeFile(path.join(root, "schemas", "main.xsd"), "<xs:schema/>");

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "schemas/main.xsd" }],
      connection,
      service,
      [root]
    );

    expect(registered).toHaveLength(1);
    expect(registered[0].xsdPath).toBe(path.join(root, "schemas", "main.xsd"));
    expect(registered[0].pattern).toBe("**/*.xml");
    expect(warnings).toHaveLength(0);
  });

  it("warns and skips when the relative xsdPath does not exist", () => {
    const root = makeTempDir();

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "schemas/missing.xsd" }],
      connection,
      service,
      [root]
    );

    expect(registered).toHaveLength(0);
    expect(warnings.some((w) => w.includes("missing.xsd"))).toBe(true);
  });

  it("uses an absolute xsdPath directly without joining any root", () => {
    const root = makeTempDir();
    const absolutePath = path.join(root, "absolute.xsd");
    writeFile(absolutePath, "<xs:schema/>");

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: absolutePath }],
      connection,
      service,
      []                            // no roots — absolute path should not need them
    );

    expect(registered).toHaveLength(1);
    expect(registered[0].xsdPath).toBe(absolutePath);
    expect(warnings).toHaveLength(0);
  });

  it("warns and skips when an absolute xsdPath does not exist on disk", () => {
    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "/nonexistent/path/schema.xsd" }],
      connection,
      service,
      []
    );

    expect(registered).toHaveLength(0);
    expect(warnings.some((w) => w.includes("schema.xsd"))).toBe(true);
  });

  it("warns and skips when no workspace roots are provided and path is relative", () => {
    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "schemas/main.xsd" }],
      connection,
      service,
      []
    );

    expect(registered).toHaveLength(0);
    expect(warnings.some((w) => w.includes("main.xsd"))).toBe(true);
  });
});

// ── Multi-root ────────────────────────────────────────────────────────────────

describe("applySchemaSettings — multi-root workspace", () => {
  it("resolves a relative xsdPath from the first root when it exists there", () => {
    const root1 = makeTempDir();
    const root2 = makeTempDir();
    writeFile(path.join(root1, "schemas", "api.xsd"), "<xs:schema/>");

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "schemas/api.xsd" }],
      connection,
      service,
      [root1, root2]
    );

    expect(registered).toHaveLength(1);
    expect(registered[0].xsdPath).toBe(path.join(root1, "schemas", "api.xsd"));
    expect(warnings).toHaveLength(0);
  });

  it("falls back to the second root when xsdPath is not in the first root", () => {
    const root1 = makeTempDir();
    const root2 = makeTempDir();
    // XSD only exists in root2, not root1
    writeFile(path.join(root2, "schemas", "api.xsd"), "<xs:schema/>");

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "schemas/api.xsd" }],
      connection,
      service,
      [root1, root2]
    );

    expect(registered).toHaveLength(1);
    expect(registered[0].xsdPath).toBe(path.join(root2, "schemas", "api.xsd"));
    expect(warnings).toHaveLength(0);
  });

  it("registers each schema against the correct root when schemas live in different roots", () => {
    const root1 = makeTempDir();
    const root2 = makeTempDir();
    writeFile(path.join(root1, "schemas", "430.xsd"), "<xs:schema/>");
    writeFile(path.join(root2, "schemas", "440.xsd"), "<xs:schema/>");

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [
        { pattern: "wso2-430-project/**/*.xml", xsdPath: "schemas/430.xsd" },
        { pattern: "wso2-440-project/**/*.xml", xsdPath: "schemas/440.xsd" },
      ],
      connection,
      service,
      [root1, root2]
    );

    expect(registered).toHaveLength(2);
    expect(registered[0].xsdPath).toBe(path.join(root1, "schemas", "430.xsd"));
    expect(registered[0].pattern).toBe("wso2-430-project/**/*.xml");
    expect(registered[1].xsdPath).toBe(path.join(root2, "schemas", "440.xsd"));
    expect(registered[1].pattern).toBe("wso2-440-project/**/*.xml");
    expect(warnings).toHaveLength(0);
  });

  it("warns and skips when xsdPath is not found in any workspace root", () => {
    const root1 = makeTempDir();
    const root2 = makeTempDir();

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [{ pattern: "**/*.xml", xsdPath: "schemas/nowhere.xsd" }],
      connection,
      service,
      [root1, root2]
    );

    expect(registered).toHaveLength(0);
    expect(warnings.some((w) => w.includes("nowhere.xsd"))).toBe(true);
  });

  it("registers valid schemas and skips invalid ones in the same call", () => {
    const root1 = makeTempDir();
    const root2 = makeTempDir();
    writeFile(path.join(root1, "schemas", "good.xsd"), "<xs:schema/>");

    const { connection, warnings } = createConnection();
    const { service, registered } = createService();

    applySchemaSettings(
      [
        { pattern: "good/**/*.xml", xsdPath: "schemas/good.xsd" },
        { pattern: "bad/**/*.xml",  xsdPath: "schemas/missing.xsd" },
      ],
      connection,
      service,
      [root1, root2]
    );

    expect(registered).toHaveLength(1);
    expect(registered[0].pattern).toBe("good/**/*.xml");
    expect(warnings.some((w) => w.includes("missing.xsd"))).toBe(true);
  });
});
