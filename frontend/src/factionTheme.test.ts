import { describe, it, expect } from "vitest";
import { getFactionTheme } from "./factionTheme";

describe("getFactionTheme", () => {
  it("returns undefined for null input", () => {
    expect(getFactionTheme(null)).toBeUndefined();
  });

  it("returns undefined for undefined input", () => {
    expect(getFactionTheme(undefined)).toBeUndefined();
  });

  it("returns undefined for empty string", () => {
    expect(getFactionTheme("")).toBeUndefined();
  });

  it("returns undefined for unknown faction", () => {
    expect(getFactionTheme("xyzzy")).toBeUndefined();
  });

  it("maps direct faction IDs", () => {
    expect(getFactionTheme("sm")).toBe("space-marines");
    expect(getFactionTheme("orks")).toBe("orks");
    expect(getFactionTheme("necrons")).toBe("necrons");
    expect(getFactionTheme("tau")).toBe("tau");
  });

  it("is case-insensitive", () => {
    expect(getFactionTheme("SM")).toBe("space-marines");
    expect(getFactionTheme("Orks")).toBe("orks");
    expect(getFactionTheme("NECRONS")).toBe("necrons");
  });

  it("maps alternative names to the same theme", () => {
    expect(getFactionTheme("eldar")).toBe("aeldari");
    expect(getFactionTheme("craftworlds")).toBe("aeldari");
    expect(getFactionTheme("aeldari")).toBe("aeldari");
  });

  it("maps faction abbreviations", () => {
    expect(getFactionTheme("csm")).toBe("chaos");
    expect(getFactionTheme("gsc")).toBe("genestealer-cults");
    expect(getFactionTheme("dg")).toBe("death-guard");
  });

  it("falls back to substring matching", () => {
    expect(getFactionTheme("some-chaos-variant")).toBe("chaos");
  });
});
