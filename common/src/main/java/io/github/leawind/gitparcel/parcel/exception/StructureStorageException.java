package io.github.leawind.gitparcel.parcel.exception;

/** 结构存储相关的异常基类 */
public class StructureStorageException extends Exception {

  public StructureStorageException(String message) {
    super(message);
  }

  public StructureStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public StructureStorageException(Throwable cause) {
    super(cause);
  }
}
