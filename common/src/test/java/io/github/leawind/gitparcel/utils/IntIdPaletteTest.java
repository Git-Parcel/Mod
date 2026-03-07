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
    for (int i = 0; i < 10; i++) {
      palette.collect("test" + i);
    }

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
    palette.insert(5, "test");
    assertEquals("test", palette.get(5));
    assertEquals(5, palette.getId("test"));

    assertThrows(
        IllegalArgumentException.class, () -> palette.insert(IntIdPalette.VOID_ID, "test"));

    palette.insert(6, null);
    assertNull(palette.get(6));
    assertEquals(IntIdPalette.VOID_ID, palette.getId(null));
  }

  @Test
  void testNextIdAllocation() {
    assertEquals(0, palette.collect("test0"));
    assertEquals(1, palette.collect("test1"));
    assertEquals(2, palette.collect("test2"));

    palette.removeById(1);
    assertEquals(3, palette.collect("test3"));
  }

  @Test
  void testNextIdWraparound() {
    for (int i = 0; i < 10; i++) {
      palette.collect("test" + i);
    }

    palette.removeById(2);
    palette.removeById(5);

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

    customPalette.collect("test");
    assertEquals(1, insertCount.get());

    customPalette.collect("test");
    assertEquals(1, insertCount.get()); // Should not increment

    customPalette.removeById(0);
    assertEquals(1, removeCount.get());

    customPalette.removeById(999);
    assertEquals(1, removeCount.get()); // Should not increment
  }

  @Test
  void testConcurrentIdAllocation() {
    palette.collect("test1");
    palette.collect("test2");
    palette.removeById(0);

    assertEquals(2, palette.collect("test3"));
    assertEquals(3, palette.collect("test4"));
  }

  @Test
  void testEdgeCaseWithSingleIdRange() {
    IntIdPalette<String> palette = new IntIdPalette<>(5, 6);

    int id = palette.collect("test1");
    assertEquals(5, id);

    assertThrows(IllegalStateException.class, () -> palette.collect("test2"));

    palette.removeById(5);
    assertEquals(5, palette.collect("test3"));
  }

  @Test
  void testSetIdStepWithInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> palette.setIdStep(0, 0));
    assertThrows(IllegalArgumentException.class, () -> palette.setIdStep(-1, 0));

    assertThrows(IllegalArgumentException.class, () -> palette.setIdStep(10, -1));
    assertThrows(IllegalArgumentException.class, () -> palette.setIdStep(10, 10));
    assertThrows(IllegalArgumentException.class, () -> palette.setIdStep(10, 15));
  }

  @Test
  void testIdAllocation() {
    palette.setIdStep(5, 2);

    assertEquals(2, palette.collect("test1")); // 0*5 + 2 = 2
    assertEquals(7, palette.collect("test2")); // 1*5 + 2 = 7

    assertEquals("test1", palette.get(2));
    assertEquals("test2", palette.get(7));

    assertThrows(IllegalStateException.class, () -> palette.collect("test3"));
  }

  @Test
  void testIdAllocationWithCustomRange() {
    IntIdPalette<String> customPalette = new IntIdPalette<>(10, 25);
    customPalette.setIdStep(5, 1);

    assertEquals(11, customPalette.collect("test1")); // 10 + 0*5 + 1 = 11
    assertEquals(16, customPalette.collect("test2")); // 10 + 1*5 + 1 = 16
    assertEquals(21, customPalette.collect("test3")); // 10 + 2*5 + 1 = 21

    assertThrows(IllegalStateException.class, () -> customPalette.collect("test4"));
  }

  @Test
  void testIdAllocationWithWraparound() {
    palette.setIdStep(4, 1);

    palette.collect("test1"); // ID 1
    palette.collect("test2"); // ID 5
    palette.collect("test3"); // ID 9

    palette.removeById(5);

    assertEquals(5, palette.collect("test4")); // Reuse ID 5 (1*4 + 1)

    // So next allocation should fail
    assertThrows(IllegalStateException.class, () -> palette.collect("test5"));
  }

  @Test
  void testSizeOneBehavior() {
    palette.setIdStep(1, 0);

    assertEquals(0, palette.collect("test1"));
    assertEquals(1, palette.collect("test2"));
    assertEquals(2, palette.collect("test3"));

    palette.removeById(1);

    assertEquals(3, palette.collect("test4"));
  }

  @Test
  void testIdAllocationWithMultipleUsers() {
    IntIdPalette<String> paletteA = new IntIdPalette<>(0, 20);
    IntIdPalette<String> paletteB = new IntIdPalette<>(0, 20);

    paletteA.setIdStep(5, 0); // User A: offsets 0, 5, 10, 15
    paletteB.setIdStep(5, 2); // User B: offsets 2, 7, 12, 17

    assertEquals(0, paletteA.collect("userA1"));
    assertEquals(5, paletteA.collect("userA2"));

    assertEquals(2, paletteB.collect("userB1"));
    assertEquals(7, paletteB.collect("userB2"));

    assertEquals("userA1", paletteA.get(0));
    assertEquals("userA2", paletteA.get(5));
    assertEquals("userB1", paletteB.get(2));
    assertEquals("userB2", paletteB.get(7));

    assertNull(paletteA.get(2));
    assertNull(paletteA.get(7));
    assertNull(paletteB.get(0));
    assertNull(paletteB.get(5));
  }

  @Test
  void testIdExhaustion() {
    IntIdPalette<String> palette = new IntIdPalette<>(0, 6);
    palette.setIdStep(3, 1);

    assertEquals(1, palette.collect("test1"));
    assertEquals(4, palette.collect("test2"));

    assertThrows(IllegalStateException.class, () -> palette.collect("test3"));
  }

  @Test
  void testShiftAtEdgeOfRange() {
    IntIdPalette<String> palette = new IntIdPalette<>(5, 8);
    palette.setIdStep(3, 0);

    assertEquals(6, palette.collect("test1"));

    assertThrows(IllegalStateException.class, () -> palette.collect("test2"));
  }

  @Test
  void testWithNonZeroStart() {
    IntIdPalette<String> palette = new IntIdPalette<>(7, 100);

    assertEquals(7, palette.collect("test1"));
    assertEquals(8, palette.collect("test2"));

    palette.setIdStep(3, 1);

    assertEquals(10, palette.collect("test3"));
    assertEquals(10, palette.collect("test3"));
    assertEquals(13, palette.collect("test4"));

    {
      // NOW Temp
      palette = new IntIdPalette<>(0, Integer.MAX_VALUE);
      assertEquals(0, palette.collect("test0"));
      assertEquals(0, palette.collect("test0"));
      assertEquals(1, palette.collect("test1"));
    }
  }
}
