package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.core.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.core.api.parcel.config.ParcelFormatConfig;
import java.util.EnumSet;

public class Temp {

  class MyFormat implements ParcelFormat.Impl<ParcelFormatConfig.None> {
    private static final Spec SPEC = new Spec("mvp", 0);

    @Override
    public Spec spec() {
      return SPEC;
    }

    @Override
    public EnumSet<Feature> features() {
      return EnumSet.noneOf(Feature.class);
    }
  }
}
