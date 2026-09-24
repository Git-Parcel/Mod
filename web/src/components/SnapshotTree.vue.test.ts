// @vitest-environment happy-dom
import { describe, expect, it } from "vitest";
import { defineComponent, h } from "vue";
import { mount } from "@vue/test-utils";
import naive, { NMessageProvider } from "naive-ui";
import type { SnapshotNodeDto } from "../api/types";
import { i18n } from "../i18n";
import SnapshotTree from "./SnapshotTree.vue";

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

function makeTree(current = "grandchild") {
  const nodes = [
    node("root", null),
    node("child", "root"),
    node("grandchild", "child"),
    node("branch", "root"),
  ];
  const emitted: SnapshotNodeDto[] = [];
  // The message provider is required by CopyText inside the tree rows.
  const Harness = defineComponent({
    setup() {
      return () =>
        h(NMessageProvider, () =>
          h(SnapshotTree, {
            nodes,
            current,
            onRestore: (node: SnapshotNodeDto) => emitted.push(node),
          }));
    },
  });
  const wrapper = mount(Harness, { global: { plugins: [i18n, naive] } });
  return { wrapper, emitted };
}

describe("SnapshotTree", () => {
  it("renders one row per node above the collapse threshold", () => {
    const { wrapper } = makeTree();
    expect(wrapper.findAll(".node-row")).toHaveLength(4);
  });

  it("shows the current baseline summary line", () => {
    const { wrapper } = makeTree();
    const line = wrapper.find(".current-line");
    expect(line.exists()).toBe(true);
    expect(line.text()).toContain("grandch");
  });

  it("marks the current node and omits its restore button", () => {
    const { wrapper } = makeTree();
    const currentRow = wrapper.find("#snapshot-node-grandchild");
    expect(currentRow.classes()).toContain("current");
    expect(currentRow.find(".restore-button").exists()).toBe(false);
  });

  it("collapses and re-expands a branch from its caret", async () => {
    const { wrapper } = makeTree();
    const rootCaret = wrapper.findAll(".caret")[0];
    await rootCaret.trigger("click");
    expect(wrapper.findAll(".node-row")).toHaveLength(1);
    await rootCaret.trigger("click");
    expect(wrapper.findAll(".node-row")).toHaveLength(4);
  });

  it("re-emits restore for a node", async () => {
    const { wrapper, emitted } = makeTree();
    const buttons = wrapper.findAll(".restore-button");
    await buttons[0].trigger("click");
    expect(emitted).toHaveLength(1);
    expect(emitted[0].id).toBe("root");
  });
});

describe("SnapshotTree large histories", () => {
  it("keeps a history at exactly the threshold fully expanded", () => {
    const nodes: SnapshotNodeDto[] = [node("n0", null)];
    for (let i = 1; i < 300; i++) {
      nodes.push(node(`n${i}`, `n${i - 1}`));
    }
    const emitted: SnapshotNodeDto[] = [];
    const Harness = defineComponent({
      setup() {
        return () =>
          h(NMessageProvider, () =>
            h(SnapshotTree, {
              nodes,
              current: "n299",
              onRestore: (node: SnapshotNodeDto) => emitted.push(node),
            }));
      },
    });
    const wrapper = mount(Harness, { global: { plugins: [i18n, naive] } });
    expect(wrapper.findAll(".node-row")).toHaveLength(300);
  });

  it("starts fully collapsed above the threshold", () => {
    // Build a 400-node linear chain: every parent has children, so without
    // the default collapse this would render 400 rows.
    const nodes: SnapshotNodeDto[] = [node("n0", null)];
    for (let i = 1; i < 400; i++) {
      nodes.push(node(`n${i}`, `n${i - 1}`));
    }
    const emitted: SnapshotNodeDto[] = [];
    const Harness = defineComponent({
      setup() {
        return () =>
          h(NMessageProvider, () =>
            h(SnapshotTree, {
              nodes,
              current: "n399",
              onRestore: (node: SnapshotNodeDto) => emitted.push(node),
            }));
      },
    });
    const wrapper = mount(Harness, { global: { plugins: [i18n, naive] } });

    // Only the chain root renders; the rest waits behind "load more"/expansion.
    expect(wrapper.findAll(".node-row").length).toBeLessThan(10);
    // The current baseline stays visible through the summary line.
    expect(wrapper.find(".current-line").text()).toContain("n399");
  });
});
