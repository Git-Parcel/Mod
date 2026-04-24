package io.github.leawind.gitparcel.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameTestHelper.class)
public interface AccessGameTestHelper {
  @Accessor("testInfo")
  GameTestInfo getTestInfo();

  @Accessor("testInfo")
  void setTestInfo(GameTestInfo value);

  @Accessor("finalCheckAdded")
  boolean getFinalCheckAdded();

  @Accessor("finalCheckAdded")
  void setFinalCheckAdded(boolean value);
}
