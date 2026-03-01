package io.github.leawind.gitparcel.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class IntIdPalette<T> {
  protected int minId;
  protected int maxId;

  protected Map<Integer, T> byId = new Int2ObjectRBTreeMap<>();
  protected Map<T, Integer> byData = new Object2IntArrayMap<>();

  public int nextId = 0;

  /** Creates a new int id palette with the default id range [0, {@value Integer#MAX_VALUE}]. */
  public IntIdPalette() {
    this(Integer.MAX_VALUE);
  }

  /**
   * Creates a new int id palette with the specified maximum id.
   *
   * @param maxId the maximum id of the palette
   */
  public IntIdPalette(int maxId) {
    this(0, maxId);
  }

  /**
   * Creates a new int id palette with the specified id range.
   *
   * @param minId the minimum id of the palette
   * @param maxId the maximum id of the palette
   * @throws IllegalArgumentException if minId is not less than maxId
   */
  public IntIdPalette(int minId, int maxId) throws IllegalArgumentException {
    setIdRange(minId, maxId);
  }

  /**
   * Sets the id range of the palette.
   *
   * <p>Note that the id range is inclusive.
   *
   * <p>All existing data in the palette are reserved even if their ids are out of the new id range
   *
   * @param minId the minimum id of the palette
   * @param maxId the maximum id of the palette
   * @throws IllegalArgumentException if minId is not less than maxId
   */
  public void setIdRange(int minId, int maxId) throws IllegalArgumentException {
    if (minId >= maxId) {
      throw new IllegalArgumentException(
          String.format("minId must be less than maxId, but %d >= %d", minId, maxId));
    }
    this.minId = minId;
    this.maxId = maxId;
  }

  public int idSpan() {
    return maxId - minId;
  }

  protected IllegalStateException createIdExhaustedException() {
    return new IllegalStateException(String.format("ID exhausted in [%d, %d]", minId, maxId));
  }

  /**
   * Finds the next available id in the palette.
   *
   * @return the next available id
   * @throws IllegalStateException if no available id is found
   */
  protected int findNextId() throws IllegalStateException {
    int idSpan = idSpan();

    for (int i = 0; i < idSpan; i++) {
      if (nextId < minId || nextId > maxId) {
        nextId = minId;
      }

      if (byId.containsKey(nextId)) {
        nextId++;
      } else {
        return nextId;
      }
    }

    throw createIdExhaustedException();
  }

  protected int findNextIdMinimum() throws IllegalStateException {
    for (int i = minId; i <= maxId; i++) {
      if (!byId.containsKey(i)) {
        return i;
      }
    }

    throw createIdExhaustedException();
  }

  public int size() {
    return byId.size();
  }

  public @Nullable T get(int id) {
    return byId.get(id);
  }

  /**
   * Get the id of the given data.
   *
   * @param data the data to get id for
   * @return the id of the data, or -1 if the data is not in this palette
   */
  public int getId(T data) {
    var id = byData.get(data);
    if (id == null) {
      return -1;
    }
    return id;
  }

  /**
   * Collects the given data. If the data is not in this palette, a new id will be assigned to it.
   *
   * @param data the data to collect
   * @return the id of the data
   * @throws IllegalStateException if no available id is found
   */
  public int collect(T data) throws IllegalStateException {
    var id = byData.get(data);

    if (id == null) {
      id = findNextId();
      byId.put(id, data);
      byData.put(data, id);

      onInserted(id, data);
    }

    return id;
  }

  /**
   * Removes the data with the given id.
   *
   * @param id id of the data to remove
   * @return the removed data, or null if the id is not in this palette
   */
  public @Nullable T removeById(int id) {
    var data = byId.remove(id);
    if (data != null) {
      byData.remove(data);
      onRemoved(id, data);
    }
    return data;
  }

  /**
   * Removes data.
   *
   * @param data the data to remove
   * @return the id of the removed data, or -1 if the data is not in this palette
   */
  public int removeByData(T data) {
    var id = byData.remove(data);
    if (id != null) {
      byId.remove(id);
      onRemoved(id, data);
      return id;
    }
    return -1;
  }

  /**
   * Called after a new item is inserted to this palette
   *
   * <ul>
   *   <li>Collecting visited data does not trigger this method
   *   <li>This method do nothing unless it is overridden by a subclass
   * </ul>
   *
   * @param id id of the inserted new item
   * @param data the inserted new item
   */
  protected void onInserted(int id, T data) {}

  /**
   * Called after an item is removed from this palette
   *
   * <ul>
   *   <li>Removing non-existent data does not trigger this method
   *   <li>This method do nothing unless it is overridden by a subclass
   * </ul>
   *
   * @param id id of the removed item
   * @param data the removed item
   */
  protected void onRemoved(int id, T data) {}

  public void clear() {
    byData.clear();
    byId.clear();
  }
}
