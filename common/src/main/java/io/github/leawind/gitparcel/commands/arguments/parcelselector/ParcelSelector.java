package io.github.leawind.gitparcel.commands.arguments.parcelselector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.world.Parcel;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Util;
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
  private final boolean isWorldLimited;

  private final @Nullable String name;
  private final @Nullable UUID uuid;

  public ParcelSelector(
      int maxResults,
      List<Predicate<Parcel>> predicates,
      BiConsumer<Vec3, List<Parcel>> order,
      boolean isWorldLimited,
      @Nullable String name,
      @Nullable UUID uuid) {
    this.maxResults = maxResults;
    this.predicates = predicates;
    this.order = order;
    this.isWorldLimited = isWorldLimited;
    this.name = name;
    this.uuid = uuid;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public boolean isWorldLimited() {
    return this.isWorldLimited;
  }

  public boolean useSelector() {
    return name == null && uuid == null;
  }

  private void checkPermissions(CommandSourceStack source) throws CommandSyntaxException {
    // Refer: net.minecraft.world.entity.EntitySelector#checkPermissions
    if (!useSelector()) {
      return;
    }
    var permissions = GitParcelWorldSavedData.get(source.getServer()).getPermissions();
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
    return GitParcelLevelSavedData.get(source.getLevel())
        .streamParcels()
        .filter(getPredicate())
        .toList();
  }

  private Predicate<Parcel> getPredicate() {
    if (name != null) {
      return parcel -> name.equals(parcel.meta().name());
    } else if (uuid != null) {
      return parcel -> parcel.uuid().equals(uuid);
    } else {
      return Util.allOf(predicates);
    }
  }

  private List<Parcel> sortAndLimit(Vec3 pos, List<Parcel> list) {
    if (list.size() > 1) {
      order.accept(pos, list);
    }
    return list.subList(0, Math.min(maxResults, list.size()));
  }
}
