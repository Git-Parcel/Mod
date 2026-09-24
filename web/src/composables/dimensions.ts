import { computed, type ComputedRef, type Ref } from "vue";
import type { SelectOption } from "naive-ui";
import type { ParcelDto } from "../api/types";
import { translateId } from "../i18n";

const KNOWN_DIMENSIONS = [
  "minecraft:overworld",
  "minecraft:the_nether",
  "minecraft:the_end",
];

/** A select option shaped for naive-ui, describing one dimension. */
export type DimensionOption = SelectOption & { value: string };

/**
 * Dimension picker options: every dimension seen on a parcel plus the vanilla
 * three, so a fresh world still offers somewhere to create or import into.
 */
export function useDimensionOptions(
  parcels: Ref<ParcelDto[] | null>,
): ComputedRef<DimensionOption[]> {
  return computed(() => {
    const known = new Set(
      (parcels.value ?? []).map((parcel) => parcel.dimension),
    );
    for (const fallback of KNOWN_DIMENSIONS) {
      known.add(fallback);
    }
    return [...known].map((value) => ({
      label: translateId("dims", value),
      value,
    }));
  });
}

export function mirrorOptions(): DimensionOption[] {
  return ["none", "left_right", "front_back"].map((value) => ({
    label: translateId("mirror", value),
    value,
  }));
}

export function rotationOptions(): DimensionOption[] {
  return ["none", "clockwise_90", "clockwise_180", "counterclockwise_90"].map((
    value,
  ) => ({
    label: translateId("rotation", value),
    value,
  }));
}
