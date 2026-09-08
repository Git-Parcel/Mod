package io.github.leawind.gitparcel.gametest;

import io.github.leawind.gitparcel.gametest.utils.Tester;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.jspecify.annotations.NonNull;

public class GitParcelGameTestFabric extends GitParcelGameTest implements CustomTestMethodInvoker {
  @Override
  public void invokeTestMethod(@NonNull GameTestHelper helper, Method method)
      throws ReflectiveOperationException {
    method.invoke(this, helper);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9")
  public void testParcelLifecycle(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testParcelLifecycle);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9")
  public void testSaveAndLoad(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testSaveAndLoad);
  }

  @GameTest(structure = "gametest:layers_6-9x30x10", maxTicks = 200)
  public void testLayeredSnapshotBranching(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testLayeredSnapshotBranching);
  }

  @GameTest(structure = "gametest:layers_6-9x30x10", maxTicks = 200)
  public void testLayeredD16RoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testLayeredD16RoundTrip);
  }

  @GameTest(structure = "gametest:normal-48x48x48", maxTicks = 400)
  public void testNormalSnapshotRoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testNormalSnapshotRoundTrip);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  public void testSaveThenRestoreKeepsProtectiveSnapshot(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testSaveThenRestoreKeepsProtectiveSnapshot);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  public void testIdenticalContentResaveStillCommits(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testIdenticalContentResaveStillCommits);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  public void testEntityRoundTripCharacteristics(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testEntityRoundTripCharacteristics);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  public void testMapItemCharacteristics(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testMapItemCharacteristics);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  public void testAttachmentRoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testAttachmentRoundTrip);
  }

  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  public void testBeehiveFlowerPosFollowsParcel(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testBeehiveFlowerPosFollowsParcel);
  }
}
