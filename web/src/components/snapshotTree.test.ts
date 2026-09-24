import { describe, expect, it } from "vitest";
import type { SnapshotNodeDto } from "../api/types";
import { buildSnapshotTree } from "./snapshotTree";

function node(id: string, parentId: string | null): SnapshotNodeDto {
  return {
    id,
    parentId,
    name: id,
    description: "",
    author: "a",
    createdAt: "2026-09-24T00:00:00Z",
    source: "saved",
    content: { files: 1, bytes: 2 },
  };
}

describe("buildSnapshotTree", () => {
  it("groups children under their parents and exposes roots", () => {
    const tree = buildSnapshotTree([
      node("root", null),
      node("child", "root"),
      node("grandchild", "child"),
    ]);
    expect(tree.roots.map((n) => n.id)).toEqual(["root"]);
    expect(tree.childrenOf.get("root")?.map((n) => n.id)).toEqual(["child"]);
    expect(tree.childrenOf.get("child")?.map((n) => n.id)).toEqual([
      "grandchild",
    ]);
  });

  it("surfaces children whose parent is not loaded yet as roots", () => {
    const tree = buildSnapshotTree([node("orphan", "missing-parent")]);
    expect(tree.roots.map((n) => n.id)).toEqual(["orphan"]);
    expect(tree.childrenOf.size).toBe(0);
  });

  it("keeps sibling order as given", () => {
    const tree = buildSnapshotTree([
      node("root", null),
      node("b", "root"),
      node("a", "root"),
    ]);
    expect(tree.childrenOf.get("root")?.map((n) => n.id)).toEqual(["b", "a"]);
  });
});
