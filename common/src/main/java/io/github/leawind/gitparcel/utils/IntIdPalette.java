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
 * <h2>ID allocation</h2>
 *
 * <h3>Range</h3>
 *
 * <p>{@link #idRangeStart} and {@link #idRangeEnd} define the range of valid IDs.
 *
 * <p>Changing the range does not affect existing IDs. Only future allocations will be restricted to
 * the new range.
 *
 * @param <T> the type of data objects
 */
public class IntIdPalette<T> {
  /** Used as the returned id when a data object is not in {@link #byData}. */
  public static final int VOID_ID = Integer.MIN_VALUE;

  public static final int MAX_ID_SPAN = Integer.MAX_VALUE;

  private int idRangeStart;
  private int idRangeEnd;

  private int idStride = 1;

  private int idShift = 0;

  public volatile int lastId = VOID_ID;

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
   * @param idRangeEnd the maximum id
   */
  public IntIdPalette(int idRangeEnd) {
    this(0, idRangeEnd);
  }

  /**
   * Creates a new palette with specified id range.
   *
   * @param idRangeStart the minimum id (inclusive)
   * @param idRangeEnd the maximum id (exclusive)
   * @throws IllegalArgumentException if minId >= maxId
   */
  public IntIdPalette(int idRangeStart, int idRangeEnd) throws IllegalArgumentException {
    this(idRangeStart, idRangeEnd, new Int2ObjectRBTreeMap<>(), new Object2IntOpenHashMap<>());
  }

  /**
   * Creates a new palette with specified id range and mappings.
   *
   * @param idRangeStart the minimum id (inclusive)
   * @param idRangeEnd the maximum id (exclusive)
   * @param byId the mapping from integer IDs to data objects
   * @param byData the mapping from data objects to integer IDs
   * @throws IllegalArgumentException if {@code minId >= maxId} or {@code minId} is {@value
   *     #VOID_ID}
   */
  public IntIdPalette(
      int idRangeStart, int idRangeEnd, Int2ObjectSortedMap<T> byId, Object2IntMap<T> byData)
      throws IllegalArgumentException {
    setIdRange(idRangeStart, idRangeEnd);
    setIdStep(1, 0);
    this.byId = byId;
    this.byData = byData;
    this.byData.defaultReturnValue(VOID_ID);

    lastId = idRangeStart;
  }

  /** The inclusive start of ID value range. */
  protected int idRangeStart() {
    return idRangeStart;
  }

  /** The exclusive end of ID value range. */
  protected int idRangeEnd() {
    return idRangeEnd;
  }

  /** Returns count of available IDs. */
  public int idSpan() {
    return idRangeEnd - idRangeStart;
  }

  public int idStride() {
    return idStride;
  }

  public int idShift() {
    return idShift;
  }

  /**
   * Sets the ID range.
   *
   * <p>This only affects future ID allocations. Existing entries are retained even if outside the
   * new range.
   *
   * @throws IllegalArgumentException if {@code start >= end}, or {@code start} is {@value
   *     #VOID_ID}, or {@code end} is {@value #VOID_ID}
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
   * @throws IllegalArgumentException if stride is not positive or shift is out of range
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

  /** Creates an exception indicating that all IDs have been exhausted. */
  protected IllegalStateException createIdExhaustedException() {
    return new IllegalStateException(
        String.format("ID exhausted in [%d, %d)", idRangeStart, idRangeEnd));
  }

  /**
   * Returns the next unused ID.
   *
   * <p>This method modifies {@link #lastId}
   *
   * @throws IllegalStateException if no ID is available
   */
  protected int getNextUnusedId() throws IllegalStateException {
    if (idStride == 1) {
      return getNextUnusedIdLinear();
    }

    return getNextUnusedIdUsingStride();
  }

  /** Linear search for next unused ID (original implementation). */
  private int getNextUnusedIdLinear() throws IllegalStateException {
    int id;
    if (lastId < idRangeStart || lastId >= idRangeEnd) {
      id = idRangeStart;
    } else {
      id = lastId;
    }
    int span = idSpan();

    int i = 0;
    while (i < span) {
      if (!isIdInUse(id)) {
        lastId = id + 1;
        return id;
      }

      id++;
      i++;
    }

    throw createIdExhaustedException();
  }

  private int getNextUnusedIdUsingStride() throws IllegalStateException {
    int start = 0;
    if (lastId > idRangeStart) {
      start = (lastId - idRangeStart - 1) / idStride + 1;
    }

    int maxSteps = (idSpan() + idStride - 1) / idStride;

    for (int steps = start; steps < maxSteps; steps++) {
      int candidateId = idRangeStart + steps * idStride + idShift;

      if (candidateId >= idRangeEnd) {
        continue;
      }

      if (!isIdInUse(candidateId)) {
        lastId = candidateId + 1;
        return candidateId;
      }
    }

    for (int steps = 0; steps < start; steps++) {
      int candidateId = idRangeStart + steps * idStride + idShift;

      if (candidateId >= idRangeEnd) {
        continue;
      }

      if (!isIdInUse(candidateId)) {
        lastId = candidateId + 1;
        return candidateId;
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

    var id = getNextUnusedId();
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
   * @param id ID, must not be {@value #VOID_ID}
   * @param data the data to insert, or {@code null} to insert an unused ID
   * @throws IllegalArgumentException if {@code id} is {@value #VOID_ID}
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
   * Callback invoked after a successful insertion.
   *
   * <ul>
   *   <li>Collecting a known data does not trigger this method
   *   <li>This method do nothing unless it is overridden by a subclass
   * </ul>
   *
   * @param id id of the inserted new item
   * @param data the inserted new item, might be {@code null}
   */
  protected void onAfterInserted(int id, @Nullable T data) {}

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
    int id = byData.removeInt(data);
    if (id != VOID_ID) {
      byId.remove(id);
      onAfterRemoved(id, data);
    }
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
  protected void onAfterRemoved(int id, @Nullable T data) {}

  /** Removes all entries and resets the ID counter. */
  public void clear() {
    byData.clear();
    byId.clear();
    lastId = idRangeStart;
  }
}
