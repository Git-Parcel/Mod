package io.github.leawind.gitparcel.common.platform.neoforge;

/*? if neoforge {*/
/*
import com.mojang.brigadier.arguments.ArgumentType;
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.platform.api.CommandArgumentTypeRegistrar;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CommandArgumentTypeRegistrarImpl implements CommandArgumentTypeRegistrar {
  private final DeferredRegister<ArgumentTypeInfo<?, ?>> entries =
      DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, GitParcel.MOD_ID);

  @Override
  public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(
      String name, Class<A> argumentClass, ArgumentTypeInfo<A, T> info) {
    entries.register(name, () -> ArgumentTypeInfos.registerByClass(argumentClass, info));
  }

  public void attach(IEventBus modEventBus) {
    entries.register(modEventBus);
  }
}
*//*?}*/
