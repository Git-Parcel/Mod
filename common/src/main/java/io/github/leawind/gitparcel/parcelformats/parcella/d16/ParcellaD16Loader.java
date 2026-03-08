package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Format;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraft.util.ProblemReporter;

public class ParcellaD16Loader extends ParcellaD32Loader
    implements ParcellaD16Format, ParcelFormat.Load<ParcellaD32Format.Config> {
  @Override
  protected void loadBlocks(Context ctx, ProblemReporter problemReporter)
      throws IOException, ParcelException {

    if (!Files.exists(ctx.blocksDir)) {
      // TODO error handling
      throw new ParcelException("Blocks directory not found: " + ctx.blocksDir);
    }

    loadSubparcels(ctx, 16, problemReporter);
  }

  @Override
  protected int[][][] loadSubparcelRLE3D(
      Subparcel localSubparcel, byte[] bytes, ProblemReporter problemReporter) {

    int sizeX = localSubparcel.sizeX;
    int sizeY = localSubparcel.sizeY;
    int sizeZ = localSubparcel.sizeZ;

    int[][][] blockStates = new int[sizeX][sizeY][sizeZ];

    byte x0 = 0, y0 = 0, z0 = 0;
    byte x1 = 0, y1 = 0, z1 = 0;

    // Buffer to store the current line
    byte[] buff = new byte[16];
    byte len = 0;

    boolean skipThisLine = false;

    for_each_byte:
    for (byte b : bytes) {
      if (skipThisLine && b == '\n') {
        skipThisLine = false;
        continue;
      }

      to_report_invalid_line:
      do {
        switch (b) {
          case '=' -> {
            x0 = HexUtils.parseChar(buff[0]);
            y0 = HexUtils.parseChar(buff[1]);
            z0 = HexUtils.parseChar(buff[2]);
            if (x0 == -1 || y0 == -1 || z0 == -1) {
              break to_report_invalid_line;
            }
            if (len == 3) {
              x1 = x0;
              y1 = y0;
              z1 = z0;
            } else {
              if (len != 6) {
                break to_report_invalid_line;
              }
              x1 = HexUtils.parseChar(buff[3]);
              y1 = HexUtils.parseChar(buff[4]);
              z1 = HexUtils.parseChar(buff[5]);
              if (x1 == -1 || y1 == -1 || z1 == -1) {
                break to_report_invalid_line;
              }
            }

            len = 0;
          }
          case '\n' -> {
            int paletteId = HexUtils.parsePositive(buff, 0, len);
            if (paletteId == -1) {
              break to_report_invalid_line;
            }
            len = 0;

            for (int x = x0; x <= x1; x++) {
              for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                  blockStates[x][y][z] = paletteId;
                }
              }
            }
          }
          case '\r', ' ', '\t', '\0' -> {}
          default -> {
            if (len >= buff.length) {
              break to_report_invalid_line;
            }
            buff[len++] = b;
          }
        }
        continue for_each_byte;
      } while (false);

      // Report invalid line and skip the rest of the line
      skipThisLine = true;
      byte finalLen = len;
      problemReporter.report(
          () -> String.format("Invalid line: %s", new String(buff, 0, finalLen)));
    }

    return blockStates;
  }
}
