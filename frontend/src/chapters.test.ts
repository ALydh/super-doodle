import { describe, it, expect } from "vitest";
import { getChapterTheme, isSpaceMarines } from "./chapters";

describe("getChapterTheme", () => {
  it("returns space-marines for null input", () => {
    expect(getChapterTheme(null)).toBe("space-marines");
  });

  it("returns the correct theme for known chapters", () => {
    expect(getChapterTheme("ultramarines")).toBe("sm-ultramarines");
    expect(getChapterTheme("blood-angels")).toBe("sm-blood-angels");
    expect(getChapterTheme("dark-angels")).toBe("sm-dark-angels");
  });

  it("falls back to space-marines for unknown chapter", () => {
    expect(getChapterTheme("nonexistent-chapter")).toBe("space-marines");
  });
});

describe("isSpaceMarines", () => {
  it("returns true for SM faction", () => {
    expect(isSpaceMarines("SM")).toBe(true);
  });

  it("returns false for other factions", () => {
    expect(isSpaceMarines("CSM")).toBe(false);
    expect(isSpaceMarines("orks")).toBe(false);
    expect(isSpaceMarines("sm")).toBe(false);
  });
});
