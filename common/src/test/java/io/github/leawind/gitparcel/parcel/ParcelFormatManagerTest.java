package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class ParcelFormatManagerTest {
  @Test
  void test() {
    var mgr = new ParcelFormatManager();

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
        Parcel parcel,
        Path dataDir,
        boolean saveEntities,
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
        ServerLevel level,
        BlockPos parcelOrigin,
        Path dataDir,
        boolean loadBlocks,
        boolean loadEntities)
        throws IOException, ParcelException {
      throw new IOException("Unimplemented");
    }
  }
}
