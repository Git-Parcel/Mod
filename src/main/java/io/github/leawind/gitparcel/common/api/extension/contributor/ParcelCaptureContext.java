package io.github.leawind.gitparcel.common.api.extension.contributor;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentCollector;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

/** Capture-side context for {@link ParcelCaptureContributor}. */
public record ParcelCaptureContext(
    LevelAccessor level, ParcelSpace space, AABB bounds, ParcelAttachmentCollector collector) {}
