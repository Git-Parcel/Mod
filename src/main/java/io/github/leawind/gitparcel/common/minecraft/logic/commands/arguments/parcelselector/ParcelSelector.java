package io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.parcelselector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Parcel selector similar to Minecraft's EntitySelector Following the same pattern as
 * EntitySelector for consistency
 */
public final class ParcelSelector {
  public static final int INFINITE = Integer.MAX_VALUE;

  private final int maxResults;
  private final List<Predicate<Parcel>> predicates;
  private final BiConsumer<Vec3, List<Parcel>> order;
  private final boolean isSighted;
  private final boolean isWorldLimited;

  private final @Nullable String name;
  private final @Nullable UUID uuid;

  public ParcelSelector(
      int maxResults,
      List<Predicate<Parcel>> predicates,
      BiConsumer<Vec3, List<Parcel>> order,
      boolean isSighted,
      boolean isWorldLimited,
      @Nullable String name,
      @Nullable UUID uuid) {
    this.maxResults = maxResults;
    this.predicates = predicates;
    this.order = order;
    this.isSighted = isSighted;
    this.isWorldLimited = isWorldLimited;
    this.name = name;
    this.uuid = uuid;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public boolean isSighted() {
    return isSighted;
  }

  public boolean isWorldLimited() {
    return isWorldLimited;
  }

  public boolean useSelector() {
    return name == null && uuid == null;
  }

  private void checkPermissions(CommandSourceStack source) throws CommandSyntaxException {
    // Refer: net.minecraft.world.entity.EntitySelector#checkPermissions
    if (!useSelector()) {
      return;
    }
    var permissions = GitParcelWorldSavedData.get(source.getServer()).permissions();
    if (!permissions.permits(WorldPermissions.LIST_PARCELS, source.permissions())) {
      throw ParcelArgument.ERROR_SELECTOR_NOT_ALLOWED.create();
    }
  }

  public Parcel findSingleParcel(CommandSourceStack source) throws CommandSyntaxException {
    checkPermissions(source);

    var list = findParcels(source);
    if (list.isEmpty()) {
      throw ParcelArgument.ERROR_NO_PARCEL_FOUND.create();
    } else if (list.size() > 1) {
      throw ParcelArgument.ERROR_NOT_SINGLE_PARCEL.create();
    } else {
      return list.getFirst();
    }
  }

  public List<Parcel> findParcels(CommandSourceStack source) throws CommandSyntaxException {

    checkPermissions(source);

    var parcels = GitParcelLevelSavedData.get(source.getLevel()).parcels().values().stream();

    if (name != null) {
      return parcels.filter(parcel -> name.equals(parcel.meta().name())).toList();

    } else if (uuid != null) {
      return parcels.filter(parcel -> uuid.equals(parcel.uuid())).toList();

    } else if (isSighted()) {

      Vec3 rayFrom;
      Vec3 rayTo;
      {
        var entity = source.getEntity();
        if (entity == null) {
          rayFrom = source.getPosition();
          rayTo = null;
        } else {
          rayFrom = entity.getEyePosition();
          rayTo = rayFrom.add(entity.getViewVector(1).scale(1024));
        }
      }

      Parcel result = null;
      double minDistance = Double.MAX_VALUE;

      for (Parcel parcel : parcels.toList()) {
        AABB aabb = AABB.of(parcel.getBoundingBox());

        if (aabb.contains(rayFrom)) {
          if (minDistance == 0) {
            // The executor is in the intersection of multiple parcels
            return List.of();
          }
          result = parcel;
          minDistance = 0;
        } else if (rayTo != null) {
          var clipResult = aabb.clip(rayFrom, rayTo);
          if (clipResult.isPresent()) {
            double distance = clipResult.get().distanceTo(rayFrom);
            if (distance < minDistance) {
              minDistance = distance;
              result = parcel;
            }
          }
        }
      }

      return result == null ? List.of() : List.of(result);
    }

    var list = parcels.filter(Util.allOf(predicates)).toList();

    if (list.size() > 1) {
      order.accept(source.getPosition(), list);
    }

    list = list.subList(0, Math.min(maxResults, list.size()));

    return list;
  }
}
