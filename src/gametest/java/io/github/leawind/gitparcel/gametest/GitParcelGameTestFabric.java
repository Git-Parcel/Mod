package io.github.leawind.gitparcel.gametest;

import io.github.leawind.gitparcel.testutils.Tester;
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

  @GameTest(structure = "gitparcel:swamp_hut-7x8x9")
  public void testSaveAndLoad(GameTestHelper helper) throws Exception {
    Tester.test(helper, super::testSaveAndLoad);
  }
}
