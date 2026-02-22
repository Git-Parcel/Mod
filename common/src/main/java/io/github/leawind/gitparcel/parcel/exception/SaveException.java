package io.github.leawind.gitparcel.parcel.exception;

/** 结构保存异常 */
public class SaveException extends StructureStorageException {

  public SaveException(String message) {
    super(message);
  }

  public SaveException(String message, Throwable cause) {
    super(message, cause);
  }

  public SaveException(Throwable cause) {
    super(cause);
  }
}
