package io.github.leawind.gitparcel.common.minecraft.logic.version;

import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.minecraft.SharedConstants;

/** Access to Minecraft version metadata whose shape changes between supported versions. */
@VersionSensitive("Minecraft version and data-version API")
public final class MinecraftVersion {
  private MinecraftVersion() {}

  /** Returns the data version to persist for data created by the active Minecraft runtime. */
  public static int currentDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*?} else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
     *//*?}*/
  }
}
