package io.github.leawind.gitparcel;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class GitParcelTranslations {
  /**
   * @see net.minecraft.network.chat.Component#translatable(String)
   */
  public static MutableComponent of(String key) {
    return MutableComponent.create(
        new TranslatableContents(key, null, TranslatableContents.NO_ARGS));
  }

  /**
   * @see net.minecraft.network.chat.Component#translatable(String, Object...)
   */
  public static MutableComponent of(String key, Object... args) {
    return MutableComponent.create(new TranslatableContents(key, null, args));
  }

  public static MutableComponent esc(String key, Object... args) {
    return Component.translatable(key, args);
  }
}
