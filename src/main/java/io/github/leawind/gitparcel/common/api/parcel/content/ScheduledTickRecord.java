package io.github.leawind.gitparcel.common.api.parcel.content;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.ticks.TickPriority;

/**
 * One scheduled block or fluid tick inside the parcel extent (SEMANTICS.md rule 6.3).
 *
 * <p>The position is anchor-relative and always inside-pointing: a tick's anchor is the block it
 * targets, so the extent containment doubles as the inside/outside test. The delay is the trigger
 * tick relativized per rule 2.3 ({@code triggerTick - gameTime}); restores write back
 * {@code gameTime' + delay}. Sub-tick ordering is not part of the portable record, mirroring
 * vanilla chunk serialization which also discards it.
 */
public record ScheduledTickRecord(
    boolean fluid, Identifier typeId, BlockPos pos, int delay, TickPriority priority) {}
