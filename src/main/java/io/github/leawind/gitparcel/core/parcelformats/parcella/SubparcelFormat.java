package io.github.leawind.gitparcel.core.parcelformats.parcella;

public enum SubparcelFormat {
  /**
   * Flat format
   *
   * <p>Encode block coord to index in a 1D array with x-y-z order
   */
  FLAT,
  /** Run-length encoding 3D */
  RLE3D,
}
