package io.github.leawind.gitparcel.gametest.utils;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import java.util.Collection;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

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
}
