// @vitest-environment happy-dom
import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import type { Vec3 } from "../api/types";
import CoordInput from "./CoordInput.vue";

function makeInput(value: Vec3) {
  const wrapper = mount(CoordInput, { props: { value } });
  const inputs = wrapper.findAll("input");
  return { wrapper, inputs };
}

describe("CoordInput", () => {
  it("renders one input per axis with the current value", () => {
    const { inputs } = makeInput([1, 2, 3]);
    expect(inputs).toHaveLength(3);
    const values = inputs.map((el) => (el.element as HTMLInputElement).value);
    expect(values).toEqual(["1", "2", "3"]);
  });

  it("emits the updated triple, treating a cleared axis as zero", async () => {
    const wrapper = mount(CoordInput, { props: { value: [7, 8, 9] } });
    const inputs = wrapper.findAll("input");
    const middle = inputs[1];
    const setter = Object.getOwnPropertyDescriptor(
      HTMLInputElement.prototype,
      "value",
    )!.set!;
    setter.call(middle.element, "");
    await middle.setValue("");
    const emitted = wrapper.emitted("update:value");
    expect(emitted).toHaveLength(1);
    expect(emitted![0][0]).toEqual([7, 0, 9]);
  });
});
