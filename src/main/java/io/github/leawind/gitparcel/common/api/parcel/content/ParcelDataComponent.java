package io.github.leawind.gitparcel.common.api.parcel.content;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Identifies one semantic kind of data contained in a parcel. */
public record ParcelDataComponent(Identifier id) {
  public ParcelDataComponent {
    Objects.requireNonNull(id, "id");
  }
}
