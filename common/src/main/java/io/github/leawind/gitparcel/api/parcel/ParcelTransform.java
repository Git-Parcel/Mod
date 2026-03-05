package io.github.leawind.gitparcel.api.parcel;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * @see net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 */
public class ParcelTransform {
  public static ParcelTransform none() {
    return new ParcelTransform();
  }

  protected Mirror mirror = Mirror.NONE;
  protected Rotation rotation = Rotation.NONE;

  public ParcelTransform() {}

  public ParcelTransform(Mirror mirror, Rotation rotation) {
    this.mirror = mirror;
    this.rotation = rotation;
  }

  public ParcelTransform copy() {
    return new ParcelTransform(mirror, rotation);
  }

  public ParcelTransform setMirror(Mirror mirror) {
    this.mirror = mirror;
    return this;
  }

  public ParcelTransform setRotation(Rotation rotation) {
    this.rotation = rotation;
    return this;
  }

  public Mirror getMirror() {
    return mirror;
  }

  public Rotation getRotation() {
    return rotation;
  }
}
