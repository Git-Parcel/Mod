package io.github.leawind.gitparcel.gametest.utils;

import net.minecraft.world.entity.EntityType;
/*? if >=26.1 {*/
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
/*?} else {*/
/*import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
*//*?}*/
/*? if >=26.3 {*/
import net.minecraft.world.entity.EntityTypes;
/*?}*/
import net.minecraft.world.entity.decoration.ItemFrame;
/*? if >=26.1 {*/
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.Arrow;
/*?} else {*/
/*import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
*//*?}*/
import net.minecraft.world.entity.monster.Shulker;
/*? if >=26.1 {*/
import net.minecraft.world.entity.npc.villager.Villager;
/*?} else {*/
/*import net.minecraft.world.entity.npc.Villager;
*//*?}*/

/**
 * Version seam for entity type constants, which 26.3 moved from {@link EntityType}, and for the
 * animal classes, which 26.1 split into per-species packages.
 */
public final class GameEntityTypes {
  /*? if >=26.3 {*/
  public static final EntityType<Cow> COW = EntityTypes.COW;
  public static final EntityType<Chicken> CHICKEN = EntityTypes.CHICKEN;
  public static final EntityType<ItemFrame> ITEM_FRAME = EntityTypes.ITEM_FRAME;
  public static final EntityType<Painting> PAINTING = EntityTypes.PAINTING;
  public static final EntityType<Shulker> SHULKER = EntityTypes.SHULKER;
  public static final EntityType<Skeleton> SKELETON = EntityTypes.SKELETON;
  public static final EntityType<Arrow> ARROW = EntityTypes.ARROW;
  public static final EntityType<Turtle> TURTLE = EntityTypes.TURTLE;
  public static final EntityType<Villager> VILLAGER = EntityTypes.VILLAGER;
  /*?} else {*/
  /*public static final EntityType<Cow> COW = EntityType.COW;
  public static final EntityType<Chicken> CHICKEN = EntityType.CHICKEN;
  public static final EntityType<ItemFrame> ITEM_FRAME = EntityType.ITEM_FRAME;
  public static final EntityType<Painting> PAINTING = EntityType.PAINTING;
  public static final EntityType<Shulker> SHULKER = EntityType.SHULKER;
  public static final EntityType<Skeleton> SKELETON = EntityType.SKELETON;
  public static final EntityType<Arrow> ARROW = EntityType.ARROW;
  public static final EntityType<Turtle> TURTLE = EntityType.TURTLE;
  public static final EntityType<Villager> VILLAGER = EntityType.VILLAGER;
  *//*?}*/

  private GameEntityTypes() {}
}
