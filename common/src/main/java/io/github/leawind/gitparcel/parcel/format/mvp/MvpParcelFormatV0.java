package io.github.leawind.gitparcel.parcel.format.mvp;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;

import java.io.IOException;
import java.nio.file.Path;

public class MvpParcelFormatV0 extends ParcelFormat {
  public static ParcelFormat INSTANCE = new MvpParcelFormatV0();

  MvpParcelFormatV0() {
    super("mvp", 0);
  }

  @Override
  public void saveContent(Parcel parcel, Path dir) throws IOException {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void loadContent(Parcel parcel, Path dir) throws IOException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
