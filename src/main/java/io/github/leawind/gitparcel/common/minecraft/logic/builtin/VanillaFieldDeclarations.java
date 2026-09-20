package io.github.leawind.gitparcel.common.minecraft.logic.builtin;

import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField.Encoding;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField.Target;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField;
import net.minecraft.resources.Identifier;

/**
 * Vanilla semantic field declarations from the SEMANTICS.md section 9 audit.
 *
 * <p>Key names drift between the supported eras: 26.x writes snake_case keys with codec forms
 * while 1.20.1 writes PascalCase keys and legacy flat-axes compounds, so era-dependent keys are
 * declared on each side of the {@code >=26.1} boundary. Every key here was verified against that
 * version's serialization source; GameTests that inject NBT must use the same era-correct keys or
 * they decouple from vanilla serialization.
 */
final class VanillaFieldDeclarations {
  private static final String[] HIVES = {"beehive", "bee_nest"};
  private static final String[] FRAMES = {"item_frame", "glow_item_frame"};

  private VanillaFieldDeclarations() {}

  static void register(ParcelExtensionRegistrar registrar) {
    registerSpatialEdges(registrar);
    registerIdentityEdges(registrar);
    registerTimeEdges(registrar);
    registerTransientFields(registrar);
  }

  private static void registerSpatialEdges(ParcelExtensionRegistrar registrar) {
    // Item frames carry a wall/floor facing plus an in-plane item rotation; shulkers carry the
    // hanging facing. All use the engine's 3D data value encoding in both eras.
    for (String frame : FRAMES) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(Target.ENTITY, mc(frame), "Facing", Encoding.DIRECTION));
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(Target.ENTITY, mc(frame), "ItemRotation", Encoding.ROTATION_STEP));
    }
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.ENTITY, mc("shulker"), "AttachFace", Encoding.DIRECTION));

    // Vibration-listener event positions keep the same keys in both eras; they appear on sculk
    // block entities and on allay/warden entities, so both targets are declared. Structure-block
    // posX/Y/Z are offsets relative to the block itself (vanilla clamps them to +-48), not world
    // coordinates: the origin travels with the block and needs no declaration.
    for (Target target : Target.values()) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forAny(target, "listener.event.pos", Encoding.POSITION));
      registrar.registerCoordinateField(
          ParcelCoordinateField.forAny(target, "listener.selector.event.pos", Encoding.POSITION));
    }

    /*? if >=26.1 {*/
    for (String hive : HIVES) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(Target.BLOCK_ENTITY, mc(hive), "flower_pos", Encoding.BLOCK_POS));
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(
              Target.BLOCK_ENTITY, mc(hive), "bees[].entity_data.flower_pos", Encoding.BLOCK_POS));
    }
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.BLOCK_ENTITY, mc("end_gateway"), "exit_portal", Encoding.BLOCK_POS));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(Target.ENTITY, "leash", Encoding.BLOCK_POS));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(Target.ENTITY, "home_pos", Encoding.BLOCK_POS));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(Target.ENTITY, "sleeping_pos", Encoding.BLOCK_POS));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(Target.ENTITY, "anchor_pos", Encoding.BLOCK_POS));
    for (String field :
        new String[] {"hive_pos", "flower_pos", "patrol_target", "bound_pos", "wander_target", "beam_target"}) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forAny(Target.ENTITY, field, Encoding.BLOCK_POS));
    }
    /*?} else {*/
    /*for (String hive : HIVES) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(Target.BLOCK_ENTITY, mc(hive), "FlowerPos", Encoding.BLOCK_POS_XYZ));
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(
              Target.BLOCK_ENTITY, mc(hive), "Bees[].EntityData.FlowerPos", Encoding.BLOCK_POS_XYZ));
    }
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.BLOCK_ENTITY, mc("end_gateway"), "ExitPortal", Encoding.BLOCK_POS_XYZ));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(Target.ENTITY, "Leash", Encoding.BLOCK_POS_XYZ));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(Target.ENTITY, "Sleeping", Encoding.BLOCK_POS_AXES));
    // 1.20.1 writes single-tag positions through NbtUtils.writeBlockPos ({X,Y,Z} compounds).
    for (String field : new String[] {"HivePos", "FlowerPos", "PatrolTarget", "WanderTarget", "BeamTarget"}) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forAny(Target.ENTITY, field, Encoding.BLOCK_POS_XYZ));
    }
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.ENTITY, mc("phantom"), "A", Encoding.BLOCK_POS_AXES));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.ENTITY, mc("turtle"), "HomePos", Encoding.BLOCK_POS_AXES));
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.ENTITY, mc("turtle"), "TravelPos", Encoding.BLOCK_POS_AXES));
    // Paintings hang off a 3D-data-value facing in this era; the 26.x painting uses the 2D-value
    // `facing` key handled by PaintingRecordProcessor.
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(Target.ENTITY, mc("painting"), "Facing", Encoding.DIRECTION));
    *//*?}*/
  }

  private static void registerIdentityEdges(ParcelExtensionRegistrar registrar) {
    // Era-stable references. Only UUID-shaped values are rewritten (EntityUuidRemapper skips
    // other payloads), so same-named non-reference fields on modded entities stay untouched.
    for (String path : new String[] {"Owner", "Thrower", "Target", "ConversionPlayer", "LoveCause"}) {
      registrar.registerEntityRefField(ParcelEntityRefField.forAny(path));
    }
    registrar.registerEntityRefField(
        ParcelEntityRefField.forType(mc("warden"), "anger.suspects[].uuid"));
    for (String path :
        new String[] {
          "listener.event.source",
          "listener.event.projectile_owner",
          "listener.selector.event.source",
          "listener.selector.event.projectile_owner"
        }) {
      registrar.registerEntityRefField(ParcelEntityRefField.forAny(path));
    }

    /*? if >=26.1 {*/
    registrar.registerEntityRefField(ParcelEntityRefField.forAny("leash.UUID"));
    registrar.registerEntityRefField(ParcelEntityRefField.forAny("angry_at"));
    registrar.registerEntityRefField(ParcelEntityRefField.forType(mc("fox"), "Trusted[]"));
    /*?} else {*/
    /*registrar.registerEntityRefField(ParcelEntityRefField.forAny("Leash.UUID"));
    registrar.registerEntityRefField(ParcelEntityRefField.forAny("AngryAt"));
    registrar.registerEntityRefField(ParcelEntityRefField.forType(mc("fox"), "TrustedUUIDs[]"));
    *//*?}*/
  }

  private static void registerTimeEdges(ParcelExtensionRegistrar registrar) {
    // The vibration selector's tick is an absolute game-time reference in both eras.
    for (ParcelTransientField.Target target : ParcelTransientField.Target.values()) {
      registrar.registerTransientField(
          ParcelTransientField.forAny(
              target, "listener.selector.tick", ParcelTransientField.Kind.OFFSET_GAME_TIME));
    }
  }

  private static void registerTransientFields(ParcelExtensionRegistrar registrar) {
    // Volatile entity state whose changes carry no cross-snapshot semantics. HurtByTimestamp is
    // the legacy PascalCase key kept until 26.1; fall_distance keeps its 1.21+ snake_case form.
    for (String path :
        new String[] {
          "HurtTime",
          "DeathTime",
          "Fire",
          "Air",
          "TicksFrozen",
          "PortalCooldown",
          "FallDistance",
          "fall_distance",
          "current_explosion_impact_pos",
          "current_impulse_context_reset_grace_time",
          "shake",
          "Steps",
          "TXD",
          "TYD",
          "TZD",
          "HurtByTimestamp"
        }) {
      registrar.registerTransientField(
          ParcelTransientField.forAny(ParcelTransientField.Target.ENTITY, path, ParcelTransientField.Kind.ELIMINATE));
    }
    registrar.registerTransientField(
        ParcelTransientField.forType(
            ParcelTransientField.Target.BLOCK_ENTITY,
            mc("end_gateway"),
            "Age",
            ParcelTransientField.Kind.ELIMINATE));
    // Progress counters whose only variation is the passage of time (SEMANTICS.md rule 2.2).
    // The spawner block entity id has been minecraft:mob_spawner in both supported eras.
    registrar.registerTransientField(
        ParcelTransientField.forType(
            ParcelTransientField.Target.BLOCK_ENTITY,
            mc("mob_spawner"),
            "Delay",
            ParcelTransientField.Kind.ELIMINATE));
    registrar.registerTransientField(
        ParcelTransientField.forType(
            ParcelTransientField.Target.BLOCK_ENTITY,
            mc("hopper"),
            "TransferCooldown",
            ParcelTransientField.Kind.ELIMINATE));
    registrar.registerTransientField(
        ParcelTransientField.forType(
            ParcelTransientField.Target.ENTITY, mc("item"), "PickupDelay", ParcelTransientField.Kind.ELIMINATE));
    // Game-time absolute references rewritten as offsets between capture and restore (rule 2.3).
    registrar.registerTransientField(
        ParcelTransientField.forAny(
            ParcelTransientField.Target.ENTITY, "anger_end_time", ParcelTransientField.Kind.OFFSET_GAME_TIME));
    registrar.registerTransientField(
        ParcelTransientField.forType(
            ParcelTransientField.Target.BLOCK_ENTITY,
            mc("vault"),
            "server_data.state_updating_resumes_at",
            ParcelTransientField.Kind.OFFSET_GAME_TIME));
    for (String path : new String[] {"next_mob_spawns_at", "cooldown_ends_at"}) {
      registrar.registerTransientField(
          ParcelTransientField.forType(
              ParcelTransientField.Target.BLOCK_ENTITY,
              mc("trial_spawner"),
              path,
              ParcelTransientField.Kind.OFFSET_GAME_TIME));
    }
    // The interaction entity records its last attacker/responder with an absolute game-time
    // timestamp (the UUID half is an identity edge that keeps its value per invariant 4.2).
    for (String path : new String[] {"attack.timestamp", "interaction.timestamp"}) {
      registrar.registerTransientField(
          ParcelTransientField.forType(
              ParcelTransientField.Target.ENTITY,
              mc("interaction"),
              path,
              ParcelTransientField.Kind.OFFSET_GAME_TIME));
    }

    /*? if >=26.1 {*/
    registrar.registerTransientField(
        ParcelTransientField.forType(
            ParcelTransientField.Target.BLOCK_ENTITY,
            mc("jukebox"),
            "ticks_since_song_started",
            ParcelTransientField.Kind.ELIMINATE));
    /*?} else {*/
    /*for (String path : new String[] {"IsPlaying", "RecordStartTick", "TickCount"}) {
      registrar.registerTransientField(
          ParcelTransientField.forType(
              ParcelTransientField.Target.BLOCK_ENTITY, mc("jukebox"), path, ParcelTransientField.Kind.ELIMINATE));
    }
    *//*?}*/
  }

  private static Identifier mc(String path) {
    return Identifier.fromNamespaceAndPath("minecraft", path);
  }
}
