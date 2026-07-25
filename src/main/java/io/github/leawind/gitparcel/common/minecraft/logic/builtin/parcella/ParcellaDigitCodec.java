package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.utils.numbase.Base32Utils;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;

public enum ParcellaDigitCodec {
  HEX16(HexUtils.UPPERS) {
    @Override
    public int parse(byte value) {
      return HexUtils.parseChar(value);
    }
  },
  BASE32(Base32Utils.CHARS) {
    @Override
    public int parse(byte value) {
      return Base32Utils.parseChar(value);
    }
  };

  private final char[] digits;

  ParcellaDigitCodec(char[] digits) {
    this.digits = digits;
  }

  public char format(int value) {
    if (value < 0 || value >= digits.length) {
      throw new IllegalArgumentException("Digit is out of range: " + value);
    }
    return digits[value];
  }

  public abstract int parse(byte value);
}
