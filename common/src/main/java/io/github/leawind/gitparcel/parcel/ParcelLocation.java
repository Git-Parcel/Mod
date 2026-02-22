package io.github.leawind.gitparcel.parcel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record ParcelLocation(ServerLevel level, BoundingBox box) {}
