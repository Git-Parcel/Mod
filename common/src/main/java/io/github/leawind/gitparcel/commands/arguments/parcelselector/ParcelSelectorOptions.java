package io.github.leawind.gitparcel.commands.arguments.parcelselector;

import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.GitParcelTranslations;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * @see EntitySelectorOptions
 */
public class ParcelSelectorOptions {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<String, ParcelSelectorOptions.Option> OPTIONS = Maps.newHashMap();

  public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION =
      new DynamicCommandExceptionType(
          arg -> GitParcelTranslations.of("argument.gitparcel.parcel.options.unknown", arg));
  public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION =
      new DynamicCommandExceptionType(
          arg -> GitParcelTranslations.esc("argument.gitparcel.parcel.options.inapplicable", arg));
  public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.options.limit.toosmall"));

  // TODO remove predicate field
  private static void register(
      String id, Modifier handler, Predicate<ParcelSelectorParser> predicate, Component tooltip) {
    OPTIONS.put(id, new Option(handler, predicate, tooltip));
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
        parser -> true,
        GitParcelTranslations.of("argument.gitparcel.parcel.options.name.description"));

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
        parser -> true,
        GitParcelTranslations.of("argument.gitparcel.parcel.options.limit.description"));
  }

  public static Modifier get(ParcelSelectorParser parser, String id, int cursor)
      throws CommandSyntaxException {
    var option = OPTIONS.get(id);
    if (option != null) {
      if (option.canUse.test(parser)) {
        return option.modifier;
      } else {
        throw ERROR_INAPPLICABLE_OPTION.createWithContext(parser.getReader(), id);
      }
    } else {
      parser.getReader().setCursor(cursor);
      throw ERROR_UNKNOWN_OPTION.createWithContext(parser.getReader(), id);
    }
  }

  public static void suggestNames(ParcelSelectorParser parser, SuggestionsBuilder builder) {
    String remainingLower = builder.getRemaining().toLowerCase(Locale.ROOT);
    var usedOptions = parser.getUsedOptions();

    for (var entry : OPTIONS.entrySet()) {
      if (entry.getValue().canUse.test(parser)
          && !usedOptions.contains(entry.getKey())
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
      if (entry.getValue().canUse.test(parser) && !usedOptions.contains(entry.getKey())) {
        return true;
      }
    }
    return false;
  }

  record Option(Modifier modifier, Predicate<ParcelSelectorParser> canUse, Component description) {}
}
