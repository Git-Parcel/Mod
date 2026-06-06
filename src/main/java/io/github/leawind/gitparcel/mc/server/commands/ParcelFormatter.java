package io.github.leawind.gitparcel.mc.server.commands;

import io.github.leawind.gitparcel.core.world.Parcel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class ParcelFormatter {

  private ParcelFormatter() {}

  public static Component formatBoundingBox(BoundingBox bb) {
    return Component.literal(
        "(" + bb.minX() + ", " + bb.minY() + ", " + bb.minZ() + ") -> (" + bb.maxX() + ", "
            + bb.maxY() + ", " + bb.maxZ() + ")");
  }

  public static Component formatParcelInfo(
      Parcel parcel, String firstLinePrefix, String linePrefix) {
    var meta = parcel.meta();
    var transform = parcel.transform();
    var bb = parcel.getBoundingBox();
    var size = meta.size();
    var center = bb.getCenter();
    var translation = transform.translation();

    var component = Component.empty();
    component
        .append(Component.literal(firstLinePrefix + "UUID: " + parcel.uuid()))
        .append(Component.literal("\n"));

    if (meta.name() != null) {
      component
          .append(Component.literal(linePrefix + "Name: " + meta.name()))
          .append(Component.literal("\n"));
    }

    component
        .append(Component.literal(linePrefix + "Format: " + meta.formatSpec()))
        .append(Component.literal("\n"))
        .append(
            Component.literal(
                linePrefix + "Size: " + size.getX() + " x " + size.getY() + " x " + size.getZ()))
        .append(Component.literal("\n"))
        .append(
            Component.literal(
                linePrefix
                    + "Center: ("
                    + center.getX()
                    + ", "
                    + center.getY()
                    + ", "
                    + center.getZ()
                    + ")"))
        .append(Component.literal("\n"))
        .append(linePrefix + "Bounds: ")
        .append(formatBoundingBox(bb))
        .append(Component.literal("\n"))
        .append(Component.literal(linePrefix + "Transform:"))
        .append(Component.literal("\n"))
        .append(
            Component.literal(
                linePrefix
                    + "  Offset: ("
                    + translation.getX()
                    + ", "
                    + translation.getY()
                    + ", "
                    + translation.getZ()
                    + ")"))
        .append(Component.literal("\n"))
        .append(Component.literal(linePrefix + "  Rotation: " + transform.rotation()))
        .append(Component.literal("\n"))
        .append(Component.literal(linePrefix + "  Mirror: " + transform.mirror()));

    return component;
  }
}
