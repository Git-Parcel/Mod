package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16FormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD32FormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import io.github.leawind.gitparcel.platform.Services;

public class GitParcelMod {

  public static void init() {
    registerFormats();
  }

  private static void registerFormats() {
    var formatManager = Constants.PARCEL_FORMATS;

    formatManager.registerDefault(new ParcellaD32FormatV0.Save());
    formatManager
        .register(new StructureTemplateFormatV0.Save())
        .register(new StructureTemplateFormatV0.Load());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      formatManager.register(new ParcellaD16FormatV0.Save());
      formatManager.register(new MvpFormatV0.Save());
    }
  }
}
