package io.github.leawind.gitparcel.parcel.exception;

/** 格式不支持异常 */
public class UnsupportedFormatException extends StructureStorageException {

  public UnsupportedFormatException(String message) {
    super(message);
  }

  public UnsupportedFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
