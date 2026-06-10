package io.github.leawind.gitparcel.core.impl;

import io.github.leawind.gitparcel.core.api.GitParcel;
import net.minecraft.resources.Identifier;

public final class GitParcelUtils {
  private GitParcelUtils() {}

  public static Identifier identifier(String path) {
    return Identifier.fromNamespaceAndPath(GitParcel.MOD_ID, path);
  }
}
