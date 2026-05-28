import { describe, expect, it } from "vitest";
import * as path from "path";
import { TextDocument } from "vscode-languageserver-textdocument";
import { DiagnosticsHandler } from "../src/diagnosticsHandler.js";
import {
  createConnection,
  createService,
  expectSchemaResolved,
  readFixture,
  validateSynapseFixtureXml,
} from "./helpers/diagnosticsTestUtils.js";

describe("DiagnosticsHandler Synapse 4.3.0 schema fixture", () => {
  it("walks the real Synapse 4.3.0 schema include graph", () => {
    const schemaPath = path.join(process.cwd(), "tests/resources/schemas/430/synapse_config.xsd");
    const schemaText = readFixture("tests/resources/schemas/430/synapse_config.xsd");
    const { connection } = createConnection();
    const handler = new DiagnosticsHandler(connection as any, {} as any) as any;

    const imports = handler.loadReferencedXsds(schemaPath, schemaText);
    const keys = Object.keys(imports).sort();

    expect(keys).toEqual(expect.arrayContaining([
      "api.xsd",
      "proxy.xsd",
      "endpoint.xsd",
      "inbound.xsd",
      "local_entry.xsd",
      "messagestore.xsd",
      "messageprocessor.xsd",
      "sequence.xsd",
      "task.xsd",
      "template.xsd",
      "registry.xsd",
      "misc/common.xsd",
      "misc/resource.xsd",
      "mediators/mediators.xsd",
      "mediators/core/call.xsd",
      "mediators/core/property.xsd",
      "mediators/filter/throttle.xsd",
      "mediators/advanced/cache.xsd",
      "mediators/transformation/xslt.xsd",
      "mediators/other/rule.xsd",
      "misc/wsdl11.xsd",
      "misc/wsdl20.xsd",
      "misc/ws-policy.xsd",
      "misc/throttle_policy.xsd",
      "misc/oasis-200401-wss-wssecurity-secext-1.0.xsd",
      "misc/oasis-200401-wss-wssecurity-utility-1.0.xsd",
      "misc/xmldsig-core-schema.xsd",
      "misc/xsd/xml.xsd",
      "misc/xsd/XMLSchema.dtd",
      "misc/xsd/datatypes.dtd",
    ]));
    expect(keys).not.toContain("catalog.xml");
    expect(keys).not.toContain("misc/xsd/datatypes.xml");
    expect(keys.length).toBeGreaterThan(50);
  });

  it("registers the real Synapse 4.3.0 include graph during validation", async () => {
    const schemaPath = path.join(process.cwd(), "tests/resources/schemas/430/synapse_config.xsd");
    const schemaText = readFixture("tests/resources/schemas/430/synapse_config.xsd");
    const registered: any[] = [];
    const { connection, sentDiagnostics } = createConnection();
    const service = createService(schemaPath, schemaText, registered);
    const handler = new DiagnosticsHandler(connection as any, service as any);
    const document = TextDocument.create("file:///workspace/synapse.xml", "xml", 1, "<definitions/>");

    await handler.validateAndSend(document);

    expect(registered).toHaveLength(1);
    expect(registered[0].xsdText).toBe(schemaText);
    expect(Object.keys(registered[0].imports).sort()).toEqual(expect.arrayContaining([
      "api.xsd",
      "sequence.xsd",
      "mediators/mediators.xsd",
      "mediators/core/log.xsd",
      "mediators/filter/throttle.xsd",
      "misc/ws-policy.xsd",
      "misc/xsd/XMLSchema.dtd",
      "misc/xsd/datatypes.dtd",
    ]));
    expect(registered[0].imports["catalog.xml"]).toBeUndefined();
    expect(sentDiagnostics).toEqual([{ uri: "file:///workspace/synapse.xml", diagnostics: [] }]);
  });

  describe("real XML cases by included schema", () => {
    it.each([
      {
        source: "api.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <api name="api1" context="/api1">
              <resource methods="GET" uri-template="/items"/>
            </api>
          </definitions>
        `,
      },
      {
        source: "proxy.xsd + misc/target.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <proxy name="proxy1" transports="http">
              <target inSequence="main"/>
            </proxy>
          </definitions>
        `,
      },
      {
        source: "endpoint.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <endpoint name="ep1">
              <address uri="http://example.com/service"/>
            </endpoint>
          </definitions>
        `,
      },
      {
        source: "inbound.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <inboundEndpoint name="in1" protocol="http" suspend="false"/>
          </definitions>
        `,
      },
      {
        source: "local_entry.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <localEntry key="entry1">value</localEntry>
          </definitions>
        `,
      },
      {
        source: "messagestore.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <messageStore name="store1" class="org.example.Store"/>
          </definitions>
        `,
      },
      {
        source: "messageprocessor.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <messageProcessor name="processor1" class="org.example.Processor" messageStore="store1"/>
          </definitions>
        `,
      },
      {
        source: "sequence.xsd + mediators/core/log.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <sequence name="main">
              <log level="full"/>
            </sequence>
          </definitions>
        `,
      },
      {
        source: "task.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <task class="org.example.Task" name="task1" group="group1">
              <trigger interval="1000"/>
            </task>
          </definitions>
        `,
      },
      {
        source: "template.xsd + mediators/core/drop.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <template name="template1">
              <sequence>
                <drop/>
              </sequence>
            </template>
          </definitions>
        `,
      },
      {
        source: "registry.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <registry provider="org.example.Registry"/>
          </definitions>
        `,
      },
      {
        source: "mediators/filter/throttle.xsd + misc/ws-policy.xsd",
        xml: `
          <definitions xmlns="http://ws.apache.org/ns/synapse">
            <sequence name="throttleSeq">
              <throttle id="throttle1">
                <policy>
                  <wsp:Policy xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy"/>
                </policy>
              </throttle>
            </sequence>
          </definitions>
        `,
      },
    ])("resolves schema declarations for XML using $source", async ({ xml }) => {
      const { diagnostics, warnings } = await validateSynapseFixtureXml(xml);

      expectSchemaResolved(diagnostics, warnings);
      expect(diagnostics).toEqual([]);
    });
  });
});
