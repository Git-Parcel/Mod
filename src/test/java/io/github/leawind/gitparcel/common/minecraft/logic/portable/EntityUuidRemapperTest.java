package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityUuidRemapperTest extends AbstractMinecraftTest {
  private static final Identifier TEST_ENTITY = Identifier.fromNamespaceAndPath("gitparceltest", "entity");
  private static final Identifier OTHER_ENTITY = Identifier.fromNamespaceAndPath("gitparceltest", "other");

  private final UUID insideA = UUID.randomUUID();
  private final UUID insideB = UUID.randomUUID();
  private final UUID outside = UUID.randomUUID();

  @BeforeAll
  static void registerRefFields() {
    var registry = ParcelEntityRefFieldRegistry.get();
    if (registry.fields().stream().anyMatch(field -> field.path().equals("partner"))) {
      return;
    }
    registry.register(ParcelEntityRefField.forType(TEST_ENTITY, "leash.UUID"));
    registry.register(ParcelEntityRefField.forType(TEST_ENTITY, "partner"));
    registry.register(ParcelEntityRefField.forAny("memories.friends[].id"));
  }

  @Test
  void assignsDistinctFreshIds() {
    var remap = EntityUuidRemapper.assignFreshIds(List.of(insideA, insideB, insideA));

    assertEquals(2, remap.size());
    assertNotEquals(insideA, remap.get(insideA));
    assertNotEquals(insideB, remap.get(insideB));
    assertNotEquals(remap.get(insideA), remap.get(insideB));
  }

  @Test
  void rewritesIntraBatchReferencesAndLeavesExternalOnesAlone() {
    var remap = Map.of(insideA, UUID.randomUUID(), insideB, UUID.randomUUID());
    var data = new CompoundTag();
    data.putString("id", TEST_ENTITY.toString());
    putUuid(putCompound(data, "leash"), "UUID", insideB);
    putUuid(data, "partner", insideA);
    putUuid(putCompound(data, "owner_external"), "UUID", outside);
    var memories = new CompoundTag();
    var friends = new net.minecraft.nbt.ListTag();
    var friend = new CompoundTag();
    putUuid(friend, "id", insideA);
    friends.add(friend);
    memories.put("friends", friends);
    data.put("memories", memories);

    EntityUuidRemapper.rewriteReferences(data, TEST_ENTITY, new HashMap<>(remap));

    assertEquals(
        remap.get(insideB),
        readUuid(data.getCompound("leash").orElseThrow(), "UUID").orElseThrow());
    assertEquals(remap.get(insideA), readUuid(data, "partner").orElseThrow());
    assertEquals(outside, readUuid(data.getCompound("owner_external").orElseThrow(), "UUID").orElseThrow());
    assertEquals(
        remap.get(insideA),
        readUuid(
                data.getCompound("memories").orElseThrow().getList("friends").orElseThrow().getCompound(0).orElseThrow(),
                "id")
            .orElseThrow());
  }

  @Test
  void respectsTypeScopes() {
    var remap = Map.of(insideA, UUID.randomUUID());
    var data = new CompoundTag();
    data.putString("id", OTHER_ENTITY.toString());
    putUuid(data, "partner", insideA);

    EntityUuidRemapper.rewriteReferences(data, OTHER_ENTITY, new HashMap<>(remap));

    assertEquals(insideA, readUuid(data, "partner").orElseThrow());
  }

  @Test
  void rewritesPassengerSubtreesWithTheirOwnScope() {
    var remap = Map.of(insideA, UUID.randomUUID());
    var data = new CompoundTag();
    data.putString("id", TEST_ENTITY.toString());
    putUuid(data, "partner", insideA);
    var passengers = new net.minecraft.nbt.ListTag();
    var passenger = new CompoundTag();
    passenger.putString("id", OTHER_ENTITY.toString());
    putUuid(passenger, "partner", insideA);
    passengers.add(passenger);
    data.put("Passengers", passengers);

    EntityUuidRemapper.rewriteReferences(data, TEST_ENTITY, new HashMap<>(remap));

    var passengerTag = data.getList("Passengers").orElseThrow().getCompound(0).orElseThrow();
    assertTrue(readUuid(passengerTag, "partner").isPresent());
  }

  private static CompoundTag putCompound(CompoundTag parent, String key) {
    var compound = new CompoundTag();
    parent.put(key, compound);
    return compound;
  }

  private static void putUuid(CompoundTag data, String key, UUID value) {
    data.put(key, UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
  }

  private static java.util.Optional<UUID> readUuid(CompoundTag data, String key) {
    var tag = data.get(key);
    return tag == null ? java.util.Optional.empty() : UUIDUtil.CODEC.parse(NbtOps.INSTANCE, tag).result();
  }
}
