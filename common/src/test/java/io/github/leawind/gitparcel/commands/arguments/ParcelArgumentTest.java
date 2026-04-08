package io.github.leawind.gitparcel.commands.arguments;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.commands.arguments.parcelselector.ParcelSelectorParser;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ParcelArgumentTest {

  private static Suggestions suggest(String input) {
    var builder = new SuggestionsBuilder(input, 0);
    ParcelSelectorParser parser = new ParcelSelectorParser(new StringReader(input));
    try {
      parser.parse();
    } catch (CommandSyntaxException ignored) {
    }
    return parser.fillSuggestions(builder, b -> {}).join();
  }

  private static Set<String> applied(String input) {
    var suggestions = suggest(input);
    return suggestions.getList().stream()
        .map(suggestion -> suggestion.apply(input))
        .collect(Collectors.toSet());
  }

  @Test
  void testCompleted() {
    assertEquals(Set.of(), applied("#p"));
    assertEquals(Set.of(), applied("#s"));

    assertEquals(Set.of(), applied("#a[]"));
    assertEquals(Set.of(), applied("#p[]"));
    assertEquals(Set.of(), applied("#s[]"));

    assertEquals(Set.of(), applied("#a[name=Test]"));
    assertEquals(Set.of(), applied("#a[limit=2]"));
    assertEquals(Set.of(), applied("#a[limit=2,name=Test]"));
  }

  @Test
  void testEmpty() {
    assertEquals(Set.of("#a", "#p", "#s"), applied(""));
  }

  @Test
  void testSharp() {
    assertEquals(Set.of("#a", "#p", "#s"), applied("#"));
  }

  @Test
  void testPreKey() {
    assertEquals(Set.of("#a[name=", "#a[limit="), applied("#a["));
  }

  @Test
  void testAfterKeyValue() {
    {
      var input = "#a[name=\"House\"";
      assertEquals(Set.of(input + "]", input + ","), applied(input));
    }

    {
      var input = "#a[limit=3";
      assertEquals(Set.of(input + "]", input + ","), applied(input));
    }
  }

  @Test
  void testAfterComma() {
    {
      var input = "#a[name=\"House\",";
      assertEquals(Set.of(input + "limit="), applied(input));
    }
    {
      var input = "#a[limit=3,";
      assertEquals(Set.of(input + "name="), applied(input));
    }
    {
      var input = "#a[name=\"House\",limit=3";
      assertEquals(Set.of(input + "]"), applied(input));
    }
    {
      var input = "#a[name=\"House\",limit=3,";
      assertEquals(Set.of(), applied(input));
    }
  }
}
