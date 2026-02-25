package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.formats.mvp.MvpV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateV0;
import io.github.leawind.gitparcel.platform.Services;

public class GitParcelMod {

  public static void init() {
    Constants.PARCEL_FORMATS
        .registerDefault(new ParcellaV0.Save())
        .register(new StructureTemplateV0.Save())
        .register(new StructureTemplateV0.Load());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      Constants.PARCEL_FORMATS.register(new MvpV0.Save());
    }
  }
}
