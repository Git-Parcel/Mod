package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.gitparcel.parcel.formats.mvp.MvpV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateV0;
import org.junit.jupiter.api.Test;

public class ParcelFormatManagerTest {
  @Test
  void test() {
    var mgr = new ParcelFormatManager();
    mgr.registerDefault(new ParcellaV0.Save())
        .register(new StructureTemplateV0.Save())
        .register(new MvpV0.Save());

    assertEquals(mgr.defaultSaver(), mgr.getSaver("parcella"));

    assertNotNull(mgr.getSaver("parcella"));
    assertNotNull(mgr.getSaver("mvp"));
    assertNotNull(mgr.getSaver("structure_template"));
  }
}
