package io.github.leawind.gitparcel.gametest.ext;

import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContext;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributor;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelRestoreContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Captures parcel-region data that no record references (here: the dimension name) and re-applies
 * it after restore. Observability fields let the game test assert both hooks ran.
 */
public enum RegionMarkerContributor implements ParcelCaptureContributor {
  INSTANCE;

  public static final Identifier ATTACHMENT_TYPE =
      Identifier.fromNamespaceAndPath("gitparceltest", "region_data");
  private static final String DIMENSION_KEY = "dimension";

  public static volatile int restoreCalls = 0;
  public static volatile String restoredDimension = null;
  public static volatile int restoredAttachmentCount = -1;

  public static void resetObservations() {
    restoreCalls = 0;
    restoredDimension = null;
    restoredAttachmentCount = -1;
  }

  @Override
  public Identifier id() {
    return Identifier.fromNamespaceAndPath("gitparceltest", "region_contributor");
  }

  @Override
  public void capture(ParcelCaptureContext context) {
    if (!(context.level() instanceof net.minecraft.world.level.Level level)) {
      throw new IllegalStateException("Capture contributors require a level");
    }
    var payload = new CompoundTag();
    payload.putString(DIMENSION_KEY, level.dimension().identifier().toString());
    payload.putDouble("bounds_min_x", context.bounds().minX);
    context
        .collector()
        .collect(
            "region", ATTACHMENT_TYPE, 1, true, payload);
  }

  @Override
  public void restore(ParcelRestoreContext context) {
    var own =
        context.attachments().stream()
            .filter(attachment -> ATTACHMENT_TYPE.equals(attachment.type()))
            .toList();
    restoreCalls++;
    restoredAttachmentCount = own.size();
    restoredDimension =
        own.isEmpty() ? null : own.getFirst().payload().getString(DIMENSION_KEY).orElse(null);
  }
}
