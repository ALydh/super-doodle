import { describe, it, expect } from "vitest";
import { sortByRoleOrder } from "./constants";

describe("sortByRoleOrder", () => {
  it("sorts known roles in defined order", () => {
    const input = ["Other", "Characters", "Dedicated Transport", "Battleline"];
    expect(sortByRoleOrder(input)).toEqual([
      "Characters",
      "Battleline",
      "Dedicated Transport",
      "Other",
    ]);
  });

  it("does not mutate the original array", () => {
    const input = ["Other", "Characters"];
    sortByRoleOrder(input);
    expect(input).toEqual(["Other", "Characters"]);
  });

  it("puts unknown roles after known ones, sorted alphabetically", () => {
    const input = ["Zebra", "Characters", "Alpha"];
    expect(sortByRoleOrder(input)).toEqual(["Characters", "Alpha", "Zebra"]);
  });

  it("returns empty array for empty input", () => {
    expect(sortByRoleOrder([])).toEqual([]);
  });

  it("handles all unknown roles by sorting alphabetically", () => {
    const input = ["Zebra", "Alpha", "Middle"];
    expect(sortByRoleOrder(input)).toEqual(["Alpha", "Middle", "Zebra"]);
  });
});
