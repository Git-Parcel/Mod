package io.github.leawind.gitparcel.common.impl.extension.attachment;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentCollector;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/** Mutable state scoped to one capture or placement operation. */
public final class ParcelAttachmentSession implements ParcelAttachmentCollector {
  private final Map<CaptureKey, LocalAttachmentId> capturedIds = new HashMap<>();
  private final List<AttachmentRecord> captured = new ArrayList<>();
  private final Map<LocalAttachmentId, Object> resolved = new HashMap<>();

  @Override
  public LocalAttachmentId collect(
      Object sourceIdentity,
      Identifier type,
      int schemaVersion,
      boolean required,
      CompoundTag payload) {
    var key = new CaptureKey(type, sourceIdentity);
    LocalAttachmentId existing = capturedIds.get(key);
    if (existing != null) {
      return existing;
    }
    LocalAttachmentId id = new LocalAttachmentId("a%08X".formatted(captured.size()));
    capturedIds.put(key, id);
    captured.add(new AttachmentRecord(id, type, schemaVersion, required, payload));
    return id;
  }

  public List<AttachmentRecord> captured() {
    return List.copyOf(captured);
  }

  @Override
  public void resolve(LocalAttachmentId id, Object value) {
    if (resolved.putIfAbsent(id, value) != null) {
      throw new IllegalStateException("Attachment was resolved more than once: " + id);
    }
  }

  @Override
  public <T> Optional<T> findResolved(LocalAttachmentId id, Class<T> type) {
    Object value = resolved.get(id);
    return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
  }

  private record CaptureKey(Identifier type, Object sourceIdentity) {}
}
