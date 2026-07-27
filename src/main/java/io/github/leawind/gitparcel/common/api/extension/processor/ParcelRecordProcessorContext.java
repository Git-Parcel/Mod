package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import net.minecraft.world.level.LevelAccessor;

/** Common context for semantic processing. */
public record ParcelRecordProcessorContext(
    LevelAccessor level, ParcelSpace space, ParcelAttachmentContext attachments) {}
