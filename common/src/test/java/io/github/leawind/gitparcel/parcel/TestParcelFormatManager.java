package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.parcel.formats.mvp.MvpV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateV0;
import org.junit.jupiter.api.Test;

public class TestParcelFormatManager {
  @Test
  void test() {
    var mgr = new ParcelFormatManager();
    mgr.registerDefault(new ParcellaV0.Save())
        .register(new StructureTemplateV0.Save())
        .register(new MvpV0.Save());

    assert mgr.defaultSaver() == mgr.saver("parcella");

    assert mgr.saver("parcella") != null;
    assert mgr.saver("mvp") != null;
    assert mgr.saver("structure_template") != null;
  }
}
