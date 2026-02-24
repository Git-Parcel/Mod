package io.github.leawind.gitparcel.parcel.formats.parcella;

import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;

public class ParcellaV0 implements ParcelFormat {

  @Override
  public String id() {
    return "parcella";
  }

  @Override
  public int version() {
    return 0;
  }

  public static final class Save extends ParcellaV0 implements ParcelFormat.Save {
    @Override
    public void save(
        ServerLevel level,
        BlockPos from,
        Vec3i size,
        Path dir,
        boolean includeBlock,
        boolean includeEntity)
        throws IOException {
      // NOW
      throw new RuntimeException("Not implemented");
    }
  }
}
