package io.github.leawind.gitparcel.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.Parcel;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;

public class ParcelArgument implements ArgumentType<ParcelArgument.ParcelSelector> {
  private static final Collection<String> EXAMPLES =
      List.of("dd12be42-52a9-4a91-a8a1-11c01849e498");

  public static final SimpleCommandExceptionType ERROR_TOO_MANY =
      new SimpleCommandExceptionType(GitParcelTranslations.of("argument.gitparcel.parcel.toomany"));
  public static final SimpleCommandExceptionType ERROR_PARCEL_NOT_FOUND =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.notfound"));

  public static ParcelArgument parcel() {
    return new ParcelArgument();
  }

  public static Parcel getParcel(CommandContext<CommandSourceStack> context, String name)
      throws CommandSyntaxException {
    return context.getArgument(name, ParcelSelector.class).get(context.getSource());
  }

  @Override
  public ParcelSelector parse(StringReader reader) throws CommandSyntaxException {
    String input = reader.readString();
    UUID uuid = UUID.fromString(input);
    return new ParcelSelector(uuid);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  /** A simple wrapper to represent either UUID-based or key-based lookup */
  public static class ParcelSelector {
    private final UUID uuid;

    private ParcelSelector(UUID uuid) {
      this.uuid = uuid;
    }

    /**
     * Finds a {@link Parcel} based on the provided {@link CommandSourceStack}.
     *
     * <p>If the parcel is found, it is returned. Otherwise, an exception is thrown.
     *
     * @param source The command source stack providing the context for the lookup.
     * @return The found {@link Parcel}.
     * @throws CommandSyntaxException If the parcel is not found
     */
    public Parcel get(CommandSourceStack source) throws CommandSyntaxException {
      var serverLevel = source.getLevel();
      var savedData = GitParcelLevelSavedData.get(serverLevel);
      var parcel = savedData.getParcel(uuid);

      if (parcel == null) {
        throw ERROR_PARCEL_NOT_FOUND.create();
      }

      return parcel;
    }
  }
}
