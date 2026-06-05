package io.github.leawind.gitparcel.core.utils;

public class TypeCastUtils {
  @SuppressWarnings("unchecked")
  public static <T, U> T blindCast(U value) {
    return (T) value;
  }

  @SuppressWarnings("unchecked")
  public static <T extends U, U> T upcast(U value) {
    return (T) value;
  }
}
