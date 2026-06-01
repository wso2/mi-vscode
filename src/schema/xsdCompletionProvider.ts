import { parse } from "@xml-tools/parser";

export interface AttributeInfo {
  name: string;
  description: string; // from xs:documentation
  type: string;        // xs:string, xs:boolean, etc.
  required: boolean;   // use="required"
}

export interface ElementInfo {
  name: string;
  description: string;     // from xs:documentation
  attributes: AttributeInfo[];
  children: string[];      // valid child element names
}

export interface SchemaCompletionData {
  elements: Map<string, ElementInfo>; // elementName → ElementInfo
}

export class XsdCompletionProvider {
  private data: SchemaCompletionData;

  constructor(xsdText: string) {
    this.data = { elements: new Map() };
    const { cst } = parse(xsdText);
    this.buildData(cst);
  }

  private buildData(cst: any): void {
    const data = this.data;
    // Maps complexType name → direct child element names, for type-ref resolution.
    const complexTypeChildren = new Map<string, string[]>();
    // Maps complexType name → attribute list, for type-ref attribute resolution.
    const complexTypeAttributes = new Map<string, AttributeInfo[]>();
    // Maps element name → type attribute value, for post-walk resolution.
    const elementTypeRefs = new Map<string, string>();
    // Maps attributeGroup name → { direct attrs, referenced group names }.
    const rawGroups = new Map<string, { attrs: AttributeInfo[]; refs: string[] }>();
    // Maps xs:group name → { direct element names, referenced group names }.
    const rawElementGroups = new Map<string, { elements: string[]; refs: string[] }>();

    function getAttrValue(node: any, attrName: string): string {
      for (const attr of node.children?.attribute ?? []) {
        if (attr.children?.Name?.[0]?.image === attrName) {
          const raw: string = attr.children?.STRING?.[0]?.image ?? "";
          return raw.replace(/^["']|["']$/g, "");
        }
      }
      return "";
    }

    function getTextContent(node: any): string {
      const parts: string[] = [];
      for (const content of node.children?.content ?? []) {
        for (const chardata of content.children?.chardata ?? []) {
          const text: string = chardata.children?.TEXT?.[0]?.image ?? "";
          if (text) parts.push(text);
        }
      }
      return parts.join(" ").replace(/\s+/g, " ").trim();
    }

    function getTagName(node: any): string {
      return node.children?.Name?.[0]?.image ?? "";
    }

    function isXsdTag(tagName: string, localName: string): boolean {
      return tagName === localName || tagName.endsWith(":" + localName);
    }

    function findDocumentation(node: any): string {
      for (const content of node.children?.content ?? []) {
        for (const child of content.children?.element ?? []) {
          const childTag = getTagName(child);
          if (isXsdTag(childTag, "annotation")) {
            for (const annContent of child.children?.content ?? []) {
              for (const docNode of annContent.children?.element ?? []) {
                if (isXsdTag(getTagName(docNode), "documentation")) {
                  return getTextContent(docNode);
                }
              }
            }
          }
        }
      }
      return "";
    }

    // Collects the names of xs:element children reachable through
    // xs:sequence / xs:all / xs:choice / xs:group inside `node`.
    function collectDirectElementNames(node: any): string[] {
      const names: string[] = [];
      function collect(n: any): void {
        const tag = getTagName(n);
        if (isXsdTag(tag, "element")) {
          const childName = getAttrValue(n, "name") || getAttrValue(n, "ref");
          if (childName && !names.includes(childName)) names.push(childName);
        } else if (isXsdTag(tag, "group")) {
          const ref = getAttrValue(n, "ref");
          if (ref) {
            for (const name of expandElementGroup(ref)) {
              if (!names.includes(name)) names.push(name);
            }
          }
        } else if (
          isXsdTag(tag, "sequence") ||
          isXsdTag(tag, "all") ||
          isXsdTag(tag, "choice")
        ) {
          for (const content of n.children?.content ?? []) {
            for (const child of content.children?.element ?? []) {
              collect(child);
            }
          }
        }
      }
      for (const content of node.children?.content ?? []) {
        for (const child of content.children?.element ?? []) {
          collect(child);
        }
      }
      return names;
    }

    // Pre-pass: collect a single top-level attributeGroup definition.
    function collectAttributeGroup(node: any): void {
      const tag = getTagName(node);
      if (!isXsdTag(tag, "attributeGroup")) return;
      const name = getAttrValue(node, "name");
      if (!name) return;
      const attrs: AttributeInfo[] = [];
      const refs: string[] = [];
      for (const c of node.children?.content ?? []) {
        for (const cc of c.children?.element ?? []) {
          const cTag = getTagName(cc);
          if (isXsdTag(cTag, "attribute")) {
            const attrName = getAttrValue(cc, "name");
            if (attrName) {
              attrs.push({
                name: attrName,
                description: findDocumentation(cc),
                type: getAttrValue(cc, "type"),
                required: getAttrValue(cc, "use") === "required",
              });
            }
          } else if (isXsdTag(cTag, "attributeGroup")) {
            const ref = getAttrValue(cc, "ref");
            if (ref) refs.push(ref);
          }
        }
      }
      rawGroups.set(name, { attrs, refs });
    }

    // Collects all xs:attribute and xs:attributeGroup ref members declared directly
    // inside a named complexType (used for xs:element type="X" attribute resolution).
    function collectComplexTypeAttributes(node: any): AttributeInfo[] {
      const attrs: AttributeInfo[] = [];
      function collect(n: any): void {
        const tag = getTagName(n);
        if (isXsdTag(tag, "element")) return; // don't cross into nested element content
        if (isXsdTag(tag, "attribute")) {
          const attrName = getAttrValue(n, "name");
          if (attrName && !attrs.find((a) => a.name === attrName)) {
            attrs.push({
              name: attrName,
              description: findDocumentation(n),
              type: getAttrValue(n, "type"),
              required: getAttrValue(n, "use") === "required",
            });
          }
        } else if (isXsdTag(tag, "attributeGroup")) {
          const ref = getAttrValue(n, "ref");
          if (ref) {
            for (const attr of expandGroup(ref)) {
              if (!attrs.find((a) => a.name === attr.name)) attrs.push(attr);
            }
          }
        } else {
          for (const c of n.children?.content ?? []) {
            for (const cc of c.children?.element ?? []) collect(cc);
          }
        }
      }
      for (const c of node.children?.content ?? []) {
        for (const cc of c.children?.element ?? []) collect(cc);
      }
      return attrs;
    }

    // Recursively expands an attributeGroup by name, resolving nested refs.
    function expandGroup(groupName: string, visited = new Set<string>()): AttributeInfo[] {
      if (visited.has(groupName)) return [];
      visited.add(groupName);
      const raw = rawGroups.get(groupName);
      if (!raw) return [];
      const result: AttributeInfo[] = [...raw.attrs];
      for (const ref of raw.refs) {
        result.push(...expandGroup(ref, new Set(visited)));
      }
      return result;
    }

    // Pre-pass: collect a single top-level xs:group definition (element groups).
    function collectElementGroup(node: any): void {
      const tag = getTagName(node);
      if (!isXsdTag(tag, "group")) return;
      const name = getAttrValue(node, "name");
      if (!name) return;
      const elements: string[] = [];
      const refs: string[] = [];
      function collect(n: any): void {
        const t = getTagName(n);
        if (isXsdTag(t, "element")) {
          const eName = getAttrValue(n, "name") || getAttrValue(n, "ref");
          if (eName && !elements.includes(eName)) elements.push(eName);
          // Don't recurse into element bodies to avoid pulling in nested group refs.
        } else if (isXsdTag(t, "group")) {
          const ref = getAttrValue(n, "ref");
          if (ref && !refs.includes(ref)) refs.push(ref);
        } else {
          for (const c of n.children?.content ?? []) {
            for (const cc of c.children?.element ?? []) collect(cc);
          }
        }
      }
      for (const c of node.children?.content ?? []) {
        for (const cc of c.children?.element ?? []) collect(cc);
      }
      rawElementGroups.set(name, { elements, refs });
    }

    // Recursively expands an xs:group by name, resolving nested group refs.
    function expandElementGroup(groupName: string, visited = new Set<string>()): string[] {
      if (visited.has(groupName)) return [];
      visited.add(groupName);
      const raw = rawElementGroups.get(groupName);
      if (!raw) return [];
      const result: string[] = [...raw.elements];
      for (const ref of raw.refs) {
        for (const name of expandElementGroup(ref, new Set(visited))) {
          if (!result.includes(name)) result.push(name);
        }
      }
      return result;
    }

    function walk(node: any, parentElementName: string | null): void {
      const tagName = getTagName(node);

      if (isXsdTag(tagName, "element")) {
        const name = getAttrValue(node, "name");
        const ref  = getAttrValue(node, "ref");

        // ref-only element: wire it to the parent without declaring a new element.
        if (!name && ref) {
          if (parentElementName !== null) {
            const parent = data.elements.get(parentElementName);
            if (parent && !parent.children.includes(ref)) {
              parent.children.push(ref);
            }
          }
          return;
        }

        if (!name) {
          recurseChildren(node, parentElementName);
          return;
        }

        // Multi-occurrence handling.  An element name can appear more than once in
        // the merged schema: e.g. xquery.xsd declares a LOCAL <xs:element name="variable">
        // unrelated to the global variable mediator.  We treat the FIRST occurrence
        // that actually has a body (inline complexType / attributes) as authoritative;
        // subsequent occurrences must NOT recurse into their bodies, or their attributes
        // would leak onto the originally-registered element.
        // A bare stub (e.g. <xs:element name="child"/> used as an inline child reference)
        // does NOT count as the authoritative occurrence — a later global declaration
        // with a body is allowed to populate the empty entry.
        const existing = data.elements.get(name);
        const existingIsPopulated =
          !!existing && (existing.attributes.length > 0 || existing.children.length > 0);
        if (!existing) {
          data.elements.set(name, {
            name,
            description: findDocumentation(node),
            attributes: [],
            children: [],
          });
        }

        if (parentElementName !== null) {
          const parent = data.elements.get(parentElementName);
          if (parent && !parent.children.includes(name)) {
            parent.children.push(name);
          }
        }

        // Record any type reference for post-walk resolution.  This must run for
        // populated duplicates too: e.g. a local <xs:element name="resource" type="APIResource">
        // inside <api> is the authoritative declaration even though a registry-style
        // <xs:element name="resource"> was registered first from common.xsd.
        const typeRef = getAttrValue(node, "type");
        if (typeRef) elementTypeRefs.set(name, typeRef);

        // Skip recursing into duplicates whose original is already populated.
        if (existingIsPopulated) return;

        recurseChildren(node, name);
      } else if (isXsdTag(tagName, "attribute")) {
        const name = getAttrValue(node, "name");
        if (name && parentElementName !== null) {
          const parent = data.elements.get(parentElementName);
          if (parent && !parent.attributes.find((a) => a.name === name)) {
            parent.attributes.push({
              name,
              description: findDocumentation(node),
              type: getAttrValue(node, "type"),
              required: getAttrValue(node, "use") === "required",
            });
          }
        }
        recurseChildren(node, parentElementName);
      } else if (isXsdTag(tagName, "attributeGroup")) {
        // Named attributeGroup declarations are handled in the pre-pass.
        // Here we only handle ref="..." inside an element's complexType.
        const ref = getAttrValue(node, "ref");
        if (ref && parentElementName !== null) {
          const parent = data.elements.get(parentElementName);
          if (parent) {
            for (const attr of expandGroup(ref)) {
              if (!parent.attributes.find((a) => a.name === attr.name)) {
                parent.attributes.push(attr);
              }
            }
          }
        }
      } else if (isXsdTag(tagName, "complexType")) {
        // Top-level named complexType: snapshot children and attributes so that
        // xs:element type="X" declarations can be resolved after the walk.
        const typeName = getAttrValue(node, "name");
        if (typeName && parentElementName === null) {
          complexTypeChildren.set(typeName, collectDirectElementNames(node));
          complexTypeAttributes.set(typeName, collectComplexTypeAttributes(node));
        }
        // Always pass the current parent element name through.
        recurseChildren(node, parentElementName);
      } else if (isXsdTag(tagName, "group")) {
        const ref = getAttrValue(node, "ref");
        if (ref) {
          // Reference: expand the group and wire element names to the parent.
          if (parentElementName !== null) {
            const parent = data.elements.get(parentElementName);
            if (parent) {
              for (const name of expandElementGroup(ref)) {
                if (!parent.children.includes(name)) parent.children.push(name);
              }
            }
          }
          // No body to recurse into for a reference node.
        } else {
          // Named definition: recurse so that inline xs:element declarations inside
          // the group (e.g. the inline "sequence" in mediatorList) are registered in
          // data.elements with their attributes.
          recurseChildren(node, parentElementName);
        }
      } else if (
        isXsdTag(tagName, "sequence") ||
        isXsdTag(tagName, "all") ||
        isXsdTag(tagName, "choice")
      ) {
        // Pass the current parent element name down into group containers.
        recurseChildren(node, parentElementName);
      } else {
        recurseChildren(node, parentElementName);
      }
    }

    function recurseChildren(node: any, parentElementName: string | null): void {
      for (const content of node.children?.content ?? []) {
        for (const child of content.children?.element ?? []) {
          walk(child, parentElementName);
        }
      }
    }

    // Pre-pass: collect all top-level attributeGroup and xs:group definitions so that
    // ref="..." usages can be expanded during the main walk.
    for (const rootEl of cst.children?.element ?? []) {
      for (const content of rootEl.children?.content ?? []) {
        for (const child of content.children?.element ?? []) {
          collectAttributeGroup(child);
          collectElementGroup(child);
        }
      }
    }

    // Main walk: enter from document root → schema element → top-level XSD declarations.
    for (const rootEl of cst.children?.element ?? []) {
      for (const content of rootEl.children?.content ?? []) {
        for (const child of content.children?.element ?? []) {
          walk(child, null);
        }
      }
    }

    // Resolve xs:element type="X" references to xs:complexType name="X".
    // Children from the complexType are merged in; attributes from the complexType
    // replace any inline attributes collected during the walk (a type reference is
    // authoritative for what attributes the element accepts).
    for (const [elementName, typeRef] of elementTypeRefs) {
      const localType = typeRef.includes(":") ? typeRef.split(":").pop()! : typeRef;
      const element = data.elements.get(elementName);
      if (!element) continue;

      const children = complexTypeChildren.get(localType);
      if (children) {
        for (const childName of children) {
          if (!element.children.includes(childName)) {
            element.children.push(childName);
          }
        }
      }

      const ctAttrs = complexTypeAttributes.get(localType);
      if (ctAttrs && ctAttrs.length > 0) {
        element.attributes = ctAttrs;
      }
    }
  }

  /** Returns the ElementInfo for the given element name, or undefined if not found. */
  getElement(name: string): ElementInfo | undefined {
    return this.data.elements.get(name);
  }

  /** Returns all known element names extracted from the schema. */
  getAllElements(): string[] {
    return Array.from(this.data.elements.keys());
  }

  /** Returns valid child element names for the given parent element. */
  getChildren(elementName: string): string[] {
    return this.data.elements.get(elementName)?.children ?? [];
  }

  /** Returns attribute definitions for the given element. */
  getAttributes(elementName: string): AttributeInfo[] {
    return this.data.elements.get(elementName)?.attributes ?? [];
  }

  /** Returns the xs:documentation description for the given element, or empty string. */
  getDescription(elementName: string): string {
    return this.data.elements.get(elementName)?.description ?? "";
  }

  /** Returns true if at least one element was extracted from the schema. */
  hasData(): boolean {
    return this.data.elements.size > 0;
  }
}
