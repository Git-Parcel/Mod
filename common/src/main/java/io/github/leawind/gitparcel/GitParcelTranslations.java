package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.utils.permission.PermissionType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public class GitParcelTranslations {
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

  public static <T> MutableComponent of(PermissionType<T> type) {
    return of("gitparcel.permission." + type.name());
  }
}
