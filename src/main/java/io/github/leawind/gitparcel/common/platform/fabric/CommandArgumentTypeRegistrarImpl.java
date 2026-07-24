/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import io.github.leawind.gitparcel.common.platform.api.CommandArgumentTypeRegistrar;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;

public final class CommandArgumentTypeRegistrarImpl implements CommandArgumentTypeRegistrar {

  @Override
  public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(
      String name, Class<A> argumentClass, ArgumentTypeInfo<A, T> info) {
    ArgumentTypeRegistry.registerArgumentType(
        GitParcelUtils.identifier(name), argumentClass, info);
  }
}
/*?}*/
