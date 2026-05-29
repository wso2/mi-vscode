/** Represents a zero-based line/character position in a text document, compatible with the LSP Position type. */
export interface Position {
  line: number;
  character: number;
}

/**
 * Converts a line/character position to a single character offset in the text.
 * Clamps out-of-bounds inputs to the valid range and never throws.
 */
export function positionToOffset(text: string, position: Position): number {
  const lines = text.split("\n");
  const line = Math.max(0, Math.min(position.line, lines.length - 1));
  const character = Math.max(0, Math.min(position.character, lines[line].length));

  let offset = 0;
  for (let i = 0; i < line; i++) {
    offset += lines[i].length + 1; // +1 for the \n
  }
  offset += character;

  return Math.max(0, Math.min(offset, text.length));
}

/**
 * Converts a character offset back to a line/character position.
 * Clamps out-of-bounds offsets to the valid range and never throws.
 */
export function offsetToPosition(text: string, offset: number): Position {
  const clamped = Math.max(0, Math.min(offset, text.length));

  let line = 0;
  let lineStart = 0;

  for (let i = 0; i < clamped; i++) {
    if (text[i] === "\n") {
      line++;
      lineStart = i + 1;
    }
  }

  return { line, character: clamped - lineStart };
}

/**
 * Returns true if the given position is within the bounds of the text.
 */
export function isValidPosition(text: string, position: Position): boolean {
  const lines = text.split("\n");
  if (position.line < 0 || position.line >= lines.length) return false;
  return position.character >= 0 && position.character <= lines[position.line].length;
}
