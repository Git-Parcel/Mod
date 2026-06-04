package io.github.leawind.gitparcel.platform.api;

import java.util.ServiceLoader;
import net.minecraft.SharedConstants;

public interface PlatformHelper {
  PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class).findFirst().orElseThrow();

  default int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*?} else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
     */
    /*?}*/
  }
}
