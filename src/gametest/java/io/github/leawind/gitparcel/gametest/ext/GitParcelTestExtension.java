package io.github.leawind.gitparcel.gametest.ext;

import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;

/**
 * Extension only present in the game-test runtime. Exercises the third-party surface end to end:
 * a processor that collects an attachment during capture and consumes it during restore.
 */
public final class GitParcelTestExtension implements GitParcelExtension {
  public static final String ID = "gitparceltest:gametest";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void register(ParcelExtensionRegistrar registrar) {
    registrar.registerAttachmentType(MarkerAttachmentType.INSTANCE);
    registrar.registerProcessor(MarkerRecordProcessor.INSTANCE);
  }
}
