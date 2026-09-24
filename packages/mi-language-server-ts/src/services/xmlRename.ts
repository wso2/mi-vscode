import { XMLDocument } from "../parser/xmlNode.js";
import { Position, positionToOffset } from "../utils/positionUtils.js";
import { TextEdit } from "./xmlFormatter.js";

/**
 * Returns TextEdits that rename both the opening and closing tags of the element at the
 * given position, or null if the position is not on a named element or the new name is invalid.
 */
export function doRename(
  document: XMLDocument,
  position: Position,
  newName: string
): TextEdit[] | null {
  if (!newName || /[\s<>]/.test(newName)) return null;

  const offset = positionToOffset(document.text, position);
  const node = document.findNodeAt(offset);

  if (!node || node.type !== "element" || !node.name) return null;

  const edits: TextEdit[] = [];

  // Opening tag: skip the leading '<'
  const openNameStart = node.startOffset + 1;
  const openNameEnd = openNameStart + node.name.length;
  edits.push({ startOffset: openNameStart, endOffset: openNameEnd, newText: newName });

  if (!node.isSelfClosing) {
    // Search backwards from the node end for the last '</' + name
    const closePattern = "</" + node.name;
    const searchText = document.text.substring(0, node.endOffset);
    const closeIdx = searchText.lastIndexOf(closePattern);
    if (closeIdx !== -1) {
      const closeNameStart = closeIdx + 2; // skip '</'
      const closeNameEnd = closeNameStart + node.name.length;
      edits.push({ startOffset: closeNameStart, endOffset: closeNameEnd, newText: newName });
    }
  }

  return edits;
}
