package io.github.leawind.gitparcel.core.mc.commands.arguments.parcelselector;

import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.core.util.Translations;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * @see EntitySelectorOptions
 */
public class ParcelSelectorOptions {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<String, Option> OPTIONS = Maps.newHashMap();

  public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION =
      new DynamicCommandExceptionType(
          arg -> Translations.of("argument.gitparcel.parcel.options.unknown", arg));
  public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL =
      new SimpleCommandExceptionType(
          Translations.of("argument.gitparcel.parcel.options.limit.toosmall"));

  private static void register(String id, Modifier handler, Component tooltip) {
    OPTIONS.put(id, new Option(handler, tooltip));
  }

  @FunctionalInterface
  public interface Modifier {
    void handle(ParcelSelectorParser parser) throws CommandSyntaxException;
  }

  public static void bootStrap() {
    if (!OPTIONS.isEmpty()) {
      return;
    }

    register(
        "name",
        parser -> {
          boolean shouldInvertValue = parser.shouldInvertValue();
          String name = parser.getReader().readString();
          parser.addPredicate(parcel -> name.equals(parcel.meta().name()) != shouldInvertValue);
          parser.setMaxResults(1);
        },
        Translations.of("argument.gitparcel.parcel.options.name.description"));

    register(
        "limit",
        parser -> {
          int cursor = parser.getReader().getCursor();
          int limit = parser.getReader().readInt();
          if (limit < 1) {
            parser.getReader().setCursor(cursor);
            throw ERROR_LIMIT_TOO_SMALL.createWithContext(parser.getReader());
          }
          parser.setMaxResults(Math.min(parser.getMaxResults(), limit));
        },
        Translations.of("argument.gitparcel.parcel.options.limit.description"));
  }

  public static Modifier get(ParcelSelectorParser parser, String id, int cursor)
      throws CommandSyntaxException {
    var option = OPTIONS.get(id);
    if (option != null) {
      return option.modifier;
    } else {
      parser.getReader().setCursor(cursor);
      throw ERROR_UNKNOWN_OPTION.createWithContext(parser.getReader(), id);
    }
  }

  public static void suggestNames(ParcelSelectorParser parser, SuggestionsBuilder builder) {
    String remainingLower = builder.getRemaining().toLowerCase(Locale.ROOT);
    var usedOptions = parser.getUsedOptions();

    for (var entry : OPTIONS.entrySet()) {
      if (!usedOptions.contains(entry.getKey())
          && entry.getKey().toLowerCase(Locale.ROOT).startsWith(remainingLower)) {
        builder.suggest(
            entry.getKey() + ParcelSelectorParser.SYNTAX_KEY_VALUE_SEPARATOR,
            entry.getValue().description);
      }
    }
  }

  public static boolean hasAvailableOptions(ParcelSelectorParser parser) {
    var usedOptions = parser.getUsedOptions();
    for (var entry : OPTIONS.entrySet()) {
      if (!usedOptions.contains(entry.getKey())) {
        return true;
      }
    }
    return false;
  }

  record Option(Modifier modifier, Component description) {}
}
