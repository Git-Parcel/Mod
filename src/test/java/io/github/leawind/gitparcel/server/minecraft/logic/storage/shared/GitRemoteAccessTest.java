package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GitRemoteAccessTest {
  @Test
  void acceptsCredentialFreeHttpsRepositoryUrl() {
    assertEquals(
        "https://github.com/Git-Parcel/Mod.git",
        GitRemoteAccess.validateRemoteUrl(
            "https://github.com/Git-Parcel/Mod.git"));
  }

  @Test
  void rejectsUnsafeOrSecretBearingUrls() {
    assertThrows(
        IllegalArgumentException.class,
        () -> GitRemoteAccess.validateRemoteUrl("file:///etc/passwd"));
    assertThrows(
        IllegalArgumentException.class,
        () -> GitRemoteAccess.validateRemoteUrl("http://example.com/repo.git"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GitRemoteAccess.validateRemoteUrl(
                "https://token@example.com/repo.git"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GitRemoteAccess.validateRemoteUrl(
                "https://example.com/repo.git?token=secret"));
  }
}
