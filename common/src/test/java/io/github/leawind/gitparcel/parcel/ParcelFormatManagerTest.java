package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16FormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import org.junit.jupiter.api.Test;

public class ParcelFormatManagerTest {
  @Test
  void test() {
    var mgr = new ParcelFormatManager();

    mgr.registerDefault(new ParcellaD16FormatV0.Save());
    mgr.register(new StructureTemplateFormatV0.Save());
    mgr.register(new MvpFormatV0.Save());

    assertEquals(mgr.defaultSaver(), mgr.getSaver("parcella_d16"));

    assertNotNull(mgr.getSaver("parcella_d16"));
    assertNotNull(mgr.getSaver("mvp"));
    assertNotNull(mgr.getSaver("structure_template"));
  }
}
