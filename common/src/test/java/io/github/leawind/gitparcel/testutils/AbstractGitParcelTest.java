package io.github.leawind.gitparcel.testutils;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class AbstractGitParcelTest {
  protected GitParcelRandom random;

  protected static class TestFormat implements ParcelFormat.Impl<ParcelFormatConfig.None> {

    @Override
    public Info info() {
      return info;
    }

    private final Info info;

    protected TestFormat(String id, int version) {
      this.info = new Info(id, version);
    }
  }

  protected static class TestSaver extends TestFormat
      implements ParcelFormat.Saver<ParcelFormatConfig.None> {

    protected TestSaver(String id, int version) {
      super(id, version);
    }

    @Override
    public void save(
        Level level,
        Vec3i parcelSize,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        ParcelFormatConfig.@Nullable None config)
        throws IOException {
      throw new IOException("Unimplemented");
    }
  }

  protected static class TestLoader extends TestFormat
      implements ParcelFormat.Loader<ParcelFormatConfig.None> {
    protected TestLoader(String id, int version) {
      super(id, version);
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
        @Block.UpdateFlags int flags,
        ParcelFormatConfig.@Nullable None config)
        throws IOException, ParcelException.CorruptedParcelException {
      throw new IOException("Unimplemented");
    }
  }

  protected IntIterable iter(int times) {
    return new IntIterable() {
      @Override
      public @NonNull IntIterator iterator() {
        return new IntIterator() {
          int i = 0;

          @Override
          public int nextInt() {
            return i++;
          }

          @Override
          public boolean hasNext() {
            return i < times;
          }
        };
      }
    };
  }

  @BeforeAll
  static void beforeAll() {
    SharedConstants.setVersion(DetectedVersion.BUILT_IN);
    ParcelFormatRegistry.INSTANCE.clear();
    ParcelFormatRegistry.INSTANCE.registerDefaultSaver(new TestSaver("alpha", 0));
    ParcelFormatRegistry.INSTANCE.register(new TestSaver("beta", 0));
    ParcelFormatRegistry.INSTANCE.register(new TestLoader("charlie", 0));
  }

  @BeforeEach
  void beforeEach() {
    random = new GitParcelRandom(12138);
  }
}
