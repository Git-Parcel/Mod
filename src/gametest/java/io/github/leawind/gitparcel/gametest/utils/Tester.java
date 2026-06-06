package io.github.leawind.gitparcel.gametest.utils;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestHelper;
import org.slf4j.Logger;

public interface Tester {
  Logger LOGGER = LogUtils.getLogger();

  void test(GameTestHelpMore helper) throws Exception;

  static void test(GameTestHelper helper, Tester tester) throws Exception {
    try {
      tester.test(GameTestHelpMore.from(helper));
    } catch (Exception e) {
      LOGGER.error("Test failed", e);
      throw e;
    }
  }
}
