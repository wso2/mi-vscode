import { describe, it, expect } from "vitest";
import {
  offsetsToRange,
  rangeToOffsets,
  containsPosition,
  containsOffset,
} from "../../src/utils/rangeUtils.js";

const text = "hello\nworld";

describe("offsetsToRange", () => {
  it("converts offsets 0 and 4 to range {line:0,char:0} → {line:0,char:4}", () => {
    expect(offsetsToRange(text, 0, 4)).toEqual({
      start: { line: 0, character: 0 },
      end: { line: 0, character: 4 },
    });
  });

  it("converts offsets 6 and 11 to range {line:1,char:0} → {line:1,char:5}", () => {
    expect(offsetsToRange(text, 6, 11)).toEqual({
      start: { line: 1, character: 0 },
      end: { line: 1, character: 5 },
    });
  });
});

describe("rangeToOffsets", () => {
  it("round-trips: offsetsToRange then rangeToOffsets returns original offsets", () => {
    const start = 6;
    const end = 11;
    const range = offsetsToRange(text, start, end);
    expect(rangeToOffsets(text, range)).toEqual({ start, end });
  });

  it("round-trips for first-line offsets", () => {
    const range = offsetsToRange(text, 0, 4);
    expect(rangeToOffsets(text, range)).toEqual({ start: 0, end: 4 });
  });
});

describe("containsPosition", () => {
  const range = {
    start: { line: 1, character: 2 },
    end: { line: 1, character: 8 },
  };

  it("returns true for a position inside the range", () => {
    expect(containsPosition(range, { line: 1, character: 5 })).toBe(true);
  });

  it("returns false for a position before the range start", () => {
    expect(containsPosition(range, { line: 0, character: 0 })).toBe(false);
  });

  it("returns false for a position after the range end", () => {
    expect(containsPosition(range, { line: 2, character: 0 })).toBe(false);
  });

  it("returns true for a position exactly on the start", () => {
    expect(containsPosition(range, { line: 1, character: 2 })).toBe(true);
  });

  it("returns true for a position exactly on the end", () => {
    expect(containsPosition(range, { line: 1, character: 8 })).toBe(true);
  });

  it("returns false for same line but character before start", () => {
    expect(containsPosition(range, { line: 1, character: 1 })).toBe(false);
  });

  it("returns false for same line but character after end", () => {
    expect(containsPosition(range, { line: 1, character: 9 })).toBe(false);
  });
});

describe("containsOffset", () => {
  it("returns true for an offset inside the range", () => {
    expect(containsOffset(5, 10, 7)).toBe(true);
  });

  it("returns false for an offset before the range", () => {
    expect(containsOffset(5, 10, 3)).toBe(false);
  });

  it("returns false for an offset after the range", () => {
    expect(containsOffset(5, 10, 12)).toBe(false);
  });

  it("returns true for an offset exactly on the start boundary", () => {
    expect(containsOffset(5, 10, 5)).toBe(true);
  });

  it("returns true for an offset exactly on the end boundary", () => {
    expect(containsOffset(5, 10, 10)).toBe(true);
  });
});
