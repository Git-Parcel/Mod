package io.github.leawind.gitparcel.common.api.extension.contributor;

import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import java.util.List;
import net.minecraft.world.level.LevelAccessor;

/** Restore-side context for {@link ParcelCaptureContributor}. */
public record ParcelRestoreContext(
    LevelAccessor level, ParcelSpace space, List<AttachmentRecord> attachments) {}
