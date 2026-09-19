package io.github.leawind.gitparcel.common.platform.forge;

/*? if forge {*/
/*
import com.mojang.brigadier.arguments.ArgumentType;
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.platform.api.CommandArgumentTypeRegistrar;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public final class CommandArgumentTypeRegistrarImpl implements CommandArgumentTypeRegistrar {
  private final DeferredRegister<ArgumentTypeInfo<?, ?>> entries =
      DeferredRegister.create(ForgeRegistries.Keys.COMMAND_ARGUMENT_TYPES, GitParcel.MOD_ID);

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
