package io.github.leawind.gitparcel.common.platform.api;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;

/**
 * Registers command argument serializers without exposing loader-specific registry APIs to common
 * initialization code.
 */
public interface CommandArgumentTypeRegistrar {

  <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(
      String name, Class<A> argumentClass, ArgumentTypeInfo<A, T> info);
}
