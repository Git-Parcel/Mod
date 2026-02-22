package io.github.leawind.gitparcel.parcel.exception;

/** 结构加载异常 */
public class LoadException extends StructureStorageException {

  public LoadException(String message) {
    super(message);
  }

  public LoadException(String message, Throwable cause) {
    super(message, cause);
  }

  public LoadException(Throwable cause) {
    super(cause);
  }
}
