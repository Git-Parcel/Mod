package io.github.leawind.gitparcel.gametest.utils;

import net.minecraft.world.entity.EntityType;
/*? if >=26.3 {*/
import net.minecraft.world.entity.EntityTypes;
/*?}*/
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.decoration.ItemFrame;

/** Version seam for entity type constants, which 26.3 moved from {@link EntityType}. */
public final class GameEntityTypes {
  /*? if >=26.3 {*/
  public static final EntityType<Cow> COW = EntityTypes.COW;
  public static final EntityType<Chicken> CHICKEN = EntityTypes.CHICKEN;
  public static final EntityType<ItemFrame> ITEM_FRAME = EntityTypes.ITEM_FRAME;
  /*?} else {*/
  /*public static final EntityType<Cow> COW = EntityType.COW;
  public static final EntityType<Chicken> CHICKEN = EntityType.CHICKEN;
  public static final EntityType<ItemFrame> ITEM_FRAME = EntityType.ITEM_FRAME;
  *//*?}*/

  private GameEntityTypes() {}
}
