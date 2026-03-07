package io.github.leawind.gitparcel.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

/**
 * A bidirectional palette that assigns and manages unique {@code int} identifiers for arbitrary
 * data objects within a configurable numeric range.
 *
 * <p>This class maintains two synchronized mappings:
 *
 * <ul>
 *   <li>An ID-to-data map (sorted by ID).
 *   <li>A data-to-ID map for reverse lookup.
 * </ul>
 *
 * <p>IDs are allocated from a half-open interval {@code [idRangeStart, idRangeEnd)}. Allocation
 * follows a configurable stride/shift scheme:
 *
 * <pre>
 *   id = k * stride + shift
 * </pre>
 *
 * where:
 *
 * <ul>
 *   <li>{@code stride > 0}
 *   <li>{@code 0 <= shift < stride}
 * </ul>
 *
 * This allows partitioning the ID space into disjoint arithmetic progressions.
 *
 * <h2>ID Allocation Semantics</h2>
 *
 * <p>When {@link #collect(Object)} is called for a previously unseen data object, the next unused
 * ID matching the configured stride/shift pattern is selected. Allocation proceeds forward from the
 * last assigned ID and wraps to the beginning of the range if necessary. If no free ID is
 * available, an {@link IllegalStateException} is thrown.
 *
 * <p>If the data object is already present, its existing ID is returned.
 *
 * <h2>Null Handling</h2>
 *
 * <p>{@code null} values may be stored in the ID-to-data map, but they are not inserted into the
 * reverse map. As a result, {@link #get(int)} returns {@code null} either when:
 *
 * <ul>
 *   <li>No mapping exists for the ID
 *   <li>The mapped value is {@code null}
 *   <li>The mapped value is not registered in the reverse map.
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This class is not thread-safe. Concurrent mutation requires external synchronization.
 *
 * @param <T> the type of data stored in the palette
 */
public class IntIdPalette<T> {
  /**
   * Special sentinel value indicating the absence of an ID. This value is never assigned to data.
   */
  public static final int VOID_ID = Integer.MIN_VALUE;

  /** Maximum allowed span of the ID range. */
  public static final int MAX_ID_SPAN = Integer.MAX_VALUE;

  private int idRangeStart;
  private int idRangeEnd;

  private int idStride = 1;

  private int idShift = 0;

  /** The last ID that was allocated, used as a starting point for finding the next available ID. */
  public int lastId = VOID_ID;

  /** The ID-to-data mapping, sorted by ID. */
  protected final Int2ObjectSortedMap<T> byId;

  /** The data-to-ID mapping for reverse lookup. */
  protected final Object2IntMap<T> byData;

  /** Constructs a palette with the default ID range [0, Integer.MAX_VALUE). */
  public IntIdPalette() {
    this(Integer.MAX_VALUE);
  }

  /**
   * Constructs a palette with the ID range [0, idRangeEnd).
   *
   * @param idRangeEnd the exclusive upper bound of the ID range
   */
  public IntIdPalette(int idRangeEnd) {
    this(0, idRangeEnd);
  }

  /**
   * Constructs a palette with the specified ID range [idRangeStart, idRangeEnd).
   *
   * @param idRangeStart the inclusive lower bound of the ID range
   * @param idRangeEnd the exclusive upper bound of the ID range
   * @throws IllegalArgumentException if the range is invalid
   */
  public IntIdPalette(int idRangeStart, int idRangeEnd) throws IllegalArgumentException {
    this(idRangeStart, idRangeEnd, new Int2ObjectRBTreeMap<>(), new Object2IntOpenHashMap<>());
  }

  /**
   * Constructs a palette with the specified ID range and custom map implementations.
   *
   * @param idRangeStart the inclusive lower bound of the ID range
   * @param idRangeEnd the exclusive upper bound of the ID range
   * @param byId the ID-to-data map implementation to use
   * @param byData the data-to-ID map implementation to use
   * @throws IllegalArgumentException if the range is invalid
   */
  public IntIdPalette(
      int idRangeStart, int idRangeEnd, Int2ObjectSortedMap<T> byId, Object2IntMap<T> byData)
      throws IllegalArgumentException {
    setIdRange(idRangeStart, idRangeEnd);
    setIdStep(1, 0);
    this.byId = byId;
    this.byData = byData;
    this.byData.defaultReturnValue(VOID_ID);

    lastId = idRangeEnd;
  }

  /** The start of the valid ID range (inclusive). */
  protected int idRangeStart() {
    return idRangeStart;
  }

  /** The end of the valid ID range (exclusive). */
  protected int idRangeEnd() {
    return idRangeEnd;
  }

  /**
   * Returns the size of the configured ID interval: {@code idRangeEnd - idRangeStart}.
   *
   * @return the span of the ID range
   */
  public int idSpan() {
    return idRangeEnd - idRangeStart;
  }

  /**
   * Returns the current stride used for ID allocation.
   *
   * @return the stride (always positive)
   */
  public int idStride() {
    return idStride;
  }

  /**
   * Returns the current shift used for ID allocation.
   *
   * @return the shift, in the range {@code [0, stride)}
   */
  public int idShift() {
    return idShift;
  }

  /**
   * Configures the half-open ID range {@code [start, end)}.
   *
   * @param start inclusive lower bound (must not equal {@link #VOID_ID})
   * @param end exclusive upper bound (must be greater than {@code start})
   * @throws IllegalArgumentException if the arguments are invalid or the span exceeds {@link
   *     #MAX_ID_SPAN}
   */
  public void setIdRange(int start, int end) throws IllegalArgumentException {
    if (start == VOID_ID) {
      throw new IllegalArgumentException("start must not be VOID_ID: " + VOID_ID);
    }
    if (start >= end) {
      throw new IllegalArgumentException(
          String.format("start must be less than end, but %d >= %d", start, end));
    }
    if ((long) end - start > MAX_ID_SPAN) {
      throw new IllegalArgumentException(
          String.format("ID span must be less than %d, but got %d", MAX_ID_SPAN, end - start + 1));
    }
    this.idRangeStart = start;
    this.idRangeEnd = end;
  }

  /**
   * Configures the stride and shift used for ID allocation.
   *
   * <p>All generated IDs satisfy:
   *
   * <pre>
   * id % stride == shift
   * </pre>
   *
   * @param stride positive step size between candidate IDs
   * @param shift offset within the stride, must satisfy {@code 0 <= shift < stride}
   * @throws IllegalArgumentException if the parameters are invalid
   */
  public void setIdStep(int stride, int shift) throws IllegalArgumentException {
    if (stride <= 0) {
      throw new IllegalArgumentException("stride must be positive: " + stride);
    }
    if (shift < 0 || shift >= stride) {
      throw new IllegalArgumentException(
          String.format("shift must be in range [0, %d), but got %d", stride, shift));
    }
    this.idStride = stride;
    this.idShift = shift;
  }

  /**
   * Finds and returns the next unused ID in the configured range. Searches forward from the last
   * allocated ID, then wraps around to the beginning if necessary.
   *
   * @return the next available ID
   * @throws IllegalStateException if no free ID is available
   */
  protected int getNextUnusedId() throws IllegalStateException {
    int id;

    // from lastId (exclusive) to idRangeEnd
    id = Math.floorDiv(lastId, idStride) * idStride + idShift;
    if (id <= lastId) id += idStride;
    while (id > idRangeStart && id < idRangeEnd) {
      if (!isIdInUse(id)) {
        return lastId = id;
      }
      id += idStride;
    }

    id = Math.floorDiv(idRangeStart, idStride) * idStride + idShift;
    if (id < idRangeStart) id += idStride;
    while (id < lastId) {
      if (!isIdInUse(id)) {
        return lastId = id;
      }
      id += idStride;
    }

    // This happens when lastId was removed, which is relatively rare
    if (!isIdInUse(lastId)) {
      return lastId;
    }

    throw new IllegalStateException(
        String.format("ID exhausted in [%d, %d)", idRangeStart, idRangeEnd));
  }

  /**
   * Returns {@code true} if the given ID is currently mapped to a valid data entry.
   *
   * @param id the ID to test
   * @return {@code true} if in use; {@code false} otherwise
   */
  public boolean isIdInUse(int id) {
    var data = byId.get(id);
    return data != null && byData.containsKey(data);
  }

  /**
   * Returns the number of ID-to-data mappings currently stored.
   *
   * @return the palette size
   */
  public int size() {
    return byId.size();
  }

  /**
   * Returns the data associated with the given ID, or {@code null} if no valid mapping exists.
   *
   * @param id the ID to look up
   * @return the mapped data, or {@code null} if absent
   */
  public @Nullable T get(int id) {
    var data = byId.get(id);
    if (byData.containsKey(data)) {
      return data;
    } else {
      return null;
    }
  }

  /**
   * Returns the ID associated with the given data, or {@link #VOID_ID} if the data is not present.
   *
   * @param data the data object
   * @return the assigned ID, or {@link #VOID_ID} if not found
   */
  public int getId(T data) {
    return byData.getInt(data);
  }

  /**
   * Ensures that the given data object has an assigned ID.
   *
   * <p>If the object is already present, its existing ID is returned. Otherwise, a new ID is
   * allocated and associated with it.
   *
   * @param data the data object
   * @return the existing or newly assigned ID
   * @throws IllegalStateException if no free ID is available
   */
  public int collect(T data) throws IllegalStateException {
    var existingId = byData.getInt(data);

    if (existingId != VOID_ID) {
      return existingId;
    }

    var id = getNextUnusedId();
    insert(id, data);
    return id;
  }

  /**
   * Inserts a new ID-to-data mapping into the palette.
   *
   * @param id the ID to insert (must not be {@link #VOID_ID})
   * @param data the associated data (may be {@code null})
   * @throws IllegalArgumentException if the ID is {@link #VOID_ID}
   */
  protected void insert(int id, @Nullable T data) throws IllegalArgumentException {
    if (id == VOID_ID) {
      throw new IllegalArgumentException("id must not be VOID_ID: " + VOID_ID);
    }
    byId.put(id, data);
    if (data != null) {
      byData.put(data, id);
    }
    onAfterInserted(id, data);
  }

  /**
   * Hook invoked after a new mapping has been inserted. Subclasses may override to perform
   * additional actions.
   *
   * @param id the inserted ID
   * @param data the associated data (possibly {@code null})
   */
  protected void onAfterInserted(int id, @Nullable T data) {}

  /**
   * Removes the mapping for the specified ID.
   *
   * @param id the ID to remove
   * @return the previously associated data, or {@code null} if none
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
   * Removes the mapping for the specified data object.
   *
   * @param data the data to remove
   * @return the previously assigned ID, or {@link #VOID_ID} if not present
   */
  public int removeByData(T data) {
    int id = byData.removeInt(data);
    if (id != VOID_ID) {
      byId.remove(id);
      onAfterRemoved(id, data);
    }
    return id;
  }

  /**
   * Hook invoked after a mapping has been removed. Subclasses may override to perform additional
   * actions.
   *
   * @param id the removed ID
   * @param data the associated data (possibly {@code null})
   */
  protected void onAfterRemoved(int id, @Nullable T data) {}

  /** Removes all mappings and resets allocation state. */
  public void clear() {
    byData.clear();
    byId.clear();
    lastId = idRangeStart;
  }
}
