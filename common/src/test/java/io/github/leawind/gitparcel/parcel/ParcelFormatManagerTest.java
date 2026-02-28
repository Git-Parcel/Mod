package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaFormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import org.junit.jupiter.api.Test;

public class ParcelFormatManagerTest {
  @Test
  void test() {
    var mgr = new ParcelFormatManager();
    mgr.registerDefault(new ParcellaFormatV0.Save())
        .register(new StructureTemplateFormatV0.Save())
        .register(new MvpFormatV0.Save());

    assertEquals(mgr.defaultSaver(), mgr.getSaver("parcella"));

    assertNotNull(mgr.getSaver("parcella"));
    assertNotNull(mgr.getSaver("mvp"));
    assertNotNull(mgr.getSaver("structure_template"));
  }
}
