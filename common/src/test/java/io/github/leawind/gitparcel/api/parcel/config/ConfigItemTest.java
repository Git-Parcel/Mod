package io.github.leawind.gitparcel.api.parcel.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConfigItemTest {

  @Test
  @DisplayName("Simple factory methods should not throw")
  void testSimple() {
    ConfigItemBuilder.ofBoolean("").build();
    ConfigItemBuilder.ofDouble("").build();
    ConfigItemBuilder.ofLong("").build();
    ConfigItemBuilder.ofString("").build();

    ConfigItemBuilder.ofEnum("", TestEnum.DEFAULT).build();
  }

  @Test
  @DisplayName("Boolean config item with default true")
  void testBooleanConfigItem() {
    ConfigItem<Boolean> enabled =
        ConfigItemBuilder.ofBoolean("enabled")
            .defaultValue(true)
            .description("Whether the feature is enabled")
            .build();

    assertEquals("enabled", enabled.name());
    assertEquals(true, enabled.defaultValue());
    assertEquals("Whether the feature is enabled", enabled.description());
    assertEquals(true, enabled.get());
    assertTrue(enabled.userVisible());
  }

  @Test
  @DisplayName("Boolean config item with default false")
  void testBooleanConfigItemDefaultFalse() {
    ConfigItem<Boolean> disabled = ConfigItemBuilder.ofBoolean("disabled").build();

    assertEquals("disabled", disabled.name());
    assertEquals(false, disabled.defaultValue());
    assertEquals(false, disabled.get());
    assertNull(disabled.description());
  }

  @Test
  @DisplayName("Boolean config item set and get")
  void testBooleanSetAndGet() {
    ConfigItem<Boolean> flag = ConfigItemBuilder.ofBoolean("flag").build();

    assertEquals(false, flag.get());
    flag.set(true);
    assertEquals(true, flag.get());
  }

  @Test
  @DisplayName("Long config item with range validation")
  void testLongConfigItem() {
    ConfigItem<Long> maxItems =
        ConfigItemBuilder.ofLong("maxItems").defaultValue(100L).range(1, 1000).build();

    assertEquals("maxItems", maxItems.name());
    assertEquals(100L, maxItems.defaultValue());
    assertEquals(100L, maxItems.get());
    assertNull(maxItems.validate(500L));
    assertNotNull(maxItems.validate(0L));
    assertNotNull(maxItems.validate(2000L));
  }

  @Test
  @DisplayName("Long config item default value is 0")
  void testLongConfigItemDefaultZero() {
    ConfigItem<Long> count = ConfigItemBuilder.ofLong("count").build();

    assertEquals(0L, count.defaultValue());
    assertEquals(0L, count.get());
  }

  @Test
  @DisplayName("Long config item range boundary values")
  void testLongConfigItemRangeBoundaries() {
    ConfigItem<Long> value =
        ConfigItemBuilder.ofLong("value").defaultValue(5L).range(1, 10).build();

    // Boundary values should be valid
    assertNull(value.validate(1L));
    assertNull(value.validate(10L));

    // Out of boundary values should be invalid
    assertNotNull(value.validate(0L));
    assertNotNull(value.validate(11L));
  }

  @Test
  @DisplayName("Long config item validation error message")
  void testLongConfigItemValidationMessage() {
    ConfigItem<Long> value =
        ConfigItemBuilder.ofLong("value").defaultValue(5L).range(1, 10).build();

    String errorMsg = value.validate(100L);
    assertNotNull(errorMsg);
    assertTrue(errorMsg.contains("100"));
    assertTrue(errorMsg.contains("min: 1"));
    assertTrue(errorMsg.contains("max: 10"));
  }

  @Test
  @DisplayName("Double config item with range validation")
  void testDoubleConfigItem() {
    ConfigItem<Double> threshold =
        ConfigItemBuilder.ofDouble("threshold").defaultValue(0.5).range(0.0, 1.0).build();

    assertEquals("threshold", threshold.name());
    assertEquals(0.5, threshold.defaultValue());
    assertNull(threshold.validate(0.75));
    assertNotNull(threshold.validate(-0.1));
    assertNotNull(threshold.validate(1.5));
  }

  @Test
  @DisplayName("Double config item default value is 0.0")
  void testDoubleConfigItemDefaultZero() {
    ConfigItem<Double> value = ConfigItemBuilder.ofDouble("value").build();

    assertEquals(0.0, value.defaultValue());
    assertEquals(0.0, value.get());
  }

  @Test
  @DisplayName("Double config item range boundary values")
  void testDoubleConfigItemRangeBoundaries() {
    ConfigItem<Double> value =
        ConfigItemBuilder.ofDouble("value").defaultValue(0.5).range(0.0, 1.0).build();

    // Boundary values should be valid
    assertNull(value.validate(0.0));
    assertNull(value.validate(1.0));

    // Out of boundary values should be invalid
    assertNotNull(value.validate(-0.001));
    assertNotNull(value.validate(1.001));
  }

  @Test
  @DisplayName("String config item with default value")
  void testStringConfigItem() {
    ConfigItem<String> name =
        ConfigItemBuilder.ofString("name")
            .defaultValue("default")
            .description("The display name")
            .build();

    assertEquals("name", name.name());
    assertEquals("default", name.defaultValue());
    assertEquals("default", name.get());
  }

  @Test
  @DisplayName("String config item default value is empty string")
  void testStringConfigItemDefaultEmpty() {
    ConfigItem<String> value = ConfigItemBuilder.ofString("value").build();

    assertEquals("", value.defaultValue());
    assertEquals("", value.get());
  }

  @Test
  @DisplayName("Enum config item")
  void testEnumConfigItem() {
    ConfigItem<TestEnum> mode = ConfigItemBuilder.ofEnum("mode", TestEnum.DEFAULT).build();

    assertEquals("mode", mode.name());
    assertEquals(TestEnum.DEFAULT, mode.defaultValue());
    assertEquals(TestEnum.DEFAULT, mode.get());
  }

  @Test
  @DisplayName("Enum config item set and get")
  void testEnumSetAndGet() {
    ConfigItem<TestEnum> mode = ConfigItemBuilder.ofEnum("mode", TestEnum.DEFAULT).build();

    assertEquals(TestEnum.DEFAULT, mode.get());
    mode.set(TestEnum.ALTERNATIVE);
    assertEquals(TestEnum.ALTERNATIVE, mode.get());
  }

  @Test
  @DisplayName("Reset should restore default value")
  void testReset() {
    ConfigItem<String> name = ConfigItemBuilder.ofString("name").defaultValue("default").build();

    name.set("changed");
    assertEquals("changed", name.get());

    name.reset();
    assertEquals("default", name.get());
  }

  @Test
  @DisplayName("Reset on boolean config item")
  void testResetBoolean() {
    ConfigItem<Boolean> flag = ConfigItemBuilder.ofBoolean("flag").defaultValue(true).build();

    flag.set(false);
    assertEquals(false, flag.get());

    flag.reset();
    assertEquals(true, flag.get());
  }

  @Test
  @DisplayName("Custom validator for email format")
  void testValidator() {
    ConfigItem<String> email =
        ConfigItemBuilder.ofString("email")
            .validator(
                v -> {
                  if (v != null && !v.contains("@")) {
                    return "Invalid email format";
                  }
                  return null;
                })
            .build();

    assertNull(email.validate("test@example.com"));
    assertNotNull(email.validate("invalid"));
  }

  @Test
  @DisplayName("Validator returning null means valid")
  void testValidatorReturnsNull() {
    ConfigItem<String> value = ConfigItemBuilder.ofString("value").validator(v -> null).build();

    assertNull(value.validate("anything"));
    assertNull(value.validate(""));
    assertNull(value.validate(null));
  }

  @Test
  @DisplayName("Validator that throws exception should be handled gracefully")
  void testValidatorThrowsException() {
    ConfigItem<String> value =
        ConfigItemBuilder.ofString("value")
            .validator(
                v -> {
                  throw new RuntimeException("Unexpected error");
                })
            .build();

    String result = value.validate("test");
    assertNotNull(result);
    assertTrue(result.contains("Validator error"));
    assertTrue(result.contains("Unexpected error"));
  }

  @Test
  @DisplayName("Validator can be overwritten")
  void testValidatorOverwritten() {
    ConfigItem<Integer> value =
        new ConfigItemBuilder<>(Codec.INT, "int", 0)
            .validator(v -> v < 0 ? "Negative" : null)
            .validator(v -> v > 100 ? "Too large" : null)
            .build();

    // Second validator should overwrite the first
    assertNull(value.validate(50));
    assertNull(value.validate(-10)); // Negative is now valid
    assertNotNull(value.validate(200));
  }

  @Test
  @DisplayName("User visible default is true")
  void testUserVisibleDefault() {
    ConfigItem<Boolean> visible = ConfigItemBuilder.ofBoolean("visible").build();

    assertTrue(visible.userVisible());
  }

  @Test
  @DisplayName("User visible can be set to false")
  void testUserVisible() {
    ConfigItem<Boolean> hidden = ConfigItemBuilder.ofBoolean("hidden").userVisible(false).build();

    assertFalse(hidden.userVisible());
  }

  @Test
  @DisplayName("User visible can be set to true explicitly")
  void testUserVisibleExplicitTrue() {
    ConfigItem<Boolean> visible = ConfigItemBuilder.ofBoolean("visible").userVisible(true).build();

    assertTrue(visible.userVisible());
  }

  @Test
  @DisplayName("Description default is null")
  void testDescriptionDefault() {
    ConfigItem<String> value = ConfigItemBuilder.ofString("value").build();

    assertNull(value.description());
  }

  @Test
  @DisplayName("Description can be set")
  void testDescriptionSet() {
    ConfigItem<String> value =
        ConfigItemBuilder.ofString("value").description("A test value").build();

    assertEquals("A test value", value.description());
  }

  @Test
  @DisplayName("Codec should be accessible")
  void testCodec() {
    ConfigItem<Boolean> flag = ConfigItemBuilder.ofBoolean("flag").defaultValue(true).build();

    assertNotNull(flag.codec());
    assertEquals(Codec.BOOL, flag.codec());
  }

  @Test
  @DisplayName("Long config item should use Codec.LONG")
  void testLongCodec() {
    ConfigItem<Long> value = ConfigItemBuilder.ofLong("value").build();

    assertEquals(Codec.LONG, value.codec());
  }

  @Test
  @DisplayName("Double config item should use Codec.DOUBLE")
  void testDoubleCodec() {
    ConfigItem<Double> value = ConfigItemBuilder.ofDouble("value").build();

    assertEquals(Codec.DOUBLE, value.codec());
  }

  @Test
  @DisplayName("String config item should use Codec.STRING")
  void testStringCodec() {
    ConfigItem<String> value = ConfigItemBuilder.ofString("value").build();

    assertEquals(Codec.STRING, value.codec());
  }

  @Test
  @DisplayName("encodeStart should encode current value")
  void testEncodeStart() {
    ConfigItem<String> value = ConfigItemBuilder.ofString("value").defaultValue("hello").build();

    DataResult<JsonElement> result = value.encodeStart(JsonOps.INSTANCE);
    assertTrue(result.result().isPresent());
    assertEquals("hello", result.result().get().getAsString());
  }

  @Test
  @DisplayName("encodeStart should reflect current value after set")
  void testEncodeStartAfterSet() {
    ConfigItem<Integer> value = new ConfigItemBuilder<>(Codec.INT, "value", 0).build();

    value.set(42);
    DataResult<JsonElement> result = value.encodeStart(JsonOps.INSTANCE);
    assertTrue(result.result().isPresent());
    assertEquals(42, result.result().get().getAsInt());
  }

  @Test
  @DisplayName("Custom getter and setter should be used")
  void testCustomGetterSetter() {
    AtomicReference<String> storage = new AtomicReference<>("initial");

    ConfigItem<String> value =
        new ConfigItemBuilder<>(Codec.STRING, "value", "default")
            .getter(storage::get)
            .setter(storage::set)
            .build();

    assertEquals("initial", value.get());
    value.set("modified");
    assertEquals("modified", storage.get());
    assertEquals("modified", value.get());
  }

  @Test
  @DisplayName("Reset should use custom setter with default value")
  void testResetWithCustomGetterSetter() {
    AtomicReference<String> storage = new AtomicReference<>("initial");

    ConfigItem<String> value =
        new ConfigItemBuilder<>(Codec.STRING, "value", "default")
            .getter(storage::get)
            .setter(storage::set)
            .build();

    value.set("modified");
    assertEquals("modified", value.get());

    value.reset();
    assertEquals("default", storage.get());
    assertEquals("default", value.get());
  }

  @Test
  @DisplayName("Config item should delegate to external storage")
  void testExternalStorageDelegation() {
    AtomicBoolean externalValue = new AtomicBoolean(false);

    ConfigItem<Boolean> flag =
        new ConfigItemBuilder<>(Codec.BOOL, "flag", true)
            .getter(externalValue::get)
            .setter(externalValue::set)
            .build();

    assertEquals(true, flag.defaultValue());
    assertEquals(false, flag.get()); // External storage starts as false

    flag.set(true);
    assertTrue(externalValue.get());
    assertTrue(flag.get());
  }

  @Test
  @DisplayName("Builder can be reused to create multiple config items")
  void testBuilderReuse() {
    var builder =
        new ConfigItemBuilder<>(Codec.STRING, "base", "default")
            .description("Shared description")
            .userVisible(false);

    ConfigItem<String> item1 = builder.defaultValue("default1").build();
    ConfigItem<String> item2 = builder.defaultValue("default2").build();

    assertEquals("default1", item1.defaultValue());
    assertEquals("default2", item2.defaultValue());
    assertEquals("Shared description", item1.description());
    assertEquals("Shared description", item2.description());
    assertFalse(item1.userVisible());
    assertFalse(item2.userVisible());
  }

  @Test
  @DisplayName("storeLocally should enable local storage")
  void testStoreLocally() {
    ConfigItem<String> value =
        new ConfigItemBuilder<>(Codec.STRING, "value", "default").storeLocally().build();

    assertEquals("default", value.get());
    value.set("modified");
    assertEquals("modified", value.get());
    value.reset();
    assertEquals("default", value.get());
  }

  @Test
  @DisplayName("Set should not prevent invalid values (no enforcement)")
  void testSetDoesNotEnforceValidation() {
    ConfigItem<Long> value =
        ConfigItemBuilder.ofLong("value").defaultValue(5L).range(1, 10).build();

    // Setting an invalid value should still work (validation is advisory)
    value.set(100L);
    assertEquals(100L, value.get());
    assertNotNull(value.validate(100L));
  }

  @Test
  @DisplayName("Generic builder with custom codec")
  void testGenericBuilder() {
    ConfigItem<Integer> value =
        new ConfigItemBuilder<>(Codec.INT, "value", 0)
            .validator(v -> v < 0 ? "Negative" : null)
            .build();

    assertEquals(0, value.defaultValue());
    assertNull(value.validate(10));
    assertNotNull(value.validate(-1));
  }

  @Test
  @DisplayName("Generic builder with custom codec and description")
  void testGenericBuilderWithDescription() {
    ConfigItem<Integer> value =
        new ConfigItemBuilder<>(Codec.INT, "value", 42).description("A test integer").build();

    assertEquals(42, value.defaultValue());
    assertEquals("A test integer", value.description());
  }

  @Test
  @DisplayName("defaultValue can be changed via builder")
  void testDefaultValueChaining() {
    ConfigItem<String> value = ConfigItemBuilder.ofString("value").defaultValue("modified").build();

    assertEquals("modified", value.defaultValue());
    assertEquals("modified", value.get());
  }

  // region: Enum helper class

  enum TestEnum {
    DEFAULT,
    ALTERNATIVE
  }
}
