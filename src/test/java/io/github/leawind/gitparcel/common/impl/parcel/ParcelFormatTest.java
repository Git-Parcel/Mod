package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class ParcelFormatTest extends AbstractMinecraftTest {
  Predicate<String> idValidator = ParcelFormat.Spec.ID_PATTERN.asPredicate();

  @Test
  void testIdPattern() {
    assertTrue(idValidator.test("mvp"));
    assertTrue(idValidator.test("parcella"));

    assertTrue(idValidator.test("parcella_d32"));
    assertTrue(idValidator.test("parcella_d16"));

    assertTrue(idValidator.test("parcella_d32_RLE3D"));
    assertTrue(idValidator.test("parcella_d32_FLAT"));

    assertFalse(idValidator.test("16_parcella"));
    assertFalse(idValidator.test("16_parcella"));
  }

  @Test
  void contextSaveCallBridgesToLegacyFormat() throws Exception {
    var receivedDataDir = new AtomicReference<Path>();
    ParcelFormat.Saver<ParcelFormatConfig.None> saver =
        new ParcelFormat.Saver<>() {
          @Override
          public Spec spec() {
            return new Spec("legacy_saver", 0);
          }

          @Override
          public void save(
              Level level,
              Vec3i parcelSize,
              Vec3i anchor,
              ParcelTransform transform,
              Path dataDir,
              boolean ignoreEntities,
              ParcelFormatConfig.@Nullable None config) {
            receivedDataDir.set(dataDir);
          }
        };
    var context = saveContext();

    saver.save(context);

    assertEquals(context.dataDir(), receivedDataDir.get());
  }

  @Test
  void legacySaveCallBridgesToContextFormat() throws Exception {
    var receivedContext =
        new AtomicReference<ParcelFormat.SaveContext<ParcelFormatConfig.None>>();
    ParcelFormat.ContextSaver<ParcelFormatConfig.None> saver =
        new ParcelFormat.ContextSaver<>() {
          @Override
          public Spec spec() {
            return new Spec("context_saver", 0);
          }

          @Override
          public void save(SaveContext<ParcelFormatConfig.None> context) {
            receivedContext.set(context);
          }
        };
    var context = saveContext();

    saver.save(
        context.level(),
        context.parcelSize(),
        context.anchor(),
        context.transform(),
        context.dataDir(),
        context.ignoreEntities(),
        context.config());

    assertEquals(context.dataDir(), receivedContext.get().dataDir());
  }

  @Test
  void contextLoadCallBridgesToLegacyFormat() throws Exception {
    var receivedFlags = new AtomicReference<Integer>();
    ParcelFormat.Loader<ParcelFormatConfig.None> loader =
        new ParcelFormat.Loader<>() {
          @Override
          public Spec spec() {
            return new Spec("legacy_loader", 0);
          }

          @Override
          public void load(
              ServerLevelAccessor level,
              Vec3i size,
              Vec3i anchor,
              ParcelTransform transform,
              Path dataDir,
              boolean ignoreBlocks,
              boolean ignoreEntities,
              int flags,
              ParcelFormatConfig.@Nullable None config) {
            receivedFlags.set(flags);
          }
        };
    var context = loadContext();

    loader.load(context);

    assertEquals(context.blockUpdateFlags(), receivedFlags.get());
  }

  @Test
  void legacyLoadCallBridgesToContextFormat() throws Exception {
    var receivedContext =
        new AtomicReference<ParcelFormat.LoadContext<ParcelFormatConfig.None>>();
    ParcelFormat.ContextLoader<ParcelFormatConfig.None> loader =
        new ParcelFormat.ContextLoader<>() {
          @Override
          public Spec spec() {
            return new Spec("context_loader", 0);
          }

          @Override
          public void load(LoadContext<ParcelFormatConfig.None> context) {
            receivedContext.set(context);
          }
        };
    var context = loadContext();

    loader.load(
        context.level(),
        context.parcelSize(),
        context.anchor(),
        context.transform(),
        context.dataDir(),
        context.ignoreBlocks(),
        context.ignoreEntities(),
        context.blockUpdateFlags(),
        context.config());

    assertEquals(context.dataDir(), receivedContext.get().dataDir());
  }

  private static ParcelFormat.SaveContext<ParcelFormatConfig.None> saveContext() {
    return new ParcelFormat.SaveContext<>(
        null,
        new Vec3i(3, 5, 7),
        ParcelTransform.IDENTITY,
        Vec3i.ZERO,
        Path.of("parcel-data"),
        true,
        null);
  }

  private static ParcelFormat.LoadContext<ParcelFormatConfig.None> loadContext() {
    return new ParcelFormat.LoadContext<>(
        null,
        new Vec3i(3, 5, 7),
        ParcelTransform.IDENTITY,
        Vec3i.ZERO,
        Path.of("parcel-data"),
        false,
        true,
        42,
        null);
  }
}
