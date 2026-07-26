# Git Parcel 命令参考

本文记录当前正式命令。命令和未来的玩家界面调用同一套服务端用例；开发环境中的
`parceldebug` 不在本文范围内。

## 基本概念

- parcel：世界中的受管理区域。
- 快照：parcel 内容的一份不可变副本；实现上对应内部 Git commit。
- 当前基准：当前世界内容从哪个快照继续演化。
- 恢复：把快照应用到世界，并在成功后将它设为当前基准。
- 分叉：恢复旧快照后再次保存时自然形成的另一条路线。

普通命令只接受完整的快照 ID，不接受 `HEAD~1`、branch、tag 等任意 Git revision。
共享仓库的高级导入命令仍显式接受 Git revision。

## 选择 parcel

`/parcel` 的第一个参数是名称、UUID 或选择器：

```text
/parcel "Main House"
/parcel dd12be42-52a9-4a91-a8a1-11c01849e498
/parcel #p
/parcel #s
/parcel #a[limit=3]
```

- `#p` 选择最近的 parcel。
- `#s` 选择视线命中的 parcel。
- `#a` 选择当前维度的所有 parcel。
- 选择器支持 `name=<名称>` 和 `limit=<数量>`。

## 权限

世界权限区分列出/创建/删除 parcel、查看共享仓库、发布与导入，以及管理远程 Git
操作。parcel 权限独立区分：

| 权限 | 默认等级 | 用途 |
| --- | ---: | --- |
| VIEW | 1 | 查看 parcel 和快照树 |
| SAVE | 3 | 保存快照 |
| RESTORE | 3 | 恢复快照 |
| MANAGE_HISTORY | 4 | 后续的删除、squash 等破坏性能力 |
| CONFIG | 4 | 修改 parcel 的运行时配置 |

`save-first` 恢复同时要求 SAVE 和 RESTORE。发布要求 VIEW 及世界的发布/导入权限。

## 创建与查看

```text
/parcels create <from> <to> <name> [mirror] [rotation]
/parcels formats
/parcel <selector>
/parcel <selector> teleport [players]
/parcel <selector> delete
```

创建只注册区域，不会隐式保存。删除默认只删除世界注册关系，内部 bare 仓库会保留，
以便备份或人工恢复。

## 保存快照

```text
/parcel <selector> save [name]
```

该命令原子地执行“捕获世界内容、按格式写入临时 NIO 工作区、校验文件树、写入 Git
对象并创建 commit、更新保留 ref 和当前基准”。不再存在 `save`/`commit` 两步流程。

即使内容和父快照相同，显式保存也会创建一个新快照。当前基准若是较早节点，新快照
会成为它的子节点，原有后代不会被删除。

示例：

```text
/parcel #p save
/parcel "Main House" save Add second floor
```

## 快照树

```text
/parcel <selector> history [limit]
```

按稳定游标使用的顺序显示快照节点。每项包含快照 ID、父 ID、创建时间、作者和名称；
`*` 表示当前基准。命令默认显示 20 项，最大 100 项。仓库读取由统一后台操作执行，
完成后再输出结果；网络 API 还支持继续分页。

## 恢复快照

```text
/parcel <selector> restore <snapshot_id>
/parcel <selector> restore <snapshot_id> save-first
/parcel <selector> restore recover <operation_uuid> retry
/parcel <selector> restore recover <operation_uuid> rollback
```

`snapshot_id` 必须是属于该 parcel 的完整对象 ID。直接恢复不会猜测实时世界是否存在
未保存变化；`save-first` 会先无条件创建一个保护快照，再执行恢复。
恢复和恢复操作处理都要求选择器恰好匹配一个 parcel。

恢复先完整物化并验证快照，随后写入世界；只有世界写入成功后才移动当前基准。写世界
期间会由 `refs/gitparcel/operations/*` 记录阶段、目标和恢复前快照。若中途失败或服务器
崩溃，启动审计会报告未完成操作，不会假装恢复成功。

管理员可从日志中取得未完成操作 UUID。`retry` 重新应用原目标快照；`rollback` 仅在该
操作记录了恢复前快照时可用，并尽力把它重新应用到世界。两者成功后才清除旧操作记录。

## 配置

```text
/parcel <selector> config set <key> <value>
```

现有 key 包括 `meta.format`、`meta.name`、`meta.author`、`meta.description`、
`meta.excludeEntities`、`visual.showWireframe` 和 `visual.showAnchor`。权限、视觉设置、变换
以及所属维度是运行时 parcel 属性，不会因恢复内容快照而回退。

## 共享仓库

```text
/parcels repositories [list]
/parcels repositories create <repository>
/parcels repositories clone <repository> <https_url>
/parcels repositories fetch <repository>
/parcels repositories pull <repository>
/parcels repositories push <repository>
```

共享仓库使用普通工作树、branch、tag 和 remote。`pull` 只更新外部仓库，不会自动修改
世界或内部快照。

可分发仓库根部使用版本化 `gitparcel.json`：

```json
{
  "schema_version": 1,
  "parcels": ["spawn/main-house", "spawn/tower"]
}
```

路径必须规范化、唯一、互不包含，不能包含 `.git` 或逃逸仓库。导入时会从所选 commit
读取清单，并校验清单与该 commit 中实际存在的 `parcel.json` 目录完全一致。

## 发布

```text
/parcel <selector> publish <repository> <path> [message]
```

发布当前基准快照，不会读取未保存的实时世界。服务端把该内部 commit 的文件树写入共享
仓库的新路径，同时更新 `gitparcel.json` 并创建共享仓库 commit。发布不会绑定 parcel，
也不会改变本地历史的权威性。

如需发布刚完成的世界修改，先显式执行 `save`，再执行 `publish`。

## 导入

```text
/parcels import <repository> <revision> <path> <at> [mirror] [rotation]
```

导入显式指定共享仓库 revision 和清单中的 parcel 路径。服务端安全导出子树、校验格式
与几何，为新 parcel 创建独立内部 bare 仓库和根快照，然后把内容放入世界。新 parcel
不会绑定到共享仓库。

服务端用例还支持把外部内容导入已有 parcel：它只创建当前基准的单父子快照，不会因
fetch/pull 自动写世界；之后仍需显式恢复该快照。

## 操作与进度

```text
/parcels operations
/parcels operations <operation_uuid>
```

统一操作管理器跟踪保存、恢复以及共享仓库任务。状态为 `QUEUED`、`RUNNING`、
`SUCCEEDED`、`FAILED` 或 `CANCELED`，并记录当前阶段、完成量、可选总量和单位。不同阶段
的单位不会被伪装成一个总百分比。

服务器最多保留 100 条近期结果；服务器关闭时取消仍在排队或执行的后台任务。

## 远程认证

远程 Git 使用服务端环境变量：

```text
GITPARCEL_GIT_USERNAME
GITPARCEL_GIT_TOKEN
```

受管远程默认只接受不含凭据、查询参数或片段的 HTTPS URL。凭据不会写入 URL、commit、
操作记录、网络 DTO 或日志。

## 常见流程

```text
# 创建并保存第一个快照
/parcels create ~ ~ ~ ~15 ~7 ~15 "Main House"
/parcel "Main House" save Initial version

# 从旧节点继续，形成分叉
/parcel "Main House" history
/parcel "Main House" restore <完整快照ID> save-first
/parcel "Main House" save Alternative roof

# 发布与显式导入
/parcels repositories clone team https://example.com/team/builds.git
/parcel "Main House" publish team spawn/main-house Initial publication
/parcels repositories push team
/parcels repositories pull team
/parcels import team HEAD spawn/main-house ~ ~ ~
```
