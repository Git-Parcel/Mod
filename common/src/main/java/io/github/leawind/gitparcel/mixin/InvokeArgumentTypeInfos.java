package io.github.leawind.gitparcel.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("unused")
@Mixin(ArgumentTypeInfos.class)
public interface InvokeArgumentTypeInfos {

  @Invoker("register")
  static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>>
      ArgumentTypeInfo<A, T> register(
          Registry<ArgumentTypeInfo<?, ?>> registry,
          String id,
          Class<? extends A> clazz,
          ArgumentTypeInfo<A, T> info) {
    throw new AssertionError();
  }
}
