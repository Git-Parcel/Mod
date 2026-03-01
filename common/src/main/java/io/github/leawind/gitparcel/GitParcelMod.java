package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16FormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD32FormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import io.github.leawind.gitparcel.platform.Services;

public class GitParcelMod {

  public static void init() {
    Constants.PARCEL_FORMATS
        .register(new ParcellaD16FormatV0.Save())
        .registerDefault(new ParcellaD32FormatV0.Save())
        .register(new StructureTemplateFormatV0.Save())
        .register(new StructureTemplateFormatV0.Load());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      Constants.PARCEL_FORMATS.register(new MvpFormatV0.Save());
    }
  }
}
