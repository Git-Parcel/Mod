package io.github.leawind.gitparcel.utils.json;

public class JsonAccessException extends Exception {
  public JsonAccessException(String message) {
    super(message);
  }

  public static class IncorrectType extends JsonAccessException {
    public String expectedType;
    public String actualType;

    public IncorrectType(Class<?> expected, String actualType) {
      this(expected.getSimpleName(), actualType);
    }

    public IncorrectType(String expectedType, String actualType) {
      super("Incorrect type. Expected " + expectedType + ", got " + actualType);
      this.expectedType = expectedType;
    }
  }

  public static class MissingProperty extends JsonAccessException {
    public String propertyName;

    public MissingProperty(String propertyName) {
      super("Missing property: " + propertyName);
      this.propertyName = propertyName;
    }
  }
}
