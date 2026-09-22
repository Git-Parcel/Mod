# BACKLOG

已识别、应当完成，但**排期与执行方式由人类决定**的工作事项。

## 使用规则

- 本文件记录"将来应该做"的事项；每条注明执行者类型（见下）。完成即删除条目，不保留历史。
- **Agent（AI 助手）不得自行启动其中事项**，除非人类在对话中明确指示；发现新的缺口时在对话中提出并经人类确认后记入本文件。
- 事项完成后，相关约束与背景应沉淀到 `AGENTS.md`（踩坑/指南）或 `docs/DESIGN.md`（设计），再删除条目。
- `docs/DESIGN.md` 中"文档可超前于实现"的部分不属于本文件——那里是设计已定、等待实现；本文件是设计未定或时机未决。

## 条目类型

- `[agent]` — Agent 可独立完成，人类仅决定何时做。
- `[human]` — 需要人类亲自完成（设计决策、外部沟通等）。
- `[agent-assist]` — 人类决策，Agent 可在决策后协助实现。

## 事项

### [agent-assist] 内容快照的跨 MC 版本迁移机制设计

26.1/26.3 与 1.20.1 产生的快照相互读取时，部分内容类型的落盘位置随版本不同（已知案例：`map_items` 处理器在 26.x 把附件引用写进物品 `custom_data` 组件、1.20.1 写进物品 `tag`；当前降级为静默丢失引用，数据不损坏）。DESIGN.md 的双轴版本模型只有 Minecraft 数据版本经 vanilla DataFixer 升级一条通路，内容类型的跨 MC 版本迁移没有机制。需要设计：迁移挂点（读取时按快照 `dataVersion` 分叉？布局版本 bump？）、覆盖范围（还有哪些处理器存在同类差异）、以及是否承诺跨大版本的快照互通。人类决策设计取向后，Agent 可协助实现。**人类指示：暂缓。**

### [human] 渲染路径统一的触发评估

parcel 线框在 26.x 走 vanilla gizmo 系统（`MixinLevelRenderer` → `Gizmos`），1.20.1 走 `RenderType.lines()` 手绘（无自定义线宽）。维持分叉的前提是 gizmo 仍是"无需调试开关即渲染"的官方入口。出现以下任一信号时应评估统一到 26.x 自绘（含自定义 `RenderType` 线宽支持，而非上提 1.20.1 的降级实现）：

- vanilla gizmo API 再次破坏性变更（如 26.1→26.3 的 submit 重构）；
- vanilla 为 gizmo 增加调试开关使其不再适合承载产品渲染。

几何计算已由 `ParcelRenderState.generate()` 共享，分叉的只有端点到像素的绘制步骤。可选的先行重构：把几何计算从 `ParcelRenderState` 拆为无版本依赖的独立类，两个绘制分支消费它。

### [agent] NeoForge 26.3 依赖升级为正式版

`versions/26.3-neoforge/gradle.properties` 目前锁定 `26.3.0.6-beta`（截至 2026-09，26.3 系仅有 beta）。NeoForge 发布 26.3 正式版后更新该属性并验证构建。

### [agent] history 分页游标接通与截断提示

`history` 命令面只有 `[limit]`，树查询的游标在代码中固定为空（`HistorySubcommand` 传 `Optional.empty()`），超过 limit 的节点被静默截断且无法翻页；DESIGN.md 已要求"分页加载较大的树，同时保持游标稳定"。需要决策：在客户端树 UI 落地前，命令面先补 `[cursor]` 参数并在页脚回显下一页游标，还是只显示"共 M 条、已显示 N 条"。

### [agent] 面向玩家消息以 parcel 名称替代 UUID

保存成功、history 头部、operations 的 target 等消息用 parcel UUID 作为身份展示，与"免 Git 心智"的目标用户定位不符（玩家的交互凭据是名称）。改为 `meta.name()`，UUID 退到悬停提示或括号内。

### [agent] 未完成恢复操作的可发现性

`/parcels operations` 只列内存中的近期操作，服务器崩溃重启后列表为空；未完成恢复只存在于日志与 operation ref 中。应把启动审计发现的 pending 恢复合并展示（或增加 pending 子命令），并为 `restore recover` 的 operation_id 提供补全。

### [agent] parcels contents 改名或迁移

该命令实际列出内容类型注册表（blocks/entities/scheduled_ticks/attachments），名称读作"parcel 里有什么"对玩家有误导。改为 `content-types` 或挪入 `parceldebug`。

### [agent] 重名场景下的选择器消歧

已决策允许 parcel 重名（2026-09）。两处后续：(1) `#a[name=X]` 目前强制 `maxResults=1`，重名时静默截断成一个，应按谓词语义返回全部匹配；(2) 要求恰好一个 parcel 的命令在重名失败时，错误消息应列出候选（位置、维度）帮助消歧，而不是只报"匹配过多"。
