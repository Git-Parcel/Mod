package io.github.leawind.gitparcel.gametest;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.ParcelStorage;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.gitparcel.gametest.utils.GitParcelTestUtils;
import io.github.leawind.inventory.misc.TempDirectory;
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();

  public void testSavers(GameTestHelpMore helper) throws Exception {
    GitParcelTestUtils.forEachFormatCombination(
        ParcelFormatRegistry.INSTANCE.streamSavers().toList(),
        (format, rotation, mirror) -> {
          try (var tempDir = new TempDirectory(GitParcel.MOD_ID)) {
            LOGGER.info(
                "Testing format {} with rotation={} mirror={}", format.spec(), rotation, mirror);

            ParcelStorage.save(
                format,
                helper.getLevel(),
                helper.getBoundingBox(),
                rotation,
                mirror,
                null,
                tempDir.getPath(),
                true);

            LOGGER.info("  Passed: rotation={}, mirror={}", rotation, mirror);
          }
        });

    helper.succeed();
  }
}
