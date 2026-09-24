import { describe, it, expect } from "vitest";
import {
  positionToOffset,
  offsetToPosition,
  isValidPosition,
} from "../../src/utils/positionUtils.js";

describe("positionToOffset", () => {
  it("returns 0 for {line:0, character:0} on a single-line text", () => {
    expect(positionToOffset("hello", { line: 0, character: 0 })).toBe(0);
  });

  it("returns 5 for {line:0, character:5} on a single-line text", () => {
    expect(positionToOffset("hello", { line: 0, character: 5 })).toBe(5);
  });

  it("returns 6 for {line:1, character:0} on 'hello\\nworld'", () => {
    expect(positionToOffset("hello\nworld", { line: 1, character: 0 })).toBe(6);
  });

  it("returns 9 for {line:1, character:3} on 'hello\\nworld'", () => {
    expect(positionToOffset("hello\nworld", { line: 1, character: 3 })).toBe(9);
  });

  it("clamps out-of-bounds line without throwing", () => {
    expect(() => positionToOffset("hello", { line: 99, character: 0 })).not.toThrow();
    const result = positionToOffset("hello", { line: 99, character: 0 });
    expect(result).toBeGreaterThanOrEqual(0);
    expect(result).toBeLessThanOrEqual("hello".length);
  });

  it("clamps out-of-bounds character without throwing", () => {
    expect(() => positionToOffset("hello", { line: 0, character: 999 })).not.toThrow();
    const result = positionToOffset("hello", { line: 0, character: 999 });
    expect(result).toBeGreaterThanOrEqual(0);
    expect(result).toBeLessThanOrEqual("hello".length);
  });
});

describe("offsetToPosition", () => {
  it("returns {line:0, character:0} for offset 0", () => {
    expect(offsetToPosition("hello\nworld", 0)).toEqual({ line: 0, character: 0 });
  });

  it("returns {line:0, character:5} for offset 5 on 'hello\\nworld'", () => {
    expect(offsetToPosition("hello\nworld", 5)).toEqual({ line: 0, character: 5 });
  });

  it("returns {line:1, character:0} for offset 6 on 'hello\\nworld'", () => {
    expect(offsetToPosition("hello\nworld", 6)).toEqual({ line: 1, character: 0 });
  });

  it("returns {line:1, character:3} for offset 9 on 'hello\\nworld'", () => {
    expect(offsetToPosition("hello\nworld", 9)).toEqual({ line: 1, character: 3 });
  });

  it("clamps negative offset without throwing", () => {
    expect(() => offsetToPosition("hello", -5)).not.toThrow();
    expect(offsetToPosition("hello", -5)).toEqual({ line: 0, character: 0 });
  });

  it("clamps offset beyond text length without throwing", () => {
    expect(() => offsetToPosition("hello", 999)).not.toThrow();
    const result = offsetToPosition("hello", 999);
    expect(result.line).toBeGreaterThanOrEqual(0);
    expect(result.character).toBeGreaterThanOrEqual(0);
  });
});

describe("isValidPosition", () => {
  it("returns true for a valid position", () => {
    expect(isValidPosition("hello\nworld", { line: 0, character: 3 })).toBe(true);
  });

  it("returns true for a position exactly at line end", () => {
    expect(isValidPosition("hello\nworld", { line: 0, character: 5 })).toBe(true);
  });

  it("returns false when line is out of bounds", () => {
    expect(isValidPosition("hello\nworld", { line: 5, character: 0 })).toBe(false);
  });

  it("returns false when character is out of bounds", () => {
    expect(isValidPosition("hello\nworld", { line: 0, character: 99 })).toBe(false);
  });

  it("returns false for a negative line", () => {
    expect(isValidPosition("hello", { line: -1, character: 0 })).toBe(false);
  });
});
