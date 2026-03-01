package io.github.leawind.gitparcel.utils;

import static org.junit.jupiter.api.Assertions.*;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntIdPaletteTest {

  private IntIdPalette<String> palette;

  @BeforeEach
  void setUp() {
    palette = new IntIdPalette<>(10); // Use a small range for testing
  }

  @Test
  void testDefaultConstructor() {
    IntIdPalette<String> defaultPalette = new IntIdPalette<>();
    assertEquals(0, defaultPalette.idRangeStart());
    assertEquals(Integer.MAX_VALUE, defaultPalette.idRangeEnd());
    assertEquals(Integer.MAX_VALUE, defaultPalette.idSpan());
  }

  @Test
  void testConstructorWithMaxId() {
    IntIdPalette<String> palette = new IntIdPalette<>(100);
    assertEquals(0, palette.idRangeStart());
    assertEquals(100, palette.idRangeEnd());
    assertEquals(100, palette.idSpan());
  }

  @Test
  void testConstructorWithMinMaxId() {
    IntIdPalette<String> palette = new IntIdPalette<>(5, 15);
    assertEquals(5, palette.idRangeStart());
    assertEquals(15, palette.idRangeEnd());
    assertEquals(10, palette.idSpan());
  }

  @Test
  void testConstructorWithInvalidRange() {
    assertThrows(IllegalArgumentException.class, () -> new IntIdPalette<>(10, 5));
    assertThrows(IllegalArgumentException.class, () -> new IntIdPalette<>(10, 10));
  }

  @Test
  void testConstructorWithVoidIdAsMinId() {
    assertThrows(
        IllegalArgumentException.class, () -> new IntIdPalette<>(IntIdPalette.VOID_ID, 10));
  }

  @Test
  void testConstructorWithCustomMaps() {
    Int2ObjectRBTreeMap<String> byId = new Int2ObjectRBTreeMap<>();
    Object2IntOpenHashMap<String> byData = new Object2IntOpenHashMap<>();

    IntIdPalette<String> palette = new IntIdPalette<>(1, 10, byId, byData);

    assertEquals(1, palette.idRangeStart());
    assertEquals(10, palette.idRangeEnd());
    assertSame(byId, palette.byId);
    assertSame(byData, palette.byData);
  }

  @Test
  void testSetIdRange() {
    palette.setIdRange(20, 30);
    assertEquals(20, palette.idRangeStart());
    assertEquals(30, palette.idRangeEnd());
    assertEquals(10, palette.idSpan());
  }

  @Test
  void testSetIdRangeWithInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> palette.setIdRange(10, 5));
    assertThrows(IllegalArgumentException.class, () -> palette.setIdRange(10, 10));
    assertThrows(
        IllegalArgumentException.class, () -> palette.setIdRange(IntIdPalette.VOID_ID, 10));
  }

  @Test
  void testCollectNewData() {
    int id1 = palette.collect("test1");
    int id2 = palette.collect("test2");

    assertEquals(0, id1);
    assertEquals(1, id2);
    assertEquals("test1", palette.get(id1));
    assertEquals("test2", palette.get(id2));
    assertEquals(id1, palette.getId("test1"));
    assertEquals(id2, palette.getId("test2"));
  }

  @Test
  void testCollectExistingData() {
    int id1 = palette.collect("test");
    int id2 = palette.collect("test"); // Same data

    assertEquals(id1, id2);
    assertEquals(1, palette.size());
  }

  @Test
  void testCollectWithIdExhaustion() {
    // Fill the palette
    for (int i = 0; i < 10; i++) {
      palette.collect("test" + i);
    }

    // Next collect should throw exception
    assertThrows(IllegalStateException.class, () -> palette.collect("overflow"));
  }

  @Test
  void testGetById() {
    int id = palette.collect("test");
    assertEquals("test", palette.get(id));
    assertNull(palette.get(999)); // Non-existent ID
  }

  @Test
  void testGetIdByData() {
    int id = palette.collect("test");
    assertEquals(id, palette.getId("test"));
    assertEquals(IntIdPalette.VOID_ID, palette.getId("nonexistent"));
  }

  @Test
  void testIsIdInUse() {
    int id = palette.collect("test");
    assertTrue(palette.isIdInUse(id));
    assertFalse(palette.isIdInUse(999)); // Non-existent ID
  }

  @Test
  void testSize() {
    assertEquals(0, palette.size());
    palette.collect("test1");
    assertEquals(1, palette.size());
    palette.collect("test2");
    assertEquals(2, palette.size());
  }

  @Test
  void testRemoveById() {
    int id = palette.collect("test");
    assertEquals("test", palette.removeById(id));
    assertNull(palette.get(id));
    assertEquals(IntIdPalette.VOID_ID, palette.getId("test"));
    assertFalse(palette.isIdInUse(id));
  }

  @Test
  void testRemoveNonExistentById() {
    assertNull(palette.removeById(999));
  }

  @Test
  void testRemoveByData() {
    int id = palette.collect("test");
    assertEquals(id, palette.removeByData("test"));
    assertNull(palette.get(id));
    assertEquals(IntIdPalette.VOID_ID, palette.getId("test"));
    assertFalse(palette.isIdInUse(id));
  }

  @Test
  void testRemoveNonExistentByData() {
    assertEquals(IntIdPalette.VOID_ID, palette.removeByData("nonexistent"));
  }

  @Test
  void testClear() {
    palette.collect("test1");
    palette.collect("test2");

    palette.clear();

    assertEquals(0, palette.size());
    assertEquals(0, palette.idRangeStart()); // Should reset to minId
  }

  @Test
  void testInsertProtectedMethod() {
    // Test valid insertion
    palette.insert(5, "test");
    assertEquals("test", palette.get(5));
    assertEquals(5, palette.getId("test"));

    // Test insertion with VOID_ID
    assertThrows(
        IllegalArgumentException.class, () -> palette.insert(IntIdPalette.VOID_ID, "test"));

    // Test insertion with null data
    palette.insert(6, null);
    assertNull(palette.get(6));
    assertEquals(IntIdPalette.VOID_ID, palette.getId(null));
  }

  @Test
  void testNextIdAllocation() {
    // Test sequential allocation
    assertEquals(0, palette.collect("test0"));
    assertEquals(1, palette.collect("test1"));
    assertEquals(2, palette.collect("test2"));

    // Remove middle element - ID reuse is not guaranteed
    palette.removeById(1);
    // Next allocation continues from next available ID
    assertEquals(3, palette.collect("test3"));
  }

  @Test
  void testNextIdWraparound() {
    // Fill the palette
    for (int i = 0; i < 10; i++) {
      palette.collect("test" + i);
    }

    // Remove some elements - ID reuse is not guaranteed
    palette.removeById(2);
    palette.removeById(5);

    // Next allocation continues from next available ID
    assertEquals(9, palette.collect("test9"));
    assertEquals(2, palette.collect("test10"));
    assertEquals(5, palette.collect("test11"));
  }

  @Test
  void testIdSpan() {
    assertEquals(10, palette.idSpan());
    palette.setIdRange(5, 20);
    assertEquals(15, palette.idSpan());
  }

  @Test
  void testVoidIdConstant() {
    assertEquals(Integer.MIN_VALUE, IntIdPalette.VOID_ID);
  }

  @Test
  void testCallbackMethods() {
    AtomicInteger insertCount = new AtomicInteger(0);
    AtomicInteger removeCount = new AtomicInteger(0);

    IntIdPalette<String> customPalette =
        new IntIdPalette<>(10) {
          @Override
          protected void onAfterInserted(int id, String data) {
            insertCount.incrementAndGet();
          }

          @Override
          protected void onAfterRemoved(int id, String data) {
            removeCount.incrementAndGet();
          }
        };

    // Test insertion callback
    customPalette.collect("test");
    assertEquals(1, insertCount.get());

    // Test that collecting existing data doesn't trigger callback
    customPalette.collect("test");
    assertEquals(1, insertCount.get()); // Should not increment

    // Test removal callback
    customPalette.removeById(0);
    assertEquals(1, removeCount.get());

    // Test that removing non-existent data doesn't trigger callback
    customPalette.removeById(999);
    assertEquals(1, removeCount.get()); // Should not increment
  }

  @Test
  void testConcurrentIdAllocation() {
    // Test that nextId is properly managed across operations
    palette.collect("test1");
    palette.collect("test2");
    palette.removeById(0);

    // ID reuse is not guaranteed
    assertEquals(2, palette.collect("test3"));
    assertEquals(3, palette.collect("test4"));
  }

  @Test
  void testEdgeCaseWithSingleIdRange() {
    IntIdPalette<String> singlePalette = new IntIdPalette<>(5, 6);

    int id = singlePalette.collect("test1");
    assertEquals(5, id);

    // Should throw when trying to collect another
    assertThrows(IllegalStateException.class, () -> singlePalette.collect("test2"));

    // After removal, should be able to collect again
    singlePalette.removeById(5);
    // In single ID range case, the freed ID will be reused
    assertEquals(5, singlePalette.collect("test3"));
  }
}
