package io.github.leawind.gitparcel.gametest.utils;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jspecify.annotations.Nullable;

public class GitParcelTestUtils {
  public interface ParcelFormatCombinationConsumer<F extends ParcelFormat> {
    void accept(F format, Rotation rotation, Mirror mirror) throws Exception;
  }

  public static <F extends ParcelFormat> void forEachFormatCombination(
      Collection<F> formats, ParcelFormatCombinationConsumer<F> consumer) throws Exception {
    for (var format : formats) {
      Rotation[] rotations =
          format.features().contains(ParcelFormat.Feature.ROTATE)
              ? Rotation.values()
              : new Rotation[] {Rotation.NONE};

      Mirror[] mirrors =
          format.features().contains(ParcelFormat.Feature.MIRROR)
              ? Mirror.values()
              : new Mirror[] {Mirror.NONE};

      for (Rotation rotation : rotations) {
        for (Mirror mirror : mirrors) {
          consumer.accept(format, rotation, mirror);
        }
      }
    }
  }

  public static boolean compareNbtStructure(
      @Nullable Tag tagA, @Nullable Tag tagB, boolean compareListTag) {
    if (tagA == tagB) {
      return true;
    }
    if (tagA == null || tagB == null) {
      return false;
    }
    if (!tagA.getClass().equals(tagB.getClass())) {
      return false;
    }

    return switch (tagA) {
      case CompoundTag expCompound -> {
        CompoundTag actCompound = (CompoundTag) tagB;
        if (actCompound.size() != expCompound.size()) {
          yield false;
        }
        for (var entry : expCompound.entrySet()) {
          Tag actTag = actCompound.get(entry.getKey());
          if (actTag == null || !compareNbtStructure(entry.getValue(), actTag, compareListTag)) {
            yield false;
          }
        }
        yield true;
      }
      case ListTag expList -> {
        if (!compareListTag) {
          yield true;
        }
        ListTag actList = (ListTag) tagB;
        if (actList.size() != expList.size()) {
          yield false;
        }
        for (int i = 0; i < expList.size(); i++) {
          if (!compareNbtStructure(expList.get(i), actList.get(i), compareListTag)) {
            yield false;
          }
        }
        yield true;
      }
      default -> true;
    };
  }
}
