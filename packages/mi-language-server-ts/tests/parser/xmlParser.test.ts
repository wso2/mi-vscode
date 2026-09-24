import { describe, it, expect } from "vitest";
import { parseXMLDocument } from "../../src/parser/xmlParser.js";

const simpleXML = `<root><child name="test">hello</child></root>`;
const multiLineXML = `<root>\n  <child/>\n</root>`;
const brokenXML = `<root><unclosed>`;

// Namespaced attributes and elements
const namespacedXML = `<root xmlns:ns="http://example.com" ns:attr="value"><ns:child ns:id="1"/></root>`;

// Five levels of nesting — exercises the recursive findDeepest and traverseNode paths
const deeplyNestedXML = `<a><b><c><d><e>deep content</e></d></c></b></a>`;

// Multiple siblings and attributes at each level
const complexXML = `<config version="1.0" env="prod"><database host="localhost" port="5432"><pool min="2" max="10"/><timeout value="30"/></database><cache enabled="true"><store type="redis"/></cache></config>`;

// Unclosed tag buried three levels deep — parser must survive and report an error
const deepBrokenXML = `<root><level1><level2><broken></level2></level1></root>`;

describe("parseXMLDocument return value", () => {
  it("returns a non-null XMLDocument", () => {
    const result = parseXMLDocument("file:///test.xml", simpleXML);
    expect(result).toBeDefined();
    expect(result).not.toBeNull();
  });

  it("preserves the uri passed in", () => {
    const uri = "file:///test.xml";
    expect(parseXMLDocument(uri, simpleXML).uri).toBe(uri);
  });

  it("preserves the source text", () => {
    const result = parseXMLDocument("file:///test.xml", simpleXML);
    expect(result.text).toBe(simpleXML);
  });

  it("has type 'root'", () => {
    expect(parseXMLDocument("file:///test.xml", simpleXML).type).toBe("root");
  });
});

describe("tree structure on simpleXML", () => {
  const doc = parseXMLDocument("file:///test.xml", simpleXML);

  it("has at least one top-level child", () => {
    expect(doc.children.length).toBeGreaterThan(0);
  });

  it("first child is named 'root'", () => {
    expect(doc.children[0].name).toBe("root");
  });

  it("root element has children", () => {
    expect(doc.children[0].children.length).toBeGreaterThan(0);
  });

  it("inner child is named 'child'", () => {
    const inner = doc.children[0].children[0];
    expect(inner.name).toBe("child");
  });

  it("inner child has a 'name' attribute with value 'test'", () => {
    const inner = doc.children[0].children[0];
    const attr = inner.attributes.find((a) => a.name === "name");
    expect(attr).toBeDefined();
    expect(attr?.value).toBe("test");
  });
});

describe("findNodeAt on simpleXML", () => {
  const doc = parseXMLDocument("file:///test.xml", simpleXML);

  it("findNodeAt(0) returns a node (document root or root element)", () => {
    const node = doc.findNodeAt(0);
    expect(node).toBeDefined();
  });

  it("findNodeAt(1) returns a node inside the document", () => {
    const node = doc.findNodeAt(1);
    expect(node).toBeDefined();
    expect(node.type).not.toBe(undefined);
  });

  it("findNodeAt at an offset inside the child tag returns the child node", () => {
    // <root><child ...> starts at offset 6, so offset 7 is inside the child tag
    const childStart = simpleXML.indexOf("<child");
    const node = doc.findNodeAt(childStart + 1);
    expect(node.name).toBe("child");
  });
});

describe("fault tolerance on brokenXML", () => {
  it("does not throw when parsing broken XML", () => {
    expect(() => parseXMLDocument("file:///broken.xml", brokenXML)).not.toThrow();
  });

  it("returns a valid XMLDocument even for broken XML", () => {
    const result = parseXMLDocument("file:///broken.xml", brokenXML);
    expect(result).toBeDefined();
    expect(result.type).toBe("root");
    expect(result.text).toBe(brokenXML);
  });
});

describe("namespaced attributes and elements", () => {
  const doc = parseXMLDocument("file:///ns.xml", namespacedXML);
  const root = doc.children[0];

  it("preserves xmlns:ns attribute name with colon", () => {
    const xmlns = root.attributes.find((a) => a.name === "xmlns:ns");
    expect(xmlns).toBeDefined();
    expect(xmlns?.value).toBe("http://example.com");
  });

  it("preserves ns:attr attribute name with namespace prefix", () => {
    const attr = root.attributes.find((a) => a.name === "ns:attr");
    expect(attr).toBeDefined();
    expect(attr?.value).toBe("value");
  });

  it("preserves namespaced child element name", () => {
    expect(root.children[0].name).toBe("ns:child");
  });

  it("preserves ns:id attribute on namespaced child", () => {
    const id = root.children[0].attributes.find((a) => a.name === "ns:id");
    expect(id).toBeDefined();
    expect(id?.value).toBe("1");
  });

  it("self-closing namespaced child is detected as self-closing", () => {
    expect(root.children[0].isSelfClosing).toBe(true);
  });
});

describe("deeply nested structure and recursion", () => {
  // <a><b><c><d><e>deep content</e></d></c></b></a>
  const doc = parseXMLDocument("file:///deep.xml", deeplyNestedXML);

  it("parses the outermost element as 'a'", () => {
    expect(doc.children[0].name).toBe("a");
  });

  it("builds the full five-level tree via recursive buildNode", () => {
    const a = doc.children[0];
    const b = a.children[0];
    const c = b.children[0];
    const d = c.children[0];
    const e = d.children[0];
    expect(a.name).toBe("a");
    expect(b.name).toBe("b");
    expect(c.name).toBe("c");
    expect(d.name).toBe("d");
    expect(e.name).toBe("e");
  });

  it("parent references are wired correctly up the chain", () => {
    const e = doc.children[0].children[0].children[0].children[0].children[0];
    expect(e.parent?.name).toBe("d");
    expect(e.parent?.parent?.name).toBe("c");
    expect(e.parent?.parent?.parent?.name).toBe("b");
    expect(e.parent?.parent?.parent?.parent?.name).toBe("a");
  });

  it("findNodeAt drills into the deepest element via recursive findDeepest", () => {
    // offset 13 is one char inside the opening <e> tag
    const eStart = deeplyNestedXML.indexOf("<e>");
    const node = doc.findNodeAt(eStart + 1);
    expect(node.name).toBe("e");
  });

  it("findNodeAt returns the document root when offset is out of all children", () => {
    const node = doc.findNodeAt(deeplyNestedXML.length + 10);
    expect(node.type).toBe("root");
  });

  it("traverse visits all five element nodes", () => {
    const names: string[] = [];
    doc.traverse((n) => {
      if (n.name) names.push(n.name);
    });
    expect(names).toEqual(["a", "b", "c", "d", "e"]);
  });
});

describe("complex multi-child structure", () => {
  // <config ...><database ...><pool .../><timeout .../></database><cache ...><store .../></cache></config>
  const doc = parseXMLDocument("file:///complex.xml", complexXML);
  const config = doc.children[0];

  it("root element is 'config' with two attributes", () => {
    expect(config.name).toBe("config");
    expect(config.attributes.length).toBe(2);
    expect(config.attributes.find((a) => a.name === "version")?.value).toBe("1.0");
    expect(config.attributes.find((a) => a.name === "env")?.value).toBe("prod");
  });

  it("config has exactly two direct children: database and cache", () => {
    expect(config.children.length).toBe(2);
    expect(config.children[0].name).toBe("database");
    expect(config.children[1].name).toBe("cache");
  });

  it("database has two self-closing children: pool and timeout", () => {
    const db = config.children[0];
    expect(db.children.length).toBe(2);
    expect(db.children[0].name).toBe("pool");
    expect(db.children[0].isSelfClosing).toBe(true);
    expect(db.children[1].name).toBe("timeout");
    expect(db.children[1].isSelfClosing).toBe(true);
  });

  it("pool has min and max attributes", () => {
    const pool = config.children[0].children[0];
    expect(pool.attributes.find((a) => a.name === "min")?.value).toBe("2");
    expect(pool.attributes.find((a) => a.name === "max")?.value).toBe("10");
  });

  it("traverse visits all six element nodes in depth-first order", () => {
    const names: string[] = [];
    doc.traverse((n) => {
      if (n.name) names.push(n.name);
    });
    expect(names).toEqual(["config", "database", "pool", "timeout", "cache", "store"]);
  });
});

describe("fault tolerance with error buried deep in nesting", () => {
  // <root><level1><level2><broken></level2></level1></root>
  // 'broken' is never closed before its parent closes — should produce a syntax error
  it("does not throw on a deep parse error", () => {
    expect(() => parseXMLDocument("file:///deep-broken.xml", deepBrokenXML)).not.toThrow();
  });

  it("returns a document with syntaxErrors populated", () => {
    const doc = parseXMLDocument("file:///deep-broken.xml", deepBrokenXML);
    expect(doc.syntaxErrors.length).toBeGreaterThan(0);
  });

  it("still produces a tree with root as the top-level element", () => {
    const doc = parseXMLDocument("file:///deep-broken.xml", deepBrokenXML);
    expect(doc.children.length).toBeGreaterThan(0);
    expect(doc.children[0].name).toBe("root");
  });

  it("findNodeAt still works on the valid portion before the error", () => {
    const doc = parseXMLDocument("file:///deep-broken.xml", deepBrokenXML);
    // offset 1 is inside <root> — should resolve to at least the root element
    const node = doc.findNodeAt(1);
    expect(node).toBeDefined();
    expect(node.name).toBe("root");
  });
});
