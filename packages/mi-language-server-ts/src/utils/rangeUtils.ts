import { Position, positionToOffset, offsetToPosition } from "./positionUtils.js";

/** Represents a contiguous span of text in a document, compatible with the LSP Range type. */
export interface Range {
  start: Position;
  end: Position;
}

/** Creates a Range from two Position values. */
export function createRange(start: Position, end: Position): Range {
  return { start, end };
}

/**
 * Converts a pair of character offsets into an LSP Range.
 */
export function offsetsToRange(text: string, startOffset: number, endOffset: number): Range {
  return {
    start: offsetToPosition(text, startOffset),
    end: offsetToPosition(text, endOffset),
  };
}

/**
 * Converts an LSP Range back into a pair of character offsets.
 */
export function rangeToOffsets(text: string, range: Range): { start: number; end: number } {
  return {
    start: positionToOffset(text, range.start),
    end: positionToOffset(text, range.end),
  };
}

/**
 * Returns true if the given position falls within the range, inclusive of both endpoints.
 */
export function containsPosition(range: Range, position: Position): boolean {
  if (position.line < range.start.line) return false;
  if (position.line === range.start.line && position.character < range.start.character) return false;
  if (position.line > range.end.line) return false;
  if (position.line === range.end.line && position.character > range.end.character) return false;
  return true;
}

/**
 * Returns true if the given offset falls between startOffset and endOffset, inclusive.
 */
export function containsOffset(startOffset: number, endOffset: number, offset: number): boolean {
  return offset >= startOffset && offset <= endOffset;
}
