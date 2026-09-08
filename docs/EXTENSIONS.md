# Git Parcel 扩展指南

Git Parcel 通过 `GitParcelExtension` SPI（`java.util.ServiceLoader` 发现）向其他模组开放快照能力的扩展点。本文面向集成者，按"要处理什么数据"选择通道；架构背景见 [DESIGN.md](DESIGN.md)。

> 该 API 处于 Beta 阶段，暂不承诺二进制兼容；升级时请跟进仓库变更。

## 注册入口

实现 `GitParcelExtension` 并通过 ServiceLoader 注册（`@AutoService` 或手写 `META-INF/services/io.github.leawind.gitparcel.common.api.extension.GitParcelExtension` 资源文件）：

```java
public final class ExampleIntegration implements GitParcelExtension {
  @Override
  public String id() { return "examplemod:integration"; }

  @Override
  public void register(ParcelExtensionRegistrar registrar) {
    // 在这里注册内容类型、处理器、附件类型、声明字段、贡献者
  }
}
```

所有注册在启动时一次性完成并冻结；运行期不可再注册。

## 选择通道

| 你的数据 | 通道 |
|---|---|
| 一种全新的、可独立保存的内容（自有序列化格式） | 内容类型 `ParcelContentType` |
| 实体/方块实体 NBT 中的世界坐标（如蜂巢 `flower_pos`） | 声明式坐标字段 |
| 实体 NBT 中指向包裹内其他实体的 UUID（如拴绳） | 声明式实体引用字段 |
| 记录 NBT 的任意变换、私有侧数据 | 记录处理器 `ParcelRecordProcessor` |
| 引用世界外部存储的数据（如地图画、模组侧库存） | 附件（处理器 + `ParcelAttachmentType`） |
| 不被任何记录引用的区域性数据（模组 per-region SavedData） | 区域贡献者 `ParcelCaptureContributor` |

## 声明式字段

只需声明"哪个 NBT 路径是什么"，内置处理器（`gitparcel:declared_fields`）会在捕获时把世界坐标换算为包裹系、恢复时换算回去，并在恢复分配新 UUID 时重写实体引用。路径语法为点分段：复合键、`[]` 列表通配、`[n]` 下标，如 `Items[].tag.waypoint`。

```java
// 我的世界坐标字段：只作用于我的方块实体
registrar.registerCoordinateField(ParcelCoordinateField.forType(
    ParcelCoordinateField.Target.BLOCK_ENTITY,
    Identifier.fromNamespaceAndPath("examplemod", "anchor"),
    "home.pos",                       // NBT 路径
    ParcelCoordinateField.Encoding.BLOCK_POS)); // 或 BLOCK_POS_XYZ / POSITION

// 我的实体间引用字段
registrar.registerEntityRefField(ParcelEntityRefField.forType(
    Identifier.fromNamespaceAndPath("examplemod", "drone"),
    "master.UUID")); // 值须为 UUIDUtil.CODEC 的 int[4] 形式
```

语义约定：

- 坐标字段是**包裹系引用**，随包裹平移/镜像/旋转；指向包裹外的值也被换算（不做范围校验）。真正"世界外部"的语义应改用附件。
- 实体引用只在指向**同一批次恢复的实体**时重写；指向玩家或包裹外实体的引用保持原值。
- 声明字段同时作用于实体的 `Passengers` 子树（按嵌套实体自身的 `id` 过滤类型）。

内置声明：蜂巢/蜂箱的 `flower_pos`、拴绳 `leash`（UUID 与坐标两种变体）。

## 附件：世界外部数据随快照旅行

以内置的地图支持为例（`MapItemProcessor` + `MapDataAttachmentType`）：

1. **捕获**：处理器在 `captureEntity`/`captureBlockEntity` 中发现自己的物品/数据，调用 `context.requireCollector().collect(sourceIdentity, type, schemaVersion, required, payload)` 得到 `LocalAttachmentId`；把引用编码进 NBT（地图用的是物品 `minecraft:custom_data` 组件），**并保留原版字段作为降级路径**。
2. **持久化**：附件记录由内置 `attachments` 内容类型写进 `data/attachments/`，按 `(type, sourceIdentity)` 去重——同一张地图被多个物品引用只存一份。
3. **恢复**：附件先于方块/实体交付，`ParcelAttachmentType.restore` 把数据物化到目标世界（地图分配新的 `MapId` 并写入地图存储）并 `context.attachments().resolve(id, 值)`；随后处理器在 `restoreEntity`/`restoreBlockEntity` 里 `findResolved` 取回新值并改写 NBT。

恢复要求附件类型已注册且 `schemaVersion` 精确匹配，`required` 附件缺失即显式失败——升级 schema 时保留旧版本类型的注册，或接受旧快照不可读。

## 区域贡献者

数据不被任何实体/方块实体记录引用时（模组按区域存储的 SavedData 等），实现 `ParcelCaptureContributor`：

- `capture(context)`：在服务器线程运行，拿到 level、包裹空间、世界包围盒与附件收集器；收集的附件照常持久化（同样需要已注册的附件类型）。
- `restore(context)`：在全部内容（包括延迟提交的实体批次）应用后运行；`context.attachments()` 是本次恢复的全部附件记录，按自己的类型过滤。

## 已知限制

- 物品内嵌坐标（如探险家指南针的 `LodestonePos`）目前不在声明字段覆盖范围内——处理它的物品组件需要专门的处理器。
- 附件 schema 版本要求精确匹配，没有读路径迁移钩子；内容类型同理：旧版本保持注册即可读旧快照。
- 地图数据恢复到目标世界的 overworld 地图存储；跨维度语义以原数据为准，不做重定位。
