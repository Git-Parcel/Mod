package io.github.leawind.gitparcel.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

/**
 * A bidirectional mapping between integer IDs and data objects.
 *
 * <p>This class maintains an N to 1 relationship between IDs and data.
 *
 * <p>An ID is considered as unused if any of the following conditions is satisfied:
 *
 * <ul>
 *   <li>It's not in {@link #byId}.
 *   <li>It maps to {@code null}
 *   <li>It maps to a data object that is not in {@link #byData}.
 * </ul>
 *
 * @param <T> the type of data objects
 */
public class IntIdPalette<T> {
  /** Used as the returned id when a data object is not in {@link #byData}. */
  public static final int VOID_ID = Integer.MIN_VALUE;

  private int minId;
  private int maxId;
  private volatile int nextId;

  /**
   * Mapping from integer IDs to data objects. Protected by read-write lock.
   *
   * <p>If an id is not in use, it maps to {@code null} or a data object that is not in {@link
   * #byData}.
   */
  protected final Int2ObjectSortedMap<T> byId;

  /**
   * Mapping from data objects to integer IDs. Protected by read-write lock.
   *
   * <p>Every data in this map must maps to the corresponding id in {@link #byId}.
   */
  protected final Object2IntMap<T> byData;

  /** Creates a new palette with default range [0, {@value Integer#MAX_VALUE}]. */
  public IntIdPalette() {
    this(Integer.MAX_VALUE);
  }

  /**
   * Creates a new palette with specified maximum id.
   *
   * @param maxId the maximum id
   */
  public IntIdPalette(int maxId) {
    this(0, maxId);
  }

  /**
   * Creates a new palette with specified id range.
   *
   * @param minId the minimum id
   * @param maxId the maximum id
   * @throws IllegalArgumentException if minId >= maxId
   */
  public IntIdPalette(int minId, int maxId) throws IllegalArgumentException {
    this(minId, maxId, new Int2ObjectRBTreeMap<>(), new Object2IntOpenHashMap<>());
  }

  /**
   * Creates a new palette with specified id range and mappings.
   *
   * @param minId the minimum id
   * @param maxId the maximum id
   * @param byId the mapping from integer IDs to data objects
   * @param byData the mapping from data objects to integer IDs
   * @throws IllegalArgumentException if {@code minId >= maxId} or {@code minId} is {@value
   *     #VOID_ID}
   */
  public IntIdPalette(int minId, int maxId, Int2ObjectSortedMap<T> byId, Object2IntMap<T> byData)
      throws IllegalArgumentException {
    setIdRange(minId, maxId);
    this.byId = byId;
    this.byData = byData;
    this.byData.defaultReturnValue(VOID_ID);

    nextId = minId;
  }

  /** Returns the minimum ID value. */
  protected int minId() {
    return minId;
  }

  /** Returns the maximum ID value. */
  protected int maxId() {
    return maxId;
  }

  /** Returns the span of available IDs. */
  public int idSpan() {
    return maxId - minId;
  }

  /**
   * Sets the ID range (inclusive).
   *
   * <p>This only affects future ID allocations. Existing entries are retained even if outside the
   * new range.
   *
   * @throws IllegalArgumentException if {@code minId >= maxId} or {@code minId} is {@value
   *     #VOID_ID}
   */
  public void setIdRange(int minId, int maxId) throws IllegalArgumentException {
    if (minId == VOID_ID) {
      throw new IllegalArgumentException("minId must not be VOID_ID: " + VOID_ID);
    }
    if (minId >= maxId) {
      throw new IllegalArgumentException(
          String.format("minId must be less than maxId, but %d >= %d", minId, maxId));
    }
    this.minId = minId;
    this.maxId = maxId;
  }

  /** Creates an exception indicating that all IDs have been exhausted. */
  protected IllegalStateException createIdExhaustedException() {
    return new IllegalStateException(String.format("ID exhausted in [%d, %d]", minId, maxId));
  }

  /**
   * Returns the next available ID.
   *
   * <p>This method modifies {@link #nextId}
   *
   * @throws IllegalStateException if no ID is available
   */
  @SuppressWarnings("NonAtomicOperationOnVolatileField")
  protected int getNextId() throws IllegalStateException {
    for (int i = idSpan(); i > 0; i--) {
      if (nextId < minId || nextId > maxId) {
        nextId = minId;
      }

      if (byId.containsKey(nextId) || nextId == VOID_ID) {
        nextId++;
      } else {
        return nextId;
      }
    }

    throw createIdExhaustedException();
  }

  /**
   * Returns whether the given ID is in use.
   *
   * @param id the ID
   * @return {@code true} if the ID is in use, {@code false} otherwise
   */
  public boolean isIdInUse(int id) {
    var data = byId.get(id);
    return data != null && byData.containsKey(data);
  }

  /** Returns the number of all IDs, including unused IDs. */
  public int size() {
    return byId.size();
  }

  /**
   * Returns the data associated with the given ID.
   *
   * @param id the ID
   * @return the associated data, or {@code null} if absent
   */
  public @Nullable T get(int id) {
    var data = byId.get(id);
    // if the data is not in byData, it means the id is not in use.
    // So we return null.
    if (byData.containsKey(data)) {
      return data;
    } else {
      return null;
    }
  }

  /**
   * Returns the ID associated with the given data.
   *
   * @param data the data
   * @return the ID, or {@value #VOID_ID} if absent
   */
  public int getId(T data) {
    return byData.getInt(data);
  }

  /**
   * Ensures the given data exists in the palette and returns its ID. If absent, a new ID is
   * assigned.
   *
   * @param data the data to collect
   * @return the ID
   * @throws IllegalStateException if no ID is available
   */
  public int collect(T data) throws IllegalStateException {
    var existingId = byData.getInt(data);

    if (existingId != VOID_ID) {
      return existingId;
    }

    var id = getNextId();
    insert(id, data);
    return id;
  }

  /**
   * Inserts the given data with the specified ID.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>does not check if the ID is already in use.
   *   <li>does not check if the ID is in range.
   * </ul>
   *
   * @param id ID
   * @param data the data to insert
   * @throws IllegalArgumentException if {@code id} is {@value #VOID_ID}
   */
  protected void insert(int id, T data) throws IllegalArgumentException {
    if (id == VOID_ID) {
      throw new IllegalArgumentException("id must not be VOID_ID: " + VOID_ID);
    }
    byId.put(id, data);
    byData.put(data, id);
    onAfterInserted(id, data);
  }

  /**
   * Callback invoked after a successful insertion.
   *
   * <ul>
   *   <li>Collecting visited data does not trigger this method
   *   <li>This method do nothing unless it is overridden by a subclass
   * </ul>
   *
   * @param id id of the inserted new item
   * @param data the inserted new item
   */
  protected void onAfterInserted(int id, T data) {}

  /**
   * Removes the data associated with the given ID.
   *
   * @param id the ID
   * @return the removed data, or {@code null} if absent
   */
  public @Nullable T removeById(int id) {
    var data = byId.remove(id);
    if (data != null) {
      byData.removeInt(data);
      onAfterRemoved(id, data);
    }
    return data;
  }

  /**
   * Removes the given data.
   *
   * <p>Note: If there are multiple IDs associated with the same data in {@link #byId}, only the one
   * exists in {@link #byData} will be removed.
   *
   * @param data the data
   * @return the removed ID, or {@value #VOID_ID} if absent
   */
  public int removeByData(T data) {
    var id = byData.removeInt(data);
    byId.remove(id);
    onAfterRemoved(id, data);
    return id;
  }

  /**
   * Callback invoked after a successful removal.
   *
   * <ul>
   *   <li>Removing non-existent data does not trigger this method
   *   <li>This method do nothing unless it is overridden by a subclass
   * </ul>
   */
  protected void onAfterRemoved(int id, T data) {}

  /** Removes all entries and resets the ID counter. */
  public void clear() {
    byData.clear();
    byId.clear();
    nextId = minId;
  }
}
