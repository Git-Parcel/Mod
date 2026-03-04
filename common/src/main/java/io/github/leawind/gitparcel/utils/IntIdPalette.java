package io.github.leawind.gitparcel.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

public class IntIdPalette<T> {
  public static final int VOID_ID = Integer.MIN_VALUE;

  public static final int MAX_ID_SPAN = Integer.MAX_VALUE;

  private int idRangeStart;
  private int idRangeEnd;

  private int idStride = 1;

  private int idShift = 0;

  public volatile int lastId = VOID_ID;

  protected final Int2ObjectSortedMap<T> byId;

  protected final Object2IntMap<T> byData;

  public IntIdPalette() {
    this(Integer.MAX_VALUE);
  }

  public IntIdPalette(int idRangeEnd) {
    this(0, idRangeEnd);
  }

  public IntIdPalette(int idRangeStart, int idRangeEnd) throws IllegalArgumentException {
    this(idRangeStart, idRangeEnd, new Int2ObjectRBTreeMap<>(), new Object2IntOpenHashMap<>());
  }

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

  protected int idRangeStart() {
    return idRangeStart;
  }

  protected int idRangeEnd() {
    return idRangeEnd;
  }

  public int idSpan() {
    return idRangeEnd - idRangeStart;
  }

  public int idStride() {
    return idStride;
  }

  public int idShift() {
    return idShift;
  }

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

  protected IllegalStateException createIdExhaustedException() {
    return new IllegalStateException(
        String.format("ID exhausted in [%d, %d)", idRangeStart, idRangeEnd));
  }

  protected int getNextUnusedId() throws IllegalStateException {
    int id;

    id = Math.floorDiv(lastId, idStride) * idStride + idShift;
    if (id <= lastId) id += idStride;
    while (id < idRangeEnd) {
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

    throw createIdExhaustedException();
  }

  public boolean isIdInUse(int id) {
    var data = byId.get(id);
    return data != null && byData.containsKey(data);
  }

  public int size() {
    return byId.size();
  }

  public @Nullable T get(int id) {
    var data = byId.get(id);
    if (byData.containsKey(data)) {
      return data;
    } else {
      return null;
    }
  }

  public int getId(T data) {
    return byData.getInt(data);
  }

  public int collect(T data) throws IllegalStateException {
    var existingId = byData.getInt(data);

    if (existingId != VOID_ID) {
      return existingId;
    }

    var id = getNextUnusedId();
    insert(id, data);
    return id;
  }

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

  protected void onAfterInserted(int id, @Nullable T data) {}

  public @Nullable T removeById(int id) {
    var data = byId.remove(id);
    if (data != null) {
      byData.removeInt(data);
      onAfterRemoved(id, data);
    }
    return data;
  }

  public int removeByData(T data) {
    int id = byData.removeInt(data);
    if (id != VOID_ID) {
      byId.remove(id);
      onAfterRemoved(id, data);
    }
    return id;
  }

  protected void onAfterRemoved(int id, @Nullable T data) {}

  public void clear() {
    byData.clear();
    byId.clear();
    lastId = idRangeStart;
  }
}
