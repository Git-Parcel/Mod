package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.jimfs.Jimfs;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSource;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Reader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Writer;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Reader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Writer;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

public class ParcellaTest extends AbstractMinecraftTest {
  @Test
  void d16AndD32ShareRoundTripImplementation() throws Exception {
    roundTrip(new ParcellaD16Writer(), new ParcellaD16Reader());
    roundTrip(new ParcellaD32Writer(), new ParcellaD32Reader());
  }

  private static void roundTrip(ParcellaWriter writer, ParcellaReader reader) throws Exception {
    for (boolean palette : List.of(false, true)) {
      for (SubparcelFormat sectionFormat : SubparcelFormat.values()) {
        var config = new ParcellaFormat.Config();
        config.usePalette.set(palette);
        config.subparcelFormat.set(sectionFormat);
        try (var fs = Jimfs.newFileSystem()) {
          var dataDir = fs.getPath("/data");
          Files.createDirectories(dataDir);
          var context =
              new ParcelFormat.WriteContext<>(
                  new Vec3i(2, 2, 2), Vec3i.ZERO, 0, dataDir, config);
          writer.write(context, content());

          var sink = new CollectingSink();
          reader.read(
              new ParcelFormat.ReadContext<>(
                  context.parcelSize(),
                  context.anchor(),
                  context.dataVersion(),
                  context.dataDir(),
                  config),
              sink);

          assertEquals(1, sink.sections.size());
          assertEquals(
              List.of(
                  Blocks.AIR.defaultBlockState(),
                  Blocks.STONE.defaultBlockState(),
                  Blocks.DIRT.defaultBlockState(),
                  Blocks.COBBLESTONE.defaultBlockState(),
                  Blocks.AIR.defaultBlockState(),
                  Blocks.STONE.defaultBlockState(),
                  Blocks.DIRT.defaultBlockState(),
                  Blocks.COBBLESTONE.defaultBlockState()),
              sink.sections.getFirst().states());
          assertEquals(1, sink.entities.size());
          assertEquals(1, sink.attachments.size());
        }
      }
    }
  }

  private static ParcelContentSource content() {
    var states =
        List.of(
            Blocks.AIR.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.AIR.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState());
    var section =
        new BlockSection(
            BlockPos.ZERO, new Vec3i(2, 2, 2), states, List.of());
    var entity =
        new EntityRecord(
            Identifier.fromNamespaceAndPath("minecraft", "armor_stand"),
            new Vec3(0.5, 1, 0.5),
            BlockPos.ZERO,
            new CompoundTag(),
            List.of());
    CompoundTag payload = new CompoundTag();
    payload.putString("value", "opaque");
    var attachment =
        new AttachmentRecord(
            new LocalAttachmentId("test-0"),
            Identifier.fromNamespaceAndPath("test", "opaque"),
            3,
            false,
            payload);
    return new ParcelContentSource() {
      @Override
      public void forEachBlockSection(
          int sectionSize,
          io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConsumer<BlockSection>
              consumer)
          throws java.io.IOException,
              io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(section);
      }

      @Override
      public void forEachEntity(
          io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConsumer<EntityRecord>
              consumer)
          throws java.io.IOException,
              io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(entity);
      }

      @Override
      public void forEachAttachment(
          io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConsumer<
                  AttachmentRecord>
              consumer)
          throws java.io.IOException,
              io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(attachment);
      }
    };
  }

  private static final class CollectingSink implements ParcelContentSink {
    final List<BlockSection> sections = new ArrayList<>();
    final List<EntityRecord> entities = new ArrayList<>();
    final List<AttachmentRecord> attachments = new ArrayList<>();

    @Override
    public void acceptBlockSection(BlockSection section) {
      sections.add(section);
    }

    @Override
    public void acceptEntity(EntityRecord entity) {
      entities.add(entity);
    }

    @Override
    public void acceptAttachment(AttachmentRecord attachment) {
      attachments.add(attachment);
    }
  }
}
