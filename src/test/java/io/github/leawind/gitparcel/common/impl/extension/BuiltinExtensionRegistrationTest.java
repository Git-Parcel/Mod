package io.github.leawind.gitparcel.common.impl.extension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientFieldRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.BuiltinExtension;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.TransientFieldProcessor;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Pins the audited vanilla declarations. Key names are era-dependent (26.x snake_case codec forms,
 * 1.20.1 PascalCase and flat-axes compounds), so each node asserts its own era's keys plus the
 * era-stable ones.
 */
class BuiltinExtensionRegistrationTest extends AbstractMinecraftTest {

  @Test
  void declaresAuditedSpatialEdgeFields() {
    registerOnce();
    var fields = ParcelCoordinateFieldRegistry.get().fields();

    /*? if >=26.1 {*/
    for (String path :
        new String[] {
          "home_pos", "sleeping_pos", "anchor_pos", "hive_pos", "flower_pos", "patrol_target",
          "bound_pos", "wander_target", "beam_target"
        }) {
      assertTrue(
          declaresEntity(fields, path, null, ParcelCoordinateField.Encoding.BLOCK_POS),
          "missing declaration for " + path);
    }
    assertTrue(
        declaresBe(fields, "exit_portal", "end_gateway", ParcelCoordinateField.Encoding.BLOCK_POS));
    assertTrue(
        declaresBe(
            fields, "bees[].entity_data.flower_pos", "beehive", ParcelCoordinateField.Encoding.BLOCK_POS));
    /*?} else {*/
    /*assertTrue(
        declaresEntity(fields, "Leash", null, ParcelCoordinateField.Encoding.BLOCK_POS_XYZ));
    assertTrue(
        declaresEntity(fields, "Sleeping", null, ParcelCoordinateField.Encoding.BLOCK_POS_AXES));
    for (String path : new String[] {"HivePos", "FlowerPos", "PatrolTarget", "WanderTarget", "BeamTarget"}) {
      assertTrue(
          declaresEntity(fields, path, null, ParcelCoordinateField.Encoding.BLOCK_POS),
          "missing declaration for " + path);
    }
    assertTrue(
        declaresEntity(fields, "A", "phantom", ParcelCoordinateField.Encoding.BLOCK_POS_AXES));
    assertTrue(
        declaresEntity(fields, "HomePos", "turtle", ParcelCoordinateField.Encoding.BLOCK_POS_AXES));
    assertTrue(
        declaresEntity(fields, "TravelPos", "turtle", ParcelCoordinateField.Encoding.BLOCK_POS_AXES));
    assertTrue(
        declaresEntity(fields, "Facing", "painting", ParcelCoordinateField.Encoding.DIRECTION));
    assertTrue(
        declaresBe(fields, "ExitPortal", "end_gateway", ParcelCoordinateField.Encoding.BLOCK_POS));
    assertTrue(
        declaresBe(
            fields, "Bees[].EntityData.FlowerPos", "beehive", ParcelCoordinateField.Encoding.BLOCK_POS));
    *//*? }*/

    // Era-stable declarations: shulker attach face (key corrected from the never-written
    // "Facing"), structure-block origin axes, and vibration-listener positions.
    assertTrue(
        declaresEntity(fields, "AttachFace", "shulker", ParcelCoordinateField.Encoding.DIRECTION));
    assertTrue(
        declaresBe(
            fields, "pos", "structure_block", ParcelCoordinateField.Encoding.BLOCK_POS_AXES));
    for (String path : new String[] {"listener.event.pos", "listener.selector.event.pos"}) {
      assertTrue(
          declaresEntity(fields, path, null, ParcelCoordinateField.Encoding.POSITION),
          "missing declaration for " + path);
      assertTrue(
          declaresBe(fields, path, null, ParcelCoordinateField.Encoding.POSITION),
          "missing block-entity declaration for " + path);
    }
  }

  @Test
  void registersTransientFieldProcessor() {
    registerOnce();
    assertNotNull(
        ParcelRecordProcessorRegistry.get().get(TransientFieldProcessor.ID),
        "the builtin extension must register the transient-field processor");
  }

  /** Pins the audited game-time offset declarations. */
  @Test
  void declaresAuditedGameTimeOffsetFields() {
    registerOnce();
    var fields = ParcelTransientFieldRegistry.get().fields();

    assertTrue(
        declaresTransient(fields, "server_data.state_updating_resumes_at", "vault",
            ParcelTransientField.Kind.OFFSET_GAME_TIME));
    for (String path : new String[] {"next_mob_spawns_at", "cooldown_ends_at"}) {
      assertTrue(
          declaresTransient(fields, path, "trial_spawner", ParcelTransientField.Kind.OFFSET_GAME_TIME),
          "missing declaration for " + path);
    }
    for (String path : new String[] {"attack.timestamp", "interaction.timestamp"}) {
      assertTrue(
          declaresTransient(fields, path, "interaction", ParcelTransientField.Kind.OFFSET_GAME_TIME),
          "missing declaration for " + path);
    }
    assertTrue(
        declaresTransient(
            fields, "listener.selector.tick", null, ParcelTransientField.Kind.OFFSET_GAME_TIME),
        "the vibration selector tick must travel as a game-time offset");
    // Time-noise eliminations from the audit.
    for (String path : new String[] {"Delay", "TransferCooldown"}) {
      assertTrue(
          fields.stream()
              .anyMatch(
                  field ->
                      field.path().equals(path)
                          && field.kind() == ParcelTransientField.Kind.ELIMINATE),
          "missing elimination for " + path);
    }
    assertTrue(
        fields.stream()
            .anyMatch(
                field ->
                    field.path().equals("PickupDelay")
                        && field.target() == ParcelTransientField.Target.ENTITY
                        && field.kind() == ParcelTransientField.Kind.ELIMINATE),
        "missing PickupDelay elimination");
    /*? if >=26.1 {*/
    assertTrue(
        fields.stream()
            .anyMatch(
                field ->
                    field.path().equals("ticks_since_song_started")
                        && field.kind() == ParcelTransientField.Kind.ELIMINATE));
    /*?} else {*/
    /*for (String path : new String[] {"IsPlaying", "RecordStartTick", "TickCount"}) {
      assertTrue(
          fields.stream()
              .anyMatch(
                  field -> field.path().equals(path) && field.kind() == ParcelTransientField.Kind.ELIMINATE),
          "missing jukebox elimination for " + path);
    }
    *//*? }*/
  }

  /** Registers the builtin extension once; the era-stable AttachFace key guards re-entry. */
  private static void registerOnce() {
    var registry = ParcelCoordinateFieldRegistry.get();
    if (registry.fields().stream().noneMatch(field -> field.path().equals("AttachFace"))) {
      var extension = new BuiltinExtension();
      var registrar = new ParcelExtensionRegistrarImpl(extension);
      extension.register(registrar);
      registrar.commit(ParcelContentTypeRegistry.get());
    }
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath("minecraft", path);
  }

  private static boolean declaresEntity(
      java.util.List<ParcelCoordinateField> fields,
      String path,
      String type,
      ParcelCoordinateField.Encoding encoding) {
    return fields.stream()
        .anyMatch(
            field ->
                field.path().equals(path)
                    && field.encoding() == encoding
                    && field.target() == ParcelCoordinateField.Target.ENTITY
                    && (type == null || field.type().equals(java.util.Optional.of(id(type)))));
  }

  private static boolean declaresBe(
      java.util.List<ParcelCoordinateField> fields,
      String path,
      String type,
      ParcelCoordinateField.Encoding encoding) {
    return fields.stream()
        .anyMatch(
            field ->
                field.path().equals(path)
                    && field.encoding() == encoding
                    && field.target() == ParcelCoordinateField.Target.BLOCK_ENTITY
                    && (type == null || field.type().equals(java.util.Optional.of(id(type)))));
  }

  private static boolean declaresTransient(
      java.util.List<ParcelTransientField> fields,
      String path,
      String type,
      ParcelTransientField.Kind kind) {
    return fields.stream()
        .anyMatch(
            field ->
                field.path().equals(path)
                    && field.kind() == kind
                    && (type == null || field.type().equals(java.util.Optional.of(id(type)))));
  }
}
