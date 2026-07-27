package io.github.leawind.gitparcel.common.testutils;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;

/** Minecraft test base with a small format registry fixture. */
public class AbstractGitParcelTest extends AbstractMinecraftTest {
  protected abstract static class TestFormat
      implements ParcelFormat.Impl<ParcelFormatConfig.None> {
    private final Spec spec;

    protected TestFormat(String id, int version) {
      this.spec = new Spec(id, version);
    }

    @Override
    public Spec spec() {
      return spec;
    }
  }

  protected static final class TestWriter extends TestFormat
      implements ParcelFormat.Writer<ParcelFormatConfig.None> {
    public TestWriter(String id, int version) {
      super(id, version);
    }

    @Override
    public int blockSectionSize() {
      return 16;
    }

    @Override
    public void write(
        WriteContext<ParcelFormatConfig.None> context, ParcelDataSource source)
        throws IOException, ParcelException {}
  }

  protected static final class TestReader extends TestFormat
      implements ParcelFormat.Reader<ParcelFormatConfig.None> {
    public TestReader(String id, int version) {
      super(id, version);
    }

    @Override
    public void read(ReadContext<ParcelFormatConfig.None> context, ParcelDataSink sink)
        throws IOException, ParcelException {}
  }

  @BeforeAll
  static void beforeAllGitParcel() {
    var registry = ParcelFormatRegistry.get();
    registry.clear();
    registry.registerDefaultWriter(new TestWriter("alpha", 0));
    registry.register(new TestWriter("beta", 0));
    registry.register(new TestReader("charlie", 0));
  }
}
