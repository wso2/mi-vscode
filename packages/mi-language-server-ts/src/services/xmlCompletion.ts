import { XMLDocument } from "../parser/xmlNode.js";
import { Position, positionToOffset } from "../utils/positionUtils.js";

/** A single completion suggestion returned to the editor. */
export interface CompletionItem {
  label: string;
  kind: "element" | "attribute" | "value" | "closeTag";
  insertText: string;
  detail?: string;
}

/** A list of completion suggestions for a given cursor position. */
export interface CompletionList {
  items: CompletionItem[];
  isIncomplete: boolean;
}

/**
 * Returns completion suggestions for the given cursor position by detecting whether
 * the cursor is after '<', '</', or inside an attribute list. Uses schema data when
 * available, falling back to document-tree scanning.
 */
export function doComplete(
  document: XMLDocument,
  position: Position,
  schemaProvider?: any
): CompletionList {
  const offset = positionToOffset(document.text, position);
  const textBefore = document.text.substring(0, offset);

  // Context: cursor is right after '</'
  if (/<\/$/.test(textBefore)) {
    const node = document.findNodeAt(offset);
    if (node && node.type === "element" && node.name) {
      return {
        items: [{ label: node.name, kind: "closeTag", insertText: `${node.name}>` }],
        isIncomplete: false,
      };
    }
    return { items: [], isIncomplete: false };
  }

  // Context: cursor is right after '<' or '<' + partial tag name (no space yet)
  if (/<\w*$/.test(textBefore)) {
    if (schemaProvider?.hasData()) {
      const node = document.findNodeAt(offset);

      // Determine whether the cursor is still inside this node's open tag (e.g. '<foo|')
      // or in its content area (e.g. '<foo>\n  <|').  When inside the open tag we want
      // to suggest SIBLINGS (children of the parent); when in the content area we want
      // to suggest CHILDREN of the current node.
      let parentName: string | undefined;
      if (node.type === "element") {
        const nodeText = document.text.substring(node.startOffset, offset);
        const cursorIsInOpenTag = !nodeText.includes(">");
        if (cursorIsInOpenTag) {
          parentName =
            node.parent?.type === "element" ? node.parent.name : undefined;
        } else {
          parentName = node.name;
        }
      }

      let names: string[];
      if (parentName === undefined) {
        // Root-level context: prefer children of the top-level container element
        // (e.g. <definitions> in Synapse config) so only config-artifact elements
        // are offered rather than every globally-declared element including mediators.
        names = schemaProvider.getChildren("definitions");
        if (names.length === 0) names = schemaProvider.getAllElements();
      } else {
        // Inside a known element: suggest only its schema-defined children.
        // No getAllElements() fallback — unknown or childless elements get nothing.
        names = schemaProvider.getChildren(parentName);
      }

      return {
        items: names.map((name: string) => ({
          label: name,
          kind: "element" as const,
          insertText: `${name}>$0</${name}>`,
        })),
        isIncomplete: false,
      };
    }

    // Fallback: scan document tree for known element names
    const names = new Set<string>();
    document.traverse((n) => {
      if (n.type === "element" && n.name) names.add(n.name);
    });
    return {
      items: Array.from(names).map((name) => ({
        label: name,
        kind: "element" as const,
        insertText: `${name}>$0</${name}>`,
      })),
      isIncomplete: false,
    };
  }

  // Context: cursor is inside an open tag (attribute position).
  // Use lastIndexOf('<') so this fires even after completed attribute values like
  // '<log level="full" |' where the old \w+\s+\w*$ regex would miss the quote boundary.
  const lastOpenAngle = textBefore.lastIndexOf("<");
  if (lastOpenAngle !== -1) {
    const fragment = textBefore.slice(lastOpenAngle);
    // Confirm: not a close tag, and the `<` hasn't been closed yet.
    if (!fragment.startsWith("</") && !fragment.includes(">")) {
      const tagMatch = /^<([\w:]+)/.exec(fragment);
      if (tagMatch && fragment.length > tagMatch[0].length) {
        const afterTagName = fragment.slice(tagMatch[0].length);
        // Only enter attribute context when there is at least one space after the tag name.
        if (/^\s/.test(afterTagName)) {
          const tagName = tagMatch[1];

          if (schemaProvider?.hasData()) {
            const schemaAttrs: any[] = schemaProvider.getAttributes(tagName) ?? [];
            const schemaAttrNames = new Set<string>(schemaAttrs.map((a: any) => a.name));

            const docAttrs = new Set<string>();
            document.traverse((n) => {
              if (n.type === "element" && n.name === tagName) {
                for (const a of n.attributes) docAttrs.add(a.name);
              }
            });
            const extraDocAttrs = Array.from(docAttrs).filter(
              (a) => !schemaAttrNames.has(a)
            );

            return {
              items: [
                ...schemaAttrs.map((attr: any) => ({
                  label: attr.name,
                  kind: "attribute" as const,
                  insertText: `${attr.name}="$0"`,
                  ...(attr.type ? { detail: attr.type } : {}),
                })),
                ...extraDocAttrs.map((attr) => ({
                  label: attr,
                  kind: "attribute" as const,
                  insertText: `${attr}="$0"`,
                })),
              ],
              isIncomplete: false,
            };
          }

          // Fallback: static common XML attributes
          const attrs = ["xml:lang", "xml:space", "xmlns"];
          return {
            items: attrs.map((attr) => ({
              label: attr,
              kind: "attribute" as const,
              insertText: `${attr}="$0"`,
            })),
            isIncomplete: false,
          };
        }
      }
    }
  }

  return { items: [], isIncomplete: false };
}
