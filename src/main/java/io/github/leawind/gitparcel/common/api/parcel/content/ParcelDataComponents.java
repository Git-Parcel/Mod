package io.github.leawind.gitparcel.common.api.parcel.content;

import net.minecraft.resources.Identifier;

/** Built-in parcel data components. Formats may also advertise extension-defined components. */
public final class ParcelDataComponents {
  public static final ParcelDataComponent BLOCKS = component("blocks");
  public static final ParcelDataComponent ENTITIES = component("entities");
  public static final ParcelDataComponent ATTACHMENTS = component("attachments");

  private ParcelDataComponents() {}

  private static ParcelDataComponent component(String path) {
    return new ParcelDataComponent(Identifier.fromNamespaceAndPath("gitparcel", path));
  }
}
