package io.github.leawind.gitparcel.parcel.formats.parcella;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.utils.numbase.Base32Utils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface ParcellaD32FormatV0 extends ParcellaD16FormatV0 {

  @Override
  default String id() {
    return "parcella_d32";
  }

  class Save extends ParcellaD16FormatV0.Save
      implements ParcellaD32FormatV0, ParcelFormat.Save<Config> {
    @Override
    public void save(
        Level level, Parcel parcel, Path dataDir, boolean saveEntities, @Nullable Config config)
        throws IOException {
      if (config == null) {
        config = new Config();
      }

      var ctx = new Context(level, parcel, dataDir, saveEntities, config);

      try (ProblemReporter.ScopedCollector problemReporter =
          new ProblemReporter.ScopedCollector(LOGGER)) {

        saveBlocks(ctx, 32);

        if (saveEntities) {
          saveEntities(ctx, problemReporter);
        }
      }
    }

    @Override
    protected void writeSubparcelWithMicroparcels(Context ctx, Path file, Subparcel subparcel)
        throws IOException {
      StringBuilder sb = new StringBuilder(8192);
      char[] chars = Base32Utils.BASE32_DIGITS;

      for (var microparcel : Microparcel.subdivide(subparcel, ctx.level, ctx.blockPalette)) {
        sb.append(chars[microparcel.originX])
            .append(chars[microparcel.originY])
            .append(chars[microparcel.originZ]);

        if (microparcel.sizeX != 1 || microparcel.sizeY != 1 || microparcel.sizeZ != 1) {
          sb.append(chars[microparcel.sizeX - 1])
              .append(chars[microparcel.sizeY - 1])
              .append(chars[microparcel.sizeZ - 1]);
        }

        sb.append('=').append(Base32Utils.toBase32(microparcel.value)).append('\n');
      }

      Files.writeString(file, sb, StandardCharsets.UTF_8);
    }
  }
}
