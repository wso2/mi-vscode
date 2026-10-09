import { describe, it, expect } from "vitest";
import { XsdCompletionProvider } from "../../src/schema/xsdCompletionProvider.js";

const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://test.com">
  <xs:element name="root">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="child"/>
        <xs:element name="item"/>
      </xs:sequence>
      <xs:attribute name="id" type="xs:string" use="required"/>
      <xs:attribute name="class" type="xs:string"/>
    </xs:complexType>
  </xs:element>
  <xs:element name="child">
    <xs:complexType>
      <xs:attribute name="name" type="xs:string"/>
    </xs:complexType>
  </xs:element>
</xs:schema>`;

describe("XsdCompletionProvider", () => {
  const provider = new XsdCompletionProvider(xsd);

  it("creates without throwing", () => {
    expect(() => new XsdCompletionProvider(xsd)).not.toThrow();
  });

  it("hasData() returns true", () => {
    expect(provider.hasData()).toBe(true);
  });

  it("getAllElements() includes 'root' and 'child'", () => {
    const elements = provider.getAllElements();
    expect(elements).toContain("root");
    expect(elements).toContain("child");
  });

  it("getChildren('root') includes 'child' and 'item'", () => {
    const children = provider.getChildren("root");
    expect(children).toContain("child");
    expect(children).toContain("item");
  });

  it("getAttributes('root') contains an attribute named 'id'", () => {
    const attrs = provider.getAttributes("root");
    const idAttr = attrs.find((a) => a.name === "id");
    expect(idAttr).toBeDefined();
  });

  it("getAttributes('root') — 'id' attribute has required: true", () => {
    const attrs = provider.getAttributes("root");
    const idAttr = attrs.find((a) => a.name === "id");
    expect(idAttr?.required).toBe(true);
  });

  it("getAttributes('root') — 'class' attribute has required: false", () => {
    const attrs = provider.getAttributes("root");
    const classAttr = attrs.find((a) => a.name === "class");
    expect(classAttr).toBeDefined();
    expect(classAttr?.required).toBe(false);
  });

  it("getAttributes('child') contains a 'name' attribute", () => {
    const attrs = provider.getAttributes("child");
    const nameAttr = attrs.find((a) => a.name === "name");
    expect(nameAttr).toBeDefined();
  });

  it("getElement('unknown') returns undefined", () => {
    expect(provider.getElement("unknown")).toBeUndefined();
  });

  it("getChildren('unknown') returns []", () => {
    expect(provider.getChildren("unknown")).toEqual([]);
  });
});

// -------------------------------------------------------------------
// Walk-function correctness: patterns from the fix strategy
// -------------------------------------------------------------------

describe("XsdCompletionProvider — deep nesting (inline complexTypes)", () => {
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="project">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="groupId"/>
        <xs:element name="dependencies">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="dependency">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="artifactId"/>
                    <xs:element name="version"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("project children include groupId and dependencies", () => {
    expect(p.getChildren("project")).toContain("groupId");
    expect(p.getChildren("project")).toContain("dependencies");
  });

  it("dependencies children include dependency", () => {
    expect(p.getChildren("dependencies")).toContain("dependency");
  });

  it("dependency children include artifactId and version", () => {
    expect(p.getChildren("dependency")).toContain("artifactId");
    expect(p.getChildren("dependency")).toContain("version");
  });

  it("groupId is not a child of dependencies or dependency", () => {
    expect(p.getChildren("dependencies")).not.toContain("groupId");
    expect(p.getChildren("dependency")).not.toContain("groupId");
  });
});

describe("XsdCompletionProvider — named complexType with type references", () => {
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="project" type="ProjectType"/>
  <xs:complexType name="ProjectType">
    <xs:sequence>
      <xs:element name="groupId"/>
      <xs:element name="dependencies" type="DependenciesType"/>
    </xs:sequence>
  </xs:complexType>
  <xs:complexType name="DependenciesType">
    <xs:sequence>
      <xs:element ref="dependency"/>
    </xs:sequence>
  </xs:complexType>
  <xs:element name="dependency">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="artifactId"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("project children resolved via type reference", () => {
    expect(p.getChildren("project")).toContain("groupId");
    expect(p.getChildren("project")).toContain("dependencies");
  });

  it("dependencies children resolved via ref attribute", () => {
    expect(p.getChildren("dependencies")).toContain("dependency");
  });

  it("dependency children resolved correctly", () => {
    expect(p.getChildren("dependency")).toContain("artifactId");
  });
});

describe("XsdCompletionProvider — xs:attributeGroup ref expansion", () => {
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:attributeGroup name="valueOrExpression">
    <xs:attribute name="value" type="xs:string" use="optional"/>
    <xs:attribute name="expression" type="xs:string" use="optional"/>
  </xs:attributeGroup>
  <xs:attributeGroup name="nameValueOrExpression">
    <xs:attribute name="name" type="xs:string" use="required"/>
    <xs:attribute name="action" type="xs:string" use="optional"/>
    <xs:attributeGroup ref="valueOrExpression"/>
  </xs:attributeGroup>
  <xs:element name="variable">
    <xs:complexType>
      <xs:attributeGroup ref="nameValueOrExpression"/>
      <xs:attribute name="type" type="xs:string" use="optional"/>
      <xs:attribute name="description" type="xs:string" use="optional"/>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("variable has 'name' from attributeGroup (required)", () => {
    const attr = p.getAttributes("variable").find((a) => a.name === "name");
    expect(attr).toBeDefined();
    expect(attr?.required).toBe(true);
  });

  it("variable has 'action' from attributeGroup", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "action")).toBeDefined();
  });

  it("variable has 'value' from nested attributeGroup ref", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "value")).toBeDefined();
  });

  it("variable has 'expression' from nested attributeGroup ref", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "expression")).toBeDefined();
  });

  it("variable has its own 'type' and 'description' attributes", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "type")).toBeDefined();
    expect(p.getAttributes("variable").find((a) => a.name === "description")).toBeDefined();
  });

  it("variable does not have a 'key' attribute", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "key")).toBeUndefined();
  });
});

describe("XsdCompletionProvider — child element attributes do not leak to parent", () => {
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="payloadFactory">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="format">
          <xs:complexType mixed="true">
            <xs:attribute name="key" type="xs:string" use="optional"/>
          </xs:complexType>
        </xs:element>
        <xs:element name="args">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="arg">
                <xs:complexType mixed="true">
                  <xs:attribute name="value" type="xs:string" use="optional"/>
                  <xs:attribute name="evaluator" type="xs:string" use="optional"/>
                  <xs:attribute name="expression" type="xs:string" use="optional"/>
                  <xs:attribute name="literal" type="xs:boolean" use="optional"/>
                </xs:complexType>
              </xs:element>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
      </xs:sequence>
      <xs:attribute name="media-type" use="required">
        <xs:simpleType>
          <xs:restriction base="xs:string">
            <xs:enumeration value="xml"/>
            <xs:enumeration value="json"/>
            <xs:enumeration value="text"/>
          </xs:restriction>
        </xs:simpleType>
      </xs:attribute>
      <xs:attribute name="template-type" type="xs:string" use="optional"/>
      <xs:attribute name="description" type="xs:string" use="optional"/>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("payloadFactory has only its own attributes: media-type, template-type, description", () => {
    const names = p.getAttributes("payloadFactory").map((a) => a.name);
    expect(names).toContain("media-type");
    expect(names).toContain("template-type");
    expect(names).toContain("description");
  });

  it("payloadFactory does NOT have 'key' (belongs to <format>)", () => {
    expect(p.getAttributes("payloadFactory").find((a) => a.name === "key")).toBeUndefined();
  });

  it("payloadFactory does NOT have 'value' (belongs to <arg>)", () => {
    expect(p.getAttributes("payloadFactory").find((a) => a.name === "value")).toBeUndefined();
  });

  it("payloadFactory does NOT have 'evaluator' (belongs to <arg>)", () => {
    expect(p.getAttributes("payloadFactory").find((a) => a.name === "evaluator")).toBeUndefined();
  });

  it("payloadFactory does NOT have 'expression' (belongs to <arg>)", () => {
    expect(p.getAttributes("payloadFactory").find((a) => a.name === "expression")).toBeUndefined();
  });

  it("payloadFactory does NOT have 'literal' (belongs to <arg>)", () => {
    expect(p.getAttributes("payloadFactory").find((a) => a.name === "literal")).toBeUndefined();
  });

  it("payloadFactory children include 'format' and 'args'", () => {
    expect(p.getChildren("payloadFactory")).toContain("format");
    expect(p.getChildren("payloadFactory")).toContain("args");
  });

  it("format has 'key' attribute", () => {
    expect(p.getAttributes("format").find((a) => a.name === "key")).toBeDefined();
  });

  it("arg has 'value', 'evaluator', 'expression', 'literal' attributes", () => {
    const names = p.getAttributes("arg").map((a) => a.name);
    expect(names).toContain("value");
    expect(names).toContain("evaluator");
    expect(names).toContain("expression");
    expect(names).toContain("literal");
  });
});

describe("XsdCompletionProvider — named complexType attribute resolution via type ref", () => {
  // Mirrors the api.xsd + resource.xsd pattern:
  // - global registry 'resource' (location, key) is registered first
  // - 'api' declares an inline 'resource' with type="APIResource"
  // - APIResource complexType has methods (required), protocol, plus attributeGroup
  //   ref="templateOrMapping" → uri-template, url-mapping
  // After the walk the 'resource' attributes must reflect APIResource, not the registry definition.
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <!-- registry resource (must appear first to simulate common.xsd ordering) -->
  <xs:element name="resource">
    <xs:complexType>
      <xs:attribute name="location" type="xs:anyURI" use="required"/>
      <xs:attribute name="key" type="xs:string" use="required"/>
    </xs:complexType>
  </xs:element>

  <!-- attributeGroup used by APIResource -->
  <xs:attributeGroup name="templateOrMapping">
    <xs:attribute name="uri-template" type="xs:string" use="optional"/>
    <xs:attribute name="url-mapping" type="xs:string" use="optional"/>
  </xs:attributeGroup>

  <!-- named complexType for the API resource -->
  <xs:complexType name="APIResource">
    <xs:all>
      <xs:element name="inSequence" minOccurs="0"/>
      <xs:element name="outSequence" minOccurs="0"/>
    </xs:all>
    <xs:attribute name="methods" use="required"/>
    <xs:attribute name="protocol" use="optional"/>
    <xs:attributeGroup ref="templateOrMapping"/>
  </xs:complexType>

  <!-- api element whose inline 'resource' references APIResource -->
  <xs:element name="api">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="resource" type="APIResource" minOccurs="1" maxOccurs="unbounded"/>
      </xs:sequence>
      <xs:attribute name="name" type="xs:string" use="required"/>
      <xs:attribute name="context" type="xs:string" use="required"/>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("resource has 'methods' (from APIResource complexType)", () => {
    const attr = p.getAttributes("resource").find((a) => a.name === "methods");
    expect(attr).toBeDefined();
    expect(attr?.required).toBe(true);
  });

  it("resource has 'protocol' (from APIResource complexType)", () => {
    expect(p.getAttributes("resource").find((a) => a.name === "protocol")).toBeDefined();
  });

  it("resource has 'uri-template' (from templateOrMapping attributeGroup inside APIResource)", () => {
    expect(p.getAttributes("resource").find((a) => a.name === "uri-template")).toBeDefined();
  });

  it("resource has 'url-mapping' (from templateOrMapping attributeGroup inside APIResource)", () => {
    expect(p.getAttributes("resource").find((a) => a.name === "url-mapping")).toBeDefined();
  });

  it("resource does NOT have 'location' (belongs to registry resource, replaced by APIResource)", () => {
    expect(p.getAttributes("resource").find((a) => a.name === "location")).toBeUndefined();
  });

  it("resource does NOT have 'key' (belongs to registry resource, replaced by APIResource)", () => {
    expect(p.getAttributes("resource").find((a) => a.name === "key")).toBeUndefined();
  });

  it("resource children include inSequence and outSequence (from APIResource xs:all)", () => {
    expect(p.getChildren("resource")).toContain("inSequence");
    expect(p.getChildren("resource")).toContain("outSequence");
  });

  it("api has its own 'name' and 'context' attributes", () => {
    const names = p.getAttributes("api").map((a) => a.name);
    expect(names).toContain("name");
    expect(names).toContain("context");
  });
});

describe("XsdCompletionProvider — duplicate element name does not pollute the original", () => {
  // Reproduces the real-world bug where xquery.xsd declares a LOCAL
  // <xs:element name="variable"> inside its content model with attributes
  // (name, type, key, ...).  The mediator <variable> must not pick up 'key'
  // from this unrelated local element.
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:attributeGroup name="nameValueOrExpression">
    <xs:attribute name="name" type="xs:string" use="required"/>
    <xs:attribute name="action" type="xs:string" use="optional"/>
    <xs:attribute name="value" type="xs:string" use="optional"/>
    <xs:attribute name="expression" type="xs:string" use="optional"/>
  </xs:attributeGroup>

  <!-- the real variable mediator (no key) -->
  <xs:element name="variable">
    <xs:complexType>
      <xs:attributeGroup ref="nameValueOrExpression"/>
      <xs:attribute name="type" type="xs:string" use="optional"/>
      <xs:attribute name="description" type="xs:string" use="optional"/>
    </xs:complexType>
  </xs:element>

  <!-- xquery mediator with an unrelated local element ALSO named 'variable',
       carrying a 'key' attribute that must NOT leak into the global variable -->
  <xs:element name="xquery">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="variable" minOccurs="0" maxOccurs="unbounded">
          <xs:complexType>
            <xs:attribute name="name" type="xs:string" use="required"/>
            <xs:attribute name="type" type="xs:string" use="required"/>
            <xs:attribute name="key" type="xs:string" use="optional"/>
            <xs:attribute name="value" type="xs:string" use="optional"/>
          </xs:complexType>
        </xs:element>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("variable has its mediator attributes only", () => {
    const names = p.getAttributes("variable").map((a) => a.name).sort();
    expect(names).toEqual(["action", "description", "expression", "name", "type", "value"]);
  });

  it("variable does NOT have 'key' (would leak from xquery's local <variable>)", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "key")).toBeUndefined();
  });
});

describe("XsdCompletionProvider — variable has no key in Synapse-like merged schema", () => {
  // Simulates the merged schema produced when variable.xsd is loaded alongside
  // property.xsd (which shares nameValueOrExpression) and the sequence inline element
  // (which legitimately has 'key').  variable must not pick up 'key'.
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:attributeGroup name="valueOrExpression">
    <xs:attribute name="value" type="xs:string" use="optional"/>
    <xs:attribute name="expression" type="xs:string" use="optional"/>
  </xs:attributeGroup>
  <xs:attributeGroup name="nameValueOrExpression">
    <xs:attribute name="name" type="xs:string" use="required"/>
    <xs:attribute name="action" type="xs:string" use="optional"/>
    <xs:attributeGroup ref="valueOrExpression"/>
  </xs:attributeGroup>

  <!-- variable mediator (no key) -->
  <xs:element name="variable">
    <xs:complexType>
      <xs:attributeGroup ref="nameValueOrExpression"/>
      <xs:attribute name="type" type="xs:string" use="optional"/>
      <xs:attribute name="description" type="xs:string" use="optional"/>
    </xs:complexType>
  </xs:element>

  <!-- sequence inline element inside a group — has 'key', must not leak to variable -->
  <xs:group name="mediatorList">
    <xs:choice>
      <xs:element ref="variable"/>
      <xs:element name="sequence">
        <xs:complexType>
          <xs:attribute name="key" type="xs:string" use="optional"/>
          <xs:attribute name="name" type="xs:string" use="optional"/>
        </xs:complexType>
      </xs:element>
    </xs:choice>
  </xs:group>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("variable has 'name', 'action', 'value', 'expression', 'type', 'description'", () => {
    const names = p.getAttributes("variable").map((a) => a.name);
    expect(names).toContain("name");
    expect(names).toContain("action");
    expect(names).toContain("value");
    expect(names).toContain("expression");
    expect(names).toContain("type");
    expect(names).toContain("description");
  });

  it("variable does NOT have 'key' (belongs to the sequence inline element)", () => {
    expect(p.getAttributes("variable").find((a) => a.name === "key")).toBeUndefined();
  });

  it("sequence has 'key' and 'name'", () => {
    const names = p.getAttributes("sequence").map((a) => a.name);
    expect(names).toContain("key");
    expect(names).toContain("name");
  });
});

describe("XsdCompletionProvider — xs:choice inside xs:sequence", () => {
  const xsd = `<?xml version="1.0"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="root">
    <xs:complexType>
      <xs:sequence>
        <xs:choice>
          <xs:element name="option1"/>
          <xs:element name="option2"/>
        </xs:choice>
        <xs:element name="always"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>`;
  const p = new XsdCompletionProvider(xsd);

  it("root children include both choice options and the unconditional element", () => {
    expect(p.getChildren("root")).toContain("option1");
    expect(p.getChildren("root")).toContain("option2");
    expect(p.getChildren("root")).toContain("always");
  });
});
