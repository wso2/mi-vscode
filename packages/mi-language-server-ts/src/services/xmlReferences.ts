import { XMLDocument } from "../parser/xmlNode.js";
import { Position, positionToOffset } from "../utils/positionUtils.js";
import { Range, offsetsToRange } from "../utils/rangeUtils.js";

/** A location in the document that references an element with a given tag name. */
export interface ReferenceResult {
  uri: string;
  range: Range;
}

/**
 * Returns all locations in the document where an element with the same tag name as the
 * element under the cursor appears, including the cursor's own element.
 * Returns an empty array when the cursor is not on a named element node.
 */
export function findReferences(
  document: XMLDocument,
  position: Position
): ReferenceResult[] {
  const offset = positionToOffset(document.text, position);
  const node = document.findNodeAt(offset);

  if (!node || node.type !== "element" || !node.name) return [];

  const targetName = node.name;
  const results: ReferenceResult[] = [];

  document.traverse((n) => {
    if (n.type === "element" && n.name === targetName) {
      results.push({
        uri: document.uri,
        range: offsetsToRange(document.text, n.startOffset, n.endOffset),
      });
    }
  });

  return results;
}
