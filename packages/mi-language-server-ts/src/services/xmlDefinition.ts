import { XMLDocument } from "../parser/xmlNode.js";
import { Position, positionToOffset } from "../utils/positionUtils.js";
import { Range, offsetsToRange } from "../utils/rangeUtils.js";

/** The URI and range of a definition target, compatible with the LSP Location type. */
export interface DefinitionResult {
  uri: string;
  range: Range;
}

/**
 * Returns a DefinitionResult pointing to the matching open or close tag for the element
 * under the cursor, enabling tag-pair navigation. Returns null for self-closing elements
 * or when the cursor is not on a named element.
 */
export function doDefinition(
  document: XMLDocument,
  position: Position
): DefinitionResult | null {
  const offset = positionToOffset(document.text, position);
  const node = document.findNodeAt(offset);

  if (!node || node.type !== "element" || !node.name || node.isSelfClosing) return null;

  // If cursor is within the opening tag span, navigate to the closing tag
  const openTagEnd = node.startOffset + node.name.length + 1;
  const isOnOpenTag = offset <= openTagEnd;

  if (isOnOpenTag) {
    const closePattern = "</" + node.name;
    const closeIdx = document.text.indexOf(closePattern, node.startOffset + 1);
    if (closeIdx === -1) return null;
    return {
      uri: document.uri,
      range: offsetsToRange(document.text, closeIdx, closeIdx + closePattern.length + 1),
    };
  }

  // Cursor is on the closing tag — navigate back to the opening tag
  return {
    uri: document.uri,
    range: offsetsToRange(document.text, node.startOffset, node.startOffset + node.name.length + 1),
  };
}
