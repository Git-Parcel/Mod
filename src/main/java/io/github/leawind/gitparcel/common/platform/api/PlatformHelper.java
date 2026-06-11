package io.github.leawind.gitparcel.common.platform.api;

import net.minecraft.SharedConstants;

public interface PlatformHelper {

  default int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*?} else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
     */
    /*?}*/
  }

  boolean isDevelopmentEnvironment();
}
