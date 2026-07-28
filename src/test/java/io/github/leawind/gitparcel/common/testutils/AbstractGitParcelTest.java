package io.github.leawind.gitparcel.common.testutils;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;

/** Minecraft test base with a small parcel-content registry fixture. */
public class AbstractGitParcelTest extends AbstractMinecraftTest {
  protected static class TestContentType
      implements ParcelContentType<ParcelContentConfig.None> {
    private final Spec spec;

    protected TestContentType(String id, int version) {
      this.spec = new Spec(id, version);
    }

    @Override
    public Spec spec() {
      return spec;
    }
    @Override
    public void save(SaveContext<ParcelContentConfig.None> context, ParcelDataSource source)
        throws IOException, ParcelException {}

    @Override
    public void load(LoadContext<ParcelContentConfig.None> context, ParcelDataSink sink)
        throws IOException, ParcelException {}
  }

  @BeforeAll
  static void beforeAllGitParcel() {
    var registry = ParcelContentTypeRegistry.get();
    registry.clear();
    registry.register(new TestContentType("alpha", 0));
    registry.register(new TestContentType("beta", 0));
    registry.register(new TestContentType("charlie", 0));
  }
}
