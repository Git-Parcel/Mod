package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class ParcelFormatRegistryTest {
  @Test
  void test() {
    var mgr = new ParcelFormatRegistry();

    mgr.registerDefaultSaver(new TestSaver("alpha", 0));
    mgr.register(new TestSaver("beta", 0));
    mgr.register(new TestLoader("charlie", 0));

    assertEquals(mgr.defaultSaver(), mgr.getSaver("alpha"));

    assertNotNull(mgr.getSaver("beta"));
    assertNull(mgr.getLoader("beta"));

    assertNull(mgr.getSaver("charlie"));
    assertNotNull(mgr.getLoader("charlie"));

    assertNull(mgr.getSaver("non-existent"));
  }

  static class TestFormat implements ParcelFormat.Impl<ParcelFormatConfig.None> {

    @Override
    public String id() {
      return id;
    }

    @Override
    public int version() {
      return version;
    }

    private final String id;
    private final int version;

    protected TestFormat(String id, int version) {
      this.id = id;
      this.version = version;
    }
  }

  static class TestSaver extends TestFormat implements ParcelFormat.Save<ParcelFormatConfig.None> {

    protected TestSaver(String id, int version) {
      super(id, version);
    }

    @Override
    public void save(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        ParcelFormatConfig.@Nullable None config)
        throws IOException {
      throw new IOException("Unimplemented");
    }
  }

  static class TestLoader extends TestFormat implements ParcelFormat.Load<ParcelFormatConfig.None> {
    protected TestLoader(String id, int version) {
      super(id, version);
    }

    @Override
    public void load(
        ServerLevelAccessor level,
        Vec3i size,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        ParcelFormatConfig.@Nullable None config)
        throws IOException, ParcelException.CorruptedParcelException {
      throw new IOException("Unimplemented");
    }
  }
}
