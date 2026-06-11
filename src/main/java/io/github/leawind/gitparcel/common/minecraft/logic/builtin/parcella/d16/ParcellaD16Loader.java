package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.BlockPalette;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Format;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.state.BlockState;

public class ParcellaD16Loader extends ParcellaD32Loader
    implements ParcellaD16Format, ParcelFormat.Loader<ParcellaD32Format.Config> {
  @Override
  protected void loadBlocks(Context ctx, ProblemReporter problemReporter)
      throws IOException, ParcelException.CorruptedParcelException {

    if (!Files.exists(ctx.blocksDir)) {
      throw new ParcelException.CorruptedParcelException(
          "Blocks directory not found: " + ctx.blocksDir);
    }

    loadSubparcels(ctx, 16, problemReporter);
  }

  @Override
  protected void loadSubparcelBlockStatesRLE3D(
      Context ctx,
      byte[] bytes,
      BlockStateLoader blockStateLoader,
      ProblemReporter problemReporter) {

    byte x0 = 0, y0 = 0, z0 = 0;
    byte x1 = 0, y1 = 0, z1 = 0;

    byte[] buff = new byte[512];
    byte buffLen = 0;

    boolean skipThisLine = false;
    byte sepChar = 0; // 0 = not seen yet, '~' = palette ID, '=' = inline block state

    for_each_byte:
    for (byte b : bytes) {
      if (skipThisLine && b == '\n') {
        skipThisLine = false;
        continue;
      }

      to_report_invalid_line:
      do {

        switch (sepChar) {
          case 0 -> {
            switch (b) {
              case '~', '=' -> {
                x0 = HexUtils.parseChar(buff[0]);
                y0 = HexUtils.parseChar(buff[1]);
                z0 = HexUtils.parseChar(buff[2]);
                if (x0 == -1 || y0 == -1 || z0 == -1) {
                  break to_report_invalid_line;
                }

                if (buffLen == 3) {
                  x1 = x0;
                  y1 = y0;
                  z1 = z0;
                } else {
                  if (buffLen != 6) {
                    break to_report_invalid_line;
                  }
                  x1 = HexUtils.parseChar(buff[3]);
                  y1 = HexUtils.parseChar(buff[4]);
                  z1 = HexUtils.parseChar(buff[5]);
                  if (x1 == -1 || y1 == -1 || z1 == -1) {
                    break to_report_invalid_line;
                  }
                }

                sepChar = b;
                buffLen = 0;
              }
              default -> buff[buffLen++] = b;
            }
          }
          case '~', '=' -> {
            if (b == '\n') {
              if (buffLen == 0) {
                break to_report_invalid_line;
              }

              switch (sepChar) {
                case '~' -> {
                  if (ctx.blockPalette == null) {
                    byte finalLen = buffLen;
                    problemReporter.report(
                        () ->
                            String.format(
                                "Palette ID found ('~') but no palette is loaded. "
                                    + "Cannot resolve palette ID '%s'",
                                new String(buff, 0, finalLen)));
                    skipThisLine = true;
                    continue for_each_byte;
                  }

                  int paletteId = HexUtils.parsePositive(buff, 0, buffLen);
                  if (paletteId == -1) {
                    break to_report_invalid_line;
                  }
                  for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                      for (int z = z0; z <= z1; z++) {
                        BlockState blockState = ctx.blockPalette.get(paletteId);
                        if (blockState == null) {
                          problemReporter.report(
                              () -> String.format("Unknown block palette id %d", paletteId));
                          continue;
                        }
                        blockStateLoader.load(x, y, z, blockState);
                      }
                    }
                  }
                }
                case '=' -> {
                  String stateStr = new String(buff, 0, buffLen, StandardCharsets.UTF_8);

                  var parseResult = BlockPalette.parseBlockState(stateStr);
                  if (parseResult.isErr()) {
                    problemReporter.report(
                        () ->
                            String.format(
                                "Failed to parse block state '%s': %s",
                                stateStr, parseResult.unwrapErr()));
                    break to_report_invalid_line;
                  }
                  BlockState blockState = parseResult.unwrap();

                  for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                      for (int z = z0; z <= z1; z++) {
                        blockStateLoader.load(x, y, z, blockState);
                      }
                    }
                  }
                }
              }

              sepChar = 0;
              buffLen = 0;
            } else {
              buff[buffLen++] = b;
            }
          }
        }
        continue for_each_byte;
      } while (false);

      // Report invalid line and skip the rest of the line
      skipThisLine = true;
      byte finalLen = buffLen;
      sepChar = 0;
      buffLen = 0;
      problemReporter.report(
          () -> String.format("Invalid line: %s", new String(buff, 0, finalLen)));
    }
  }
}
