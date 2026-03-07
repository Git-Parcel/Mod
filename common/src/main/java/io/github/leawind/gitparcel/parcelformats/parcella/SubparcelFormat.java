package io.github.leawind.gitparcel.parcelformats.parcella;

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
