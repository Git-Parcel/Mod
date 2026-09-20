package io.github.leawind.gitparcel.gametest;

import io.github.leawind.gitparcel.gametest.utils.Tester;
import java.lang.reflect.Method;
/*? if >=26.1 {*/
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
/*?} else {*/
/*import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
*//*?}*/
import net.minecraft.gametest.framework.GameTestHelper;
/*? if >=26.1 {*/
import org.jspecify.annotations.NonNull;
/*?}*/

/*
 * 26.x registers through fabric's own @GameTest annotation and CustomTestMethodInvoker; 1.20.1's
 * fabric-gametest-api only ships the FabricGameTest entrypoint, which pairs with the vanilla
 * @GameTest(template = ...) annotation. The test bodies live in the loader-neutral base class.
 */
/*? if >=26.1 {*/
public class GitParcelGameTestFabric extends GitParcelGameTest implements CustomTestMethodInvoker {
  @Override
  public void invokeTestMethod(@NonNull GameTestHelper helper, Method method)
      throws ReflectiveOperationException {
    method.invoke(this, helper);
  }
/*?} else {*/
/*public class GitParcelGameTestFabric extends GitParcelGameTest implements FabricGameTest {
  @Override
  public void invokeTestMethod(GameTestHelper helper, Method method) {
    try {
      method.invoke(this, helper);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
*//*?}*/

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9")
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9")
  *//*?}*/
  public void testParcelLifecycle(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testParcelLifecycle);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9")
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9")
  *//*?}*/
  public void testSaveAndLoad(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testSaveAndLoad);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:layers_6-9x30x10", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:layers_6-9x30x10", timeoutTicks = 200)
  *//*?}*/
  public void testLayeredSnapshotBranching(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testLayeredSnapshotBranching);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:layers_6-9x30x10", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:layers_6-9x30x10", timeoutTicks = 200)
  *//*?}*/
  public void testLayeredD16RoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testLayeredD16RoundTrip);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:normal-48x48x48", maxTicks = 400)
  /*?} else {*/
  /*@GameTest(template = "gametest:normal-48x48x48", timeoutTicks = 400)
  *//*?}*/
  public void testNormalSnapshotRoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testNormalSnapshotRoundTrip);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testSaveThenRestoreKeepsProtectiveSnapshot(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testSaveThenRestoreKeepsProtectiveSnapshot);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testIdenticalContentResaveStillCommits(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testIdenticalContentResaveStillCommits);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testEntityRoundTripCharacteristics(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testEntityRoundTripCharacteristics);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testResizeRecapturesAdjustedExtent(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testResizeRecapturesAdjustedExtent);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testDeterministicCapture(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testDeterministicCapture);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testScheduledTickRoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testScheduledTickRoundTrip);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:normal-48x48x48", maxTicks = 400)
  /*?} else {*/
  /*@GameTest(template = "gametest:normal-48x48x48", timeoutTicks = 400)
  *//*?}*/
  public void testScheduledTickRotatedMigration(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testScheduledTickRotatedMigration);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testItemFrameOrientationFollowsPlacement(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testItemFrameOrientationFollowsPlacement);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testMapItemCharacteristics(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testMapItemCharacteristics);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testAttachmentRoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testAttachmentRoundTrip);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testBeehiveFlowerPosFollowsParcel(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testBeehiveFlowerPosFollowsParcel);
  }

  /*? if >=26.1 {*/
  @GameTest(structure = "gametest:swamp_hut-7x8x9", maxTicks = 200)
  /*?} else {*/
  /*@GameTest(template = "gametest:swamp_hut-7x8x9", timeoutTicks = 200)
  *//*?}*/
  public void testCaptureContributorRoundTrip(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testCaptureContributorRoundTrip);
  }
}
