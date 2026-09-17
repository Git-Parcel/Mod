package io.github.leawind.gitparcel.common.impl.extension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.BuiltinExtension;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.TransientFieldProcessor;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** Pins the audited vanilla spatial-edge declarations. */
class BuiltinExtensionRegistrationTest extends AbstractMinecraftTest {

  @Test
  void declaresAuditedSpatialEdgeFields() {
    var registry = ParcelCoordinateFieldRegistry.get();
    if (registry.fields().stream().noneMatch(field -> field.path().equals("home_pos"))) {
      var extension = new BuiltinExtension();
      var registrar = new ParcelExtensionRegistrarImpl(extension);
      extension.register(registrar);
      registrar.commit(ParcelContentTypeRegistry.get());
    }
    var fields = registry.fields();

    assertTrue(declares(fields, "home_pos"));
    assertTrue(declares(fields, "sleeping_pos"));
    for (String path : new String[] {
      "hive_pos", "flower_pos", "anchor_pos", "patrol_target", "bound_pos", "wander_target",
      "beam_target"
    }) {
      assertTrue(declares(fields, path), "missing declaration for " + path);
    }

    assertTrue(
        fields.stream()
            .anyMatch(
                field ->
                    field.path().equals("exit_portal")
                        && field.target() == ParcelCoordinateField.Target.BLOCK_ENTITY
                        && field
                            .type()
                            .equals(java.util.Optional.of(
                                Identifier.fromNamespaceAndPath("minecraft", "end_gateway")))));
  }

  @Test
  void registersTransientFieldProcessor() {
    if (ParcelRecordProcessorRegistry.get().get(TransientFieldProcessor.ID) == null) {
      var extension = new BuiltinExtension();
      var registrar = new ParcelExtensionRegistrarImpl(extension);
      extension.register(registrar);
      registrar.commit(ParcelContentTypeRegistry.get());
    }
    assertNotNull(
        ParcelRecordProcessorRegistry.get().get(TransientFieldProcessor.ID),
        "the builtin extension must register the transient-field processor");
  }

  private static boolean declares(
      java.util.List<ParcelCoordinateField> fields, String path) {
    return fields.stream()
        .anyMatch(
            field ->
                field.path().equals(path)
                    && field.encoding() == ParcelCoordinateField.Encoding.BLOCK_POS
                    && field.target() == ParcelCoordinateField.Target.ENTITY);
  }
}
