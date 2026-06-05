package io.github.leawind.gitparcel.testutils;

import io.github.leawind.gitparcel.core.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.core.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.core.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for tests that need both Minecraft runtime and a pre-populated
 * {@link ParcelFormatRegistry} with test format entries.
 *
 * <p>Extends {@link AbstractMinecraftTest} and adds common test
 * {@link ParcelFormat.Saver}/{@link ParcelFormat.Loader} implementations.
 */
public class AbstractGitParcelTest extends AbstractMinecraftTest {

  protected static class TestFormat implements ParcelFormat.Impl<ParcelFormatConfig.None> {

    @Override
    public Spec spec() {
      return spec;
    }

    private final Spec spec;

    protected TestFormat(String id, int version) {
      this.spec = new Spec(id, version);
    }
  }

  protected static class TestSaver extends TestFormat
      implements ParcelFormat.Saver<ParcelFormatConfig.None> {

    public TestSaver(String id, int version) {
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
        throws IOException, ParcelException.UnsupportedFeature {
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

  @BeforeAll
  static void beforeAllGitParcel() {
    ParcelFormatRegistry.INSTANCE.clear();
    ParcelFormatRegistry.INSTANCE.registerDefaultSaver(new TestSaver("alpha", 0));
    ParcelFormatRegistry.INSTANCE.register(new TestSaver("beta", 0));
    ParcelFormatRegistry.INSTANCE.register(new TestLoader("charlie", 0));
  }
}
