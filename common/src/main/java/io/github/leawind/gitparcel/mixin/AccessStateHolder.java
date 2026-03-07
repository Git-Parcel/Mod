package io.github.leawind.gitparcel.mixin;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("unused")
@Mixin(StateHolder.class)
public interface AccessStateHolder {
  @Accessor("PROPERTY_ENTRY_TO_STRING_FUNCTION")
  static Function<Map.Entry<Property<?>, Comparable<?>>, String>
      getPropertyEntryToStringFunction() {
    throw new AssertionError();
  }
}
