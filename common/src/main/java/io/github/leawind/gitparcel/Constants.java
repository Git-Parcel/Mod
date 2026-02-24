package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.ParcelFormatManager;
import io.github.leawind.gitparcel.parcel.formats.mvp.MvpV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateV0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
  public static final String MOD_ID = "gitparcel";
  public static final String MOD_NAME = "Git Parcel";
  public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

  public static final ParcelFormatManager PARCEL_FORMATS =
      new ParcelFormatManager()
          .register(new MvpV0.Save())
          .register(new StructureTemplateV0.Save())
          .registerDefault(new ParcellaV0.Save());
}
