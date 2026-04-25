package io.github.leawind.gitparcel.gametest;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.inventory.misc.TempDirectory;
import java.io.IOException;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();

  public void testSavers(GameTestHelpMore helper) throws IOException, ParcelException {
    ParcelFormat.Saver<?>[] savers =
        ParcelFormatRegistry.INSTANCE.streamSavers().toArray(ParcelFormat.Saver[]::new);

    for (var format : savers) {
      Rotation[] rotations =
          format.features().contains(ParcelFormat.Feature.ROTATE)
              ? Rotation.values()
              : new Rotation[] {Rotation.NONE};

      Mirror[] mirrors =
          format.features().contains(ParcelFormat.Feature.MIRROR)
              ? Mirror.values()
              : new Mirror[] {Mirror.NONE};

      for (Rotation rotation : rotations) {
        for (Mirror mirror : mirrors) {
          try (var tempDir = new TempDirectory(GitParcel.MOD_ID)) {
            LOGGER.info(
                "Testing format {} with rotation={} mirror={}", format.spec(), rotation, mirror);

            format.save(
                helper.getLevel(),
                helper.getBoundingBox(),
                rotation,
                mirror,
                null,
                tempDir.getPath(),
                true);

            LOGGER.info("Saved successfully");
          }
          LOGGER.info("  Passed: rotation={}, mirror={}", rotation, mirror);
        }
      }
    }

    helper.succeed();
  }
}
