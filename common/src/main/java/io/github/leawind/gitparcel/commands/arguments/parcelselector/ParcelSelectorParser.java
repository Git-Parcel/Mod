package io.github.leawind.gitparcel.commands.arguments.parcelselector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.world.Parcel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * @see EntitySelectorParser
 */
public final class ParcelSelectorParser {
  public static final char SYNTAX_SELECTOR_START = '#';
  public static final String SYNTAX_SELECTOR_START_STRING = "#";
  private static final char SYNTAX_OPTIONS_START = '[';
  private static final char SYNTAX_OPTIONS_END = ']';
  public static final char SYNTAX_KEY_VALUE_SEPARATOR = '=';
  private static final char SYNTAX_OPTIONS_SEPARATOR = ',';
  public static final char SYNTAX_NOT = '!';

  private static final char SELECTOR_ARBITRARY = 'a';
  private static final char SELECTOR_NEAREST = 'p';
  private static final char SELECTOR_SIGHTED = 's';

  public static final SimpleCommandExceptionType ERROR_INVALID_NAME_OR_UUID =
      new SimpleCommandExceptionType(GitParcelTranslations.of("argument.gitparcel.parcel.invalid"));

  public static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_TYPE =
      new DynamicCommandExceptionType(
          name -> GitParcelTranslations.esc("argument.gitparcel.parcel.selector.unknown", name));

  public static final SimpleCommandExceptionType ERROR_MISSING_SELECTOR_TYPE =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.selector.missing"));

  public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_OPTIONS =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.options.expect_end"));

  public static final DynamicCommandExceptionType ERROR_EXPECTED_OPTION_VALUE =
      new DynamicCommandExceptionType(
          arg -> GitParcelTranslations.esc("argument.gitparcel.parcel.options.valueless", arg));

  public static final BiConsumer<Vec3, List<Parcel>> ORDER_ARBITRARY = (pos, list) -> {};
  public static final BiConsumer<Vec3, List<Parcel>> ORDER_NEAREST =
      (pos, parcels) ->
          parcels.sort(
              Comparator.comparingDouble(
                  parcel -> AABB.of(parcel.getBoundingBox()).distanceToSqr(pos)));

  public static final BiFunction<
          SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>>
      SUGGEST_NOTHING = (builder, suggester) -> builder.buildFuture();

  static {
    ParcelSelectorOptions.bootStrap();
  }

  private final StringReader reader;
  private int startPosition;

  /** Added by options */
  private BiFunction<
          SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>>
      suggestions = SUGGEST_NOTHING;

  private int maxResults;
  private final List<Predicate<Parcel>> predicates = new ArrayList<>();
  private final java.util.HashSet<String> usedOptions = new java.util.HashSet<>();
  private BiConsumer<Vec3, List<Parcel>> order = ORDER_ARBITRARY;
  private boolean isWorldLimited = true;
  private @Nullable String name;
  private @Nullable UUID uuid;

  public ParcelSelectorParser(StringReader reader) {
    this.reader = reader;
  }

  public ParcelSelector getSelector() {
    return new ParcelSelector(
        maxResults, List.copyOf(predicates), order, isWorldLimited, name, uuid);
  }

  public ParcelSelector parse() throws CommandSyntaxException {
    startPosition = reader.getCursor();
    suggestions = this::suggestNameOrSelector;
    if (reader.canRead() && reader.peek() == SYNTAX_SELECTOR_START) {
      reader.skip();
      parseSelector();
    } else {
      parseNameOrUUID();
    }
    finalizePredicates();
    return this.getSelector();
  }

  private void finalizePredicates() {
    // do nothing
  }

  private void parseSelector() throws CommandSyntaxException {
    this.suggestions = this::suggestSelector;
    if (!reader.canRead()) {
      throw ERROR_MISSING_SELECTOR_TYPE.createWithContext(reader);
    } else {
      int cursor = reader.getCursor();
      char ch = reader.read();

      switch (ch) {
        case SELECTOR_ARBITRARY -> maxResults = Integer.MAX_VALUE;
        case SELECTOR_NEAREST -> {
          maxResults = 1;
          order = ORDER_NEAREST;
        }
        case SELECTOR_SIGHTED -> maxResults = 1;
        default -> {
          reader.setCursor(cursor);
          throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(
              reader, SYNTAX_SELECTOR_START_STRING + ch);
        }
      }

      this.suggestions = this::suggestOpenOptions;
      if (reader.canRead() && reader.peek() == SYNTAX_OPTIONS_START) {
        reader.skip();
        suggestions = this::suggestOptionsKeyOrClose;
        parseOptions();
      } else {
        // Full valid selector with no options, no further suggestions
        this.suggestions = SUGGEST_NOTHING;
      }
    }
  }

  private void parseOptions() throws CommandSyntaxException {
    suggestions = this::suggestOptionsKey;
    reader.skipWhitespace();

    while (reader.canRead() && reader.peek() != SYNTAX_OPTIONS_END) {
      reader.skipWhitespace();
      int cursor = reader.getCursor();
      String str = reader.readString();
      ParcelSelectorOptions.Modifier modifier = ParcelSelectorOptions.get(this, str, cursor);
      usedOptions.add(str);

      reader.skipWhitespace();

      if (!reader.canRead() || reader.peek() != SYNTAX_KEY_VALUE_SEPARATOR) {
        reader.setCursor(cursor);
        throw ERROR_EXPECTED_OPTION_VALUE.createWithContext(reader, str);
      }

      reader.skip();
      reader.skipWhitespace();
      this.suggestions = SUGGEST_NOTHING;
      modifier.handle(this);
      reader.skipWhitespace();
      this.suggestions = this::suggestOptionsNextOrClose;
      if (reader.canRead()) {
        if (reader.peek() != SYNTAX_OPTIONS_SEPARATOR) {
          if (reader.peek() != SYNTAX_OPTIONS_END) {
            throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(reader);
          }
          break;
        }

        reader.skip();
        this.suggestions = this::suggestOptionsKey;
      }
    }

    if (reader.canRead()) {
      reader.skip();
      this.suggestions = SUGGEST_NOTHING;
    } else {
      throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(reader);
    }
  }

  public boolean shouldInvertValue() {
    this.reader.skipWhitespace();
    if (this.reader.canRead() && this.reader.peek() == SYNTAX_NOT) {
      this.reader.skip();
      this.reader.skipWhitespace();
      return true;
    } else {
      return false;
    }
  }

  private void parseNameOrUUID() throws CommandSyntaxException {
    if (reader.canRead()) {
      suggestions = this::suggestName;
    }

    int cursor = reader.getCursor();
    String str = reader.readString();

    try {
      uuid = UUID.fromString(str);
    } catch (IllegalArgumentException ignored) {
      // TODO 16?
      if (str.isEmpty() || str.length() > 16) {
        reader.setCursor(cursor);
        throw ERROR_INVALID_NAME_OR_UUID.createWithContext(reader);
      }

      this.name = str;
    }

    this.maxResults = 1;
  }

  private static void fillSelectorSuggestions(SuggestionsBuilder builder) {
    builder.suggest(
        SYNTAX_SELECTOR_START_STRING + SELECTOR_ARBITRARY,
        GitParcelTranslations.of("argument.gitparcel.parcel.selector.arbitrary"));
    builder.suggest(
        SYNTAX_SELECTOR_START_STRING + SELECTOR_NEAREST,
        GitParcelTranslations.of("argument.gitparcel.parcel.selector.nearest"));
    builder.suggest(
        SYNTAX_SELECTOR_START_STRING + SELECTOR_SIGHTED,
        GitParcelTranslations.of("argument.gitparcel.parcel.selector.sighted"));
  }

  private CompletableFuture<Suggestions> suggestNameOrSelector(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    suggester.accept(builder);
    fillSelectorSuggestions(builder);
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestName(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    SuggestionsBuilder suggestionsbuilder = builder.createOffset(startPosition);
    suggester.accept(suggestionsbuilder);
    return builder.add(suggestionsbuilder).buildFuture();
  }

  private CompletableFuture<Suggestions> suggestSelector(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    SuggestionsBuilder suggestionsbuilder = builder.createOffset(builder.getStart() - 1);
    fillSelectorSuggestions(suggestionsbuilder);
    return suggestionsbuilder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestOpenOptions(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    builder.suggest(SYNTAX_OPTIONS_START);
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestOptionsKeyOrClose(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    builder.suggest(String.valueOf(SYNTAX_OPTIONS_END));
    ParcelSelectorOptions.suggestNames(this, builder);
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestOptionsKey(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    ParcelSelectorOptions.suggestNames(this, builder);
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestOptionsNextOrClose(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    // Only suggest comma if there are still unused options available
    if (ParcelSelectorOptions.hasAvailableOptions(this)) {
      builder.suggest(String.valueOf(SYNTAX_OPTIONS_SEPARATOR));
    }
    builder.suggest(String.valueOf(SYNTAX_OPTIONS_END));
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestEquals(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    builder.suggest(String.valueOf(SYNTAX_KEY_VALUE_SEPARATOR));
    return builder.buildFuture();
  }

  public void setSuggestions(
      BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>>
          suggestionHandler) {
    this.suggestions = suggestionHandler;
  }

  public CompletableFuture<Suggestions> fillSuggestions(
      SuggestionsBuilder builder, Consumer<SuggestionsBuilder> suggester) {
    return this.suggestions.apply(builder.createOffset(reader.getCursor()), suggester);
  }

  public StringReader getReader() {
    return this.reader;
  }

  public void addPredicate(Predicate<Parcel> predicate) {
    predicates.add(predicate);
  }

  public void setWorldLimited(boolean isWorldLimited) {
    this.isWorldLimited = isWorldLimited;
  }

  public int getMaxResults() {
    return this.maxResults;
  }

  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  public BiConsumer<Vec3, List<Parcel>> getOrder() {
    return this.order;
  }

  public void setOrder(BiConsumer<Vec3, List<Parcel>> order) {
    this.order = order;
  }

  public java.util.Set<String> getUsedOptions() {
    return usedOptions;
  }
}
