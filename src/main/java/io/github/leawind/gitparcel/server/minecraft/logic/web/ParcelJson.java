package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentManifest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedContent;
import java.util.Locale;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Builders for the JSON projections returned by the API endpoints. */
final class ParcelJson {
  private ParcelJson() {}

  static JsonObject parcel(Parcel parcel, String dimension) {
    var meta = parcel.meta();
    var transform = parcel.transform();
    var box = parcel.getBoundingBox();

    var o = new JsonObject();
    o.addProperty("uuid", parcel.uuid().toString());
    o.addProperty("dimension", dimension);
    o.add("name", stringOrNull(meta.name()));
    o.add("description", stringOrNull(meta.description()));
    o.add("author", stringOrNull(meta.author()));
    o.add("tags", meta.tags() == null ? new JsonArray() : ApiJson.stringArray(meta.tags()));
    o.addProperty("excludeEntities", meta.getExcludeEntities());
    o.addProperty("dataVersion", meta.dataVersion());
    o.add("contents", ApiJson.stringArray(meta.contents().keySet()));

    o.addProperty("mirror", lowerName(transform.mirror()));
    o.addProperty("rotation", lowerName(transform.rotation()));
    o.add("anchorWorld", vec3(transform.translation()));
    o.add("anchorParcel", vec3(meta.anchor()));

    o.add(
        "bounds",
        prop(
            "from", vec3(new Vec3i(box.minX(), box.minY(), box.minZ())),
            "to", vec3(new Vec3i(box.maxX(), box.maxY(), box.maxZ()))));
    o.add("sizeParcel", vec3(meta.size()));
    o.add("sizeWorld", vec3(parcel.getSizeWorldSpace()));

    var visual = parcel.visual();
    o.add(
        "visual",
        prop("showWireframe", visual.showWireframe(), "showAnchor", visual.showAnchor()));

    var sectionSize = sectionSizeOf(parcel);
    o.add("sectionSize", sectionSize == null ? JsonNull.INSTANCE : new JsonPrimitive(sectionSize));

    o.add("archiveSync", parcel.archiveSync().map(ParcelJson::archiveSync).orElse(JsonNull.INSTANCE));
    return o;
  }

  static JsonObject snapshotNode(SnapshotNode node) {
    var o = new JsonObject();
    o.addProperty("id", node.id().value());
    o.add("parentId", idOrNull(node.parentId()));
    o.addProperty("name", node.name());
    o.addProperty("description", node.description());
    o.addProperty("author", node.author());
    o.addProperty("createdAt", node.createdAt());
    o.addProperty("source", lowerName(node.source()));
    o.add(
        "content",
        prop("files", node.content().files(), "bytes", node.content().bytes()));
    return o;
  }

  static JsonObject treePage(SnapshotTreePage page) {
    var nodes = new JsonArray();
    for (var node : page.nodes()) {
      nodes.add(snapshotNode(node));
    }
    var o = new JsonObject();
    o.addProperty("parcelUuid", page.parcelUuid().toString());
    o.add("nodes", nodes);
    o.add("current", idOrNull(page.current()));
    o.add("nextCursor", idOrNull(page.nextCursor()));
    page.error().ifPresent(error -> o.addProperty("error", error));
    return o;
  }

  static JsonObject operation(OperationSnapshot snapshot) {
    var o = new JsonObject();
    o.addProperty("operationId", snapshot.operationId().toString());
    o.addProperty("kind", snapshot.kind());
    o.addProperty("owner", snapshot.owner());
    o.addProperty("target", snapshot.target());
    o.addProperty("state", lowerName(snapshot.state()));
    o.addProperty("phase", snapshot.phase());
    o.addProperty("completed", snapshot.completed());
    o.add(
        "total",
        snapshot.total().<JsonElement>map(JsonPrimitive::new).orElse(JsonNull.INSTANCE));
    o.add("unit", stringOrNull(snapshot.unit().orElse(null)));
    o.addProperty("submittedAt", snapshot.submittedAt());
    o.add("startedAt", stringOrNull(snapshot.startedAt().orElse(null)));
    o.addProperty("updatedAt", snapshot.updatedAt());
    o.add("completedAt", stringOrNull(snapshot.completedAt().orElse(null)));
    o.add("result", stringOrNull(snapshot.result().orElse(null)));
    o.add("error", stringOrNull(snapshot.error().orElse(null)));
    o.add(
        "errorCode",
        snapshot
            .errorCode()
            .<JsonElement>map(code -> new JsonPrimitive(lowerName(code)))
            .orElse(JsonNull.INSTANCE));
    return o;
  }

  static JsonObject repository(String name, SharedContent.RepoInfo info) {
    var o = new JsonObject();
    o.addProperty("name", name);
    o.addProperty("type", info.type());
    o.add("remoteUrl", stringOrNull(info.remoteUrl()));
    o.add("lastSync", stringOrNull(info.lastSync()));
    return o;
  }

  static JsonObject player(ServerPlayer player) {
    var o = new JsonObject();
    o.addProperty("uuid", player.getUUID().toString());
    o.addProperty("name", player.getName().getString());
    return o;
  }

  static String dimensionId(ServerLevel level) {
    /*? if >=26.1 {*/
    return level.dimension().identifier().toString();
    /*?} else {*/
    /*return level.dimension().location().toString();
     *//*? }*/
  }

  private static JsonElement archiveSync(Parcel.ArchiveSync sync) {
    return prop(
        "size", vec3(sync.size()),
        "anchor", vec3(sync.anchor()),
        "repositorySizeBytes", sync.repositorySizeBytes());
  }

  private static Integer sectionSizeOf(Parcel parcel) {
    ParcelContentManifest manifest = parcel.meta().contents().get(BlockContentType.ID);
    if (manifest == null || manifest.config() == null || !manifest.config().isJsonObject()) {
      return null;
    }
    var config = new BlockContentType.Config();
    config.setFromJson(manifest.config().getAsJsonObject());
    return config.sectionSize.get().edgeLength();
  }

  private static JsonElement idOrNull(java.util.Optional<SnapshotId> id) {
    return id.<JsonElement>map(value -> new JsonPrimitive(value.value())).orElse(JsonNull.INSTANCE);
  }

  private static JsonElement stringOrNull(String value) {
    return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
  }

  private static String lowerName(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }

  static JsonArray vec3(Vec3i v) {
    var array = new JsonArray();
    array.add(v.getX());
    array.add(v.getY());
    array.add(v.getZ());
    return array;
  }

  static JsonObject prop(String key1, Object value1, String key2, Object value2) {
    var o = new JsonObject();
    add(o, key1, value1);
    add(o, key2, value2);
    return o;
  }

  static JsonObject prop(
      String key1, Object value1, String key2, Object value2, String key3, Object value3) {
    var o = prop(key1, value1, key2, value2);
    add(o, key3, value3);
    return o;
  }

  private static void add(JsonObject o, String key, Object value) {
    if (value instanceof Integer i) {
      o.addProperty(key, i);
    } else if (value instanceof Long l) {
      o.addProperty(key, l);
    } else if (value instanceof Boolean b) {
      o.addProperty(key, b);
    } else if (value instanceof String s) {
      o.addProperty(key, s);
    } else if (value instanceof JsonArray a) {
      o.add(key, a);
    } else if (value instanceof JsonObject obj) {
      o.add(key, obj);
    } else {
      throw new IllegalArgumentException("Unsupported property type: " + value.getClass());
    }
  }
}
