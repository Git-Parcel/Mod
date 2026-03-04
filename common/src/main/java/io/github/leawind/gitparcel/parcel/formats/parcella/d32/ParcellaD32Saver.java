package io.github.leawind.gitparcel.parcel.formats.parcella.d32;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.formats.parcella.Microparcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.d16.ParcellaD16Format;
import io.github.leawind.gitparcel.parcel.formats.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.utils.numbase.Base32Utils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ParcellaD32Saver extends ParcellaD16Saver
    implements ParcellaD32Format, ParcelFormat.Save<ParcellaD16Format.Config> {
  @Override
  public void save(
      Level level, Parcel parcel, Path dataDir, boolean ignoreEntities, @Nullable Config config)
      throws IOException {
    if (config == null) {
      config = new Config();
    }

    var ctx = new Context(level, parcel, dataDir, ignoreEntities, config);

    try (ProblemReporter.ScopedCollector problemReporter =
        new ProblemReporter.ScopedCollector(LOGGER)) {

      saveBlocks(ctx, 32);

      if (!ignoreEntities) {
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
