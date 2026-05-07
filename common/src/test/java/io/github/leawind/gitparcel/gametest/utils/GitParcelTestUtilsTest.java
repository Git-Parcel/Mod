package io.github.leawind.gitparcel.gametest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GitParcelTestUtilsTest {

  @Test
  void cartesianProduct_emptyMap_returnsSingleEmptyMap() {
    Map<String, List<?>> map = Map.of();
    List<Map<String, ?>> result = GitParcelTestUtils.cartesianProduct(map);
    assertEquals(0, result.size());
  }

  @Test
  void cartesianProduct_singleKeySingleValue() {
    Map<String, List<?>> map = Map.of("a", List.of(1));
    List<Map<String, ?>> result = GitParcelTestUtils.cartesianProduct(map);
    assertEquals(1, result.size());
    assertEquals(1, result.getFirst().get("a"));
  }

  @Test
  void cartesianProduct_singleKeyMultipleValues() {
    Map<String, List<?>> map = Map.of("a", List.of(1, 2, 3));
    List<Map<String, ?>> result = GitParcelTestUtils.cartesianProduct(map);
    assertEquals(3, result.size());
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(1)));
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(2)));
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(3)));
  }

  @Test
  void cartesianProduct_twoKeysMultipleValues() {
    Map<String, List<?>> map = new HashMap<>();
    map.put("a", List.of(1, 2));
    map.put("b", List.of("x", "y"));
    List<Map<String, ?>> result = GitParcelTestUtils.cartesianProduct(map);
    assertEquals(4, result.size());
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(1) && m.get("b").equals("x")));
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(1) && m.get("b").equals("y")));
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(2) && m.get("b").equals("x")));
    assertTrue(result.stream().anyMatch(m -> m.get("a").equals(2) && m.get("b").equals("y")));
  }

  @Test
  void cartesianProduct_threeKeysMixedValues() {
    Map<String, List<?>> map = new HashMap<>();
    map.put("a", List.of(true, false));
    map.put("b", List.of("x"));
    map.put("c", List.of(1, 2));
    List<Map<String, ?>> result = GitParcelTestUtils.cartesianProduct(map);
    assertEquals(4, result.size());
    assertTrue(
        result.stream()
            .anyMatch(
                m -> m.get("a").equals(true) && m.get("b").equals("x") && m.get("c").equals(1)));
    assertTrue(
        result.stream()
            .anyMatch(
                m -> m.get("a").equals(true) && m.get("b").equals("x") && m.get("c").equals(2)));
    assertTrue(
        result.stream()
            .anyMatch(
                m -> m.get("a").equals(false) && m.get("b").equals("x") && m.get("c").equals(1)));
    assertTrue(
        result.stream()
            .anyMatch(
                m -> m.get("a").equals(false) && m.get("b").equals("x") && m.get("c").equals(2)));
  }
}
