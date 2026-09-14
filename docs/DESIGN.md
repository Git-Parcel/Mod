# Git Parcel 设计文档

## 文档定位

本文描述 Git Parcel 的目标设计，而不是当前代码的实现清单。命令参考与扩展指南合并于本文；语境语义的形式化规约（定义、不变式与验收性质）单独位于 SEMANTICS.md，本文与实现都应服从它。文档可以包含尚未实现的能力；当代码与本文冲突时，后续开发应优先向本文收敛。决策与理由成对出现：理由是后续决策的先例，新决策应与已有理由一致。

当前项目尚未发布稳定版本，不要求兼容旧的命令、API、磁盘布局或存档数据。为了得到更简单且更安全的产品模型，可以删除或替换现有实现。

Git Parcel 是服务端权威的 Minecraft 模组。它把世界中的长方体区域抽象为 parcel，并使用 Git 保存和管理 parcel 的每一个快照。普通玩家不需要理解 Git；模组把 Git commit、ref 和对象数据库包装成保存快照、恢复快照和管理树状历史等产品概念。

## 目标用户与产品原则

主要用户是建筑玩家和原版生电玩家。基础功能必须能够只用以下概念解释：

- parcel：世界中的一个受管理区域。
- 快照：某一时刻 parcel 内容的不可变副本。
- 当前基准：当前世界内容从哪个快照继续演化。
- 恢复：把某个快照的内容重新应用到世界，并把它设为新的当前基准。
- 分叉：恢复较早快照后再次保存，自然产生的另一条发展路线。
- 快照树：用父子关系展示所有保存结果。

基础界面和基础命令不应要求玩家理解工作树、暂存区、提交、HEAD、revision、rebase 或 detached HEAD。Git 术语只出现在明确标记的高级功能中。理由：目标用户不了解也不打算了解 Git，任何 Git 概念泄漏都会把模组的主要价值（免 Git 心智）抵消掉。

设计优先级如下：

- 保存当前 parcel 为快照。
- 查看和管理易于理解的树状快照历史。
- 安全地恢复任意快照，并能从旧快照继续形成分叉。
- 保证数据安全、服务端一致性和可诊断的失败结果。
- 在不污染基础交互的前提下提供常用 Git 能力。
- 为 compare、squash、发布和协作等后续能力保留扩展空间。

## 核心设计决策

| 问题                          | 决策                                                                    | 理由                                                                                 |
| ----------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| 快照的本质                    | 快照只是 Git commit 面向普通玩家的别名，不建立第二套模型                | 两套模型必然漂移；不可变性、内容寻址与 DAG 已由 Git 提供                             |
| 普通玩家的保存动作            | 保存快照是一次原子用例，不再拆成 save 和 commit                         | 两步流程暴露 Git 心智模型；"已 save 未 commit"的中间状态没有玩家可理解的语义         |
| 存档内历史模型                | 每个 parcel 拥有一个单父节点的逻辑快照树                                | 普通玩家的心智模型是一条可回退的历史线；多父 DAG 只由显式的高级能力暴露              |
| 分叉的产生方式                | 恢复较早快照后保存，新快照成为该旧快照的子节点                          | 分叉是恢复的自然结果，不需要玩家理解 branch                                          |
| 存档内物理存储                | 每个 parcel 使用文件系统中的一个独立 bare Git 仓库目录                  | 独立仓库换取故障、并发和删除边界的完全隔离；bare 形态没有工作树中间状态              |
| 存档内仓库是否是标准 Git 仓库 | 是；由 JGit 直接管理对象、commit 和 ref，不使用虚拟文件系统             | 玩家可用标准 Git 工具检查与备份；JGit 文件仓库实现也只支持默认文件系统               |
| 格式输出目标                  | 格式通过属于任意 `java.nio.file.FileSystem` 的 `Path` 读写快照树        | 一致性与路径逃逸测试可以对 Jimfs、ZipFS 等提供者运行，而不依赖本地文件系统特性       |
| Git 的职责                    | 内部快照、树状历史、发布、导入、远程同步和高级操作都建立在 Git 上       | 内容寻址、去重、压缩与历史查询不必重造                                               |
| 外部仓库与实时世界的关系      | 外部 Git 内容不会自动成为 parcel 的权威存储，也不会因 pull 自动修改世界 | 远程操作既不可信也不原子；内容进入世界必须经显式导入与恢复，失败半径可控             |
| 世界变化判断                  | 不提供额外的 dirty 查询；显式保存总是完整捕获并创建 commit              | 准确判断需要完整捕获，与保存同价；增量缓存难以覆盖实体与模组变化，漏报比多保存更危险 |
| 权威位置                      | 世界状态、权限、快照操作和外部同步均由服务端裁决                        | 服务端是唯一能同时看到世界与仓库的一方；客户端投影只用于渲染和交互                   |

每个内部 parcel 仓库都有独立的对象数据库和 ref namespace。内部仓库完全由模组管理，只开放基础快照能力；它在磁盘上仍是标准 Git 仓库，可以使用 Git 工具检查和备份，但不支持玩家绕过模组直接修改 refs 或历史。

## 非目标

- 不把存档内 Git 仓库暴露成由玩家手工维护的工作仓库。
- 不在基础快照树中支持多父节点或把 Git DAG 伪装成树。
- 不让客户端直接读取 Git 仓库、凭据或服务端文件。
- 不在 pull、fetch 或外部文件变化后自动修改实时世界。
- 不在一次普通恢复中隐式执行合并、历史重写或删除其他分叉。
- 不兼容当前每个 parcel 一个带工作树的 `.git` 子目录、`save`/`commit` 两步工作流或直接仓库路径位置。
- 不在核心用例稳定前让具体 UI 控件反向决定领域接口。
- 不为兼容未声明的语境依赖数据而尝试猜测其语义（SEMANTICS.md 公理 2）。

## 总体架构

```text
命令 / Modern UI
        |
        v
服务端用例层
SaveSnapshot / RestoreSnapshot / QuerySnapshotTree / PublishSnapshot
        |
        +----------------------+-----------------------+
        |                      |                       |
        v                      v                       v
Minecraft 世界适配       InternalRepository      SharedRepository
捕获、校验、放置          受限的内部快照操作         完整的共享 Git 能力
        |                      |                       |
        v                      +-----------+-----------+
内容类型 + NIO 工作区                  GitRepositoryCore
                                         JGit 原语
```

各层职责如下：

- 领域层以适合普通玩家的名称投影 Git commit、父子关系、作者、操作请求和结构化结果，不依赖具体加载器。
- 用例层编排权限、锁、世界捕获、内容编解码、Git 对象与 ref 更新、恢复和网络同步。
- 内容类型各自负责一种可保存和加载的 parcel 内容，不拥有世界、仓库或事务生命周期。
- `GitRepositoryCore` 封装内部和共享仓库共用的 JGit 原语、安全校验、锁和结果模型。
- `InternalRepository` 在公共实现上施加更严格的能力策略，并强制执行单父快照树和模组私有 ref 规则。
- `SharedRepository` 复用同一实现，另外开放工作树、branch、tag、remote 和多父历史等能力。
- Fabric 和 NeoForge 只适配生命周期、命令、网络和版本特有 Minecraft API。
- 客户端保存服务端下发的只读投影，用于渲染和交互，不成为权威状态来源。

内部仓库允许的 Git 操作是共享仓库操作的真子集。两者复用实现而不复用权限：能力策略必须在服务端入口检查，不能只靠 UI 隐藏命令。这样既避免维护两套 Git 代码，也不会把多父提交、暂存区或工作树状态泄漏给普通玩家。

## 领域模型

### Parcel 定义

`Parcel` 表示当前世界中的受管理区域，至少包含：

- 稳定 UUID。
- 所属维度和空间范围。
- 锚点与当前放置变换。
- 名称、说明和视觉配置。
- parcel 权限配置。
- 新快照默认使用的格式及配置。

Parcel 定义不再保存直接 Git 仓库路径或"当前内容位于共享仓库"的位置。内部逻辑仓库由世界 ID 和 parcel UUID 隐式定位。理由：仓库路径属于存储实现，写进领域数据会让 parcel 定义与磁盘布局耦合，导入和迁移都要拖着它走。外部发布关系属于单独的交换模型，不能改变本地历史的权威性。

权限、视觉设置、当前变换等运行时属性不随内容恢复而回退。理由：恢复建筑内容不应意外恢复旧权限或把 parcel 移到旧位置——快照记录的是内容，不是管理状态。

### 快照与 Git commit

快照不是独立于 Git 的领域对象，而是 Git commit 面向普通玩家的别名：

```text
快照 = Git commit
快照 ID = Git commit object ID
快照内容 = commit tree
快照父节点 = commit parent
快照名称和说明 = commit message
快照作者和时间 = commit author/committer 元数据
```

快照没有可变注释、额外数据库记录或第二套标识。属于快照的全部内容都必须由该 commit 自身携带；修改名称、说明、作者、时间、父节点或内容都会产生新的 commit ID。基础功能不提供"原地编辑快照"。理由：可编辑的快照元数据会造成 commit ID 与玩家所见信息不一致，需要额外的同步与失效机制。

网络协议可以把 commit object ID 包装为 `SnapshotId` 以表达用途和阻止客户端随意拼接 revision，但该类型不引入新身份。协议不能假设 Git 始终使用 SHA-1，也不能把任意客户端 revision 当作已经鉴权的 commit。

Parcel UUID 属于 Minecraft 存档中的上层注册关系，不写入内部 Git config、commit message、commit tree 或其他仓库数据。内部仓库目录由上层通过 parcel UUID 定位，但仓库本身应可以脱离原世界复制、检查和导入。可移植内容中的名称、尺寸、格式和依赖等描述不等于运行时 parcel UUID。

### 快照树不变量

- 一个内部仓库只允许一个无父节点的根 commit。
- 除根 commit 外，每个快照 commit 恰好有一个属于同一仓库的父 commit。
- commit 一经创建不可修改；任何修改都创建新 commit。
- 每个 parcel 至多有一个当前基准；尚未首次保存时可以没有。
- 非当前分叉不会因恢复或保存而变得不可达。
- 基础操作不能生成合并节点。

子节点由查询父 ID 得到，不需要 Git branch ref 才能保留。UI 可以直接根据父子关系布局完整树。理由：保留 ref 只服务于 Git GC 可达性；树的形状由 commit 父关系唯一决定，避免 ref 与 DAG 双重事实来源。

## 基础用例语义

### 保存快照

保存快照是普通玩家的主要写入操作：

1. 服务端校验 parcel、权限、格式能力和操作互斥状态。
2. 从 Minecraft 世界捕获内容，并在临时 NIO 工作区生成完整快照文件树。
3. 重新检查文件树安全性和快照清单，使用 JGit `ObjectInserter` 将文件直接写成 blob 并构造候选 tree；重复对象写入按 object ID 自然去重。
4. 使用候选 tree 创建一个以当前基准为唯一父节点的 commit；即使 tree 与父 commit 相同，玩家明确执行的"保存快照"仍产生 commit。
5. 先建立快照保留 ref，再以 compare-and-set 方式移动当前基准 ref。ref 更新成功后向客户端发送结构化结果和树的增量变化。

首次保存产生根快照。若当前基准是较早快照，保存会在该节点下创建新子节点，原有后代完整保留，因此自然形成分叉。

理由：即使内容未变也创建 commit，是因为系统不判断世界是否变化（见核心决策表），拒绝创建会让"保存成功"的语义依赖隐式猜测；空 diff 的成本只是一个 commit 对象。

格式输出仍应尽可能确定：相同的 parcel 内容和配置应生成相同的规范化文件内容。实体枚举顺序、JSON 属性顺序或格式自行写入的时间戳不应制造无意义 Git diff，并会降低对象去重率；但保存流程不依赖确定性输出来判断世界是否"已修改"。瞬态与派生字段的消除与相对化（SEMANTICS.md 规则 2.2、规则 2.3）在语义层消灭剩余噪声源。

### 恢复快照

恢复的含义是"把所选快照应用到当前世界，并从这里继续"，而不是 Git checkout。

1. 校验查看和恢复权限、快照归属、格式支持、尺寸与锚点兼容性。
2. 不检测实时世界是否相对当前基准发生变化。恢复前由用户明确选择"直接恢复"或"先保存快照再恢复"；基础 UI 应把后者作为更安全的操作呈现，但不能谎称已经自动判断出变化。
3. 从内部仓库把目标 commit tree 流式物化到 NIO 工作区，完整验证后再开始写世界。
4. 将目标内容应用到世界。成功后才把目标 commit 设为当前基准。
5. 如果写世界失败，保留结构化的未完成操作记录供重试；只有用户事先选择了保存，系统才拥有可用于尽力回滚的恢复前 commit。

内容应用采用替换语义：方块区域整体覆盖；实体先按捕获谓词（AABB 相交、非玩家、非乘客的根实体，乘客树随根）清除区域内旧实体，再批次生成快照实体；玩家永不清除。恢复完成后区域内的实体集合与快照实体集同构（SEMANTICS.md 规则 6.2、不变式 6.2）。理由：方块已是覆盖语义，实体若只追加，重复恢复同一快照会让实体翻倍；清除谓词与捕获谓词是同一函数，往返不变性才能对实体集合成立。

Minecraft 世界写入和 Git ref 更新无法组成真正的跨系统事务。因此仓库中要用模组私有 ref 记录恢复操作的阶段、目标和可选的恢复前 commit。服务器若在写世界期间崩溃，下次启动不得假装操作已经成功；应将该 parcel 标记为需要恢复，并允许重试目标，或在存在恢复前 commit 时回滚。

恢复不会删除节点、重写父关系或创建多父节点。恢复旧节点后下一次保存才会建立新分叉。

### 管理快照树

第一阶段需要支持：

- 按树查询快照、当前基准和子节点。
- 分页加载较大的树，同时保持游标稳定。
- 查看 commit message、创建者、时间、来源和内容概要。
- 恢复所选快照。

后续可以加入 compare、分叉命名、导出、删除和 squash。收藏、UI 颜色等纯界面偏好如果需要，属于客户端或玩家配置，不能伪装成快照内容。删除与 squash 属于破坏性或历史重写操作，必须有单独权限、影响预览、二次确认和可恢复保留期。squash 应创建新的替代链或替代 commit，旧 commit 先进入可恢复状态，不能立即物理清除对象。理由：Git 的不可变性让"删除"只能是重定向可达性，物理清除与玩家对历史的预期冲突，保留期是唯一的后悔药。

## 命令

命令和未来的玩家界面调用同一套服务端用例，命令文本不是服务端 API（见"网络与 UI 边界"）。开发环境中的 `parceldebug` 不在本文范围。

普通命令只接受完整的快照 ID，不接受 `HEAD~1`、branch、tag 等任意 Git revision。理由：revision 语法把 Git 心智模型暴露给普通玩家，且任意 revision 字符串不能当作已鉴权的 commit 处理。共享仓库的高级导入命令仍显式接受 Git revision，因为它的目标用户已声明理解 Git。

### 选择 parcel

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

理由：要求玩家输入 UUID 不现实；空间选择器（最近、视线）与玩家的空间心智一致。

### 创建与查看

```text
/parcels create <from> <to> <name> [mirror] [rotation]
/parcels contents
/parcel <selector>
/parcel <selector> teleport [players]
/parcel <selector> delete
```

创建只注册区域，不会隐式保存。理由：注册与内容捕获是两个失败半径完全不同的操作，隐式保存会把"圈了一块地"放大成一次完整捕获。删除默认只删除世界注册关系，内部 bare 仓库会保留，以便备份或人工恢复；永久删除必须单独确认（破坏性操作分级原则）。

### 保存快照

```text
/parcel <selector> save [name]
```

原子地执行"捕获世界内容、按内容类型写入临时 NIO 工作区、校验文件树、写入 Git 对象并创建 commit、更新保留 ref 和当前基准"。不存在 `save`/`commit` 两步流程。

即使内容和父快照相同，显式保存也会创建一个新快照。当前基准若是较早节点，新快照会成为它的子节点，原有后代不会被删除。

```text
/parcel #p save
/parcel "Main House" save Add second floor
```

### 快照树

```text
/parcel <selector> history [limit]
```

按稳定游标使用的顺序显示快照节点。每项包含快照 ID、父 ID、创建时间、作者和名称；`*` 表示当前基准。命令默认显示 20 项，最大 100 项。仓库读取由统一后台操作执行，完成后再输出结果；网络 API 还支持继续分页。理由：大树的读取耗时不可控，同步执行会卡住命令线程，统一走操作管理器还能复用进度与限流。

### 恢复快照

```text
/parcel <selector> restore <snapshot_id>
/parcel <selector> restore <snapshot_id> save-first
/parcel <selector> restore recover <operation_uuid> retry
/parcel <selector> restore recover <operation_uuid> rollback
```

`snapshot_id` 必须是属于该 parcel 的完整对象 ID。直接恢复不会猜测实时世界是否存在未保存变化；`save-first` 会先无条件创建一个保护快照，再执行恢复。理由：恢复会覆盖世界且不可靠地可逆，保护快照是唯一由事务保证可用的回滚资本，因此把它做成显式选项而不是隐式行为。恢复和恢复操作处理都要求选择器恰好匹配一个 parcel。

恢复先完整物化并验证快照，随后写入世界；只有世界写入成功后才移动当前基准。写世界期间会由 `refs/gitparcel/operations/*` 记录阶段、目标和恢复前快照。若中途失败或服务器崩溃，启动审计会报告未完成操作，不会假装恢复成功。

管理员可从日志中取得未完成操作 UUID。`retry` 重新应用原目标快照；`rollback` 仅在该操作记录了恢复前快照时可用，并尽力把它重新应用到世界。两者成功后才清除旧操作记录。理由：崩溃恢复的两个动作对应"继续目标"与"回到恢复前"两种意图，隐式选择任一都是猜测。

### 配置

```text
/parcel <selector> config set <key> <value>
```

现有 key 包括 `content.blocks.sectionSize`（仅接受 `16` 或 `32`）、`meta.name`、`meta.author`、`meta.description`、`meta.excludeEntities`、`visual.showWireframe` 和 `visual.showAnchor`。权限、视觉设置、变换以及所属维度是运行时 parcel 属性，不会因恢复内容快照而回退。

### 共享仓库

```text
/parcels repositories [list]
/parcels repositories create <repository>
/parcels repositories clone <repository> <https_url>
/parcels repositories fetch <repository>
/parcels repositories pull <repository>
/parcels repositories push <repository>
```

共享仓库使用普通工作树、branch、tag 和 remote。`pull` 只更新外部仓库，不会自动修改世界或内部快照；内容进入世界必须经显式导入与恢复。

可分发仓库根部使用版本化 `gitparcel.json`，规范见"可分发仓库规范"。

### 发布

```text
/parcel <selector> publish <repository> <path> [message]
```

发布当前基准快照，不会读取未保存的实时世界。服务端把该内部 commit 的文件树写入共享仓库的新路径，同时更新 `gitparcel.json` 并创建共享仓库 commit。发布不会绑定 parcel，也不会改变本地历史的权威性。理由：快照是唯一经过完整校验的内容；发布未保存的世界状态会绕过格式校验并让"已发布"与"已保存"脱钩。如需发布刚完成的世界修改，先显式执行 `save`，再执行 `publish`。

### 导入

```text
/parcels import <repository> <revision> <path> <at> [mirror] [rotation]
```

导入显式指定共享仓库 revision 和清单中的 parcel 路径。服务端安全导出子树、校验格式与几何，为新 parcel 创建独立内部 bare 仓库和根快照，然后把内容放入世界。新 parcel 不会绑定到共享仓库。

服务端用例还支持把外部内容导入已有 parcel：它只创建当前基准的单父子快照，不会因 fetch/pull 自动写世界；之后仍需显式恢复该快照。

### 操作与进度

```text
/parcels operations
/parcels operations <operation_uuid>
```

统一操作管理器跟踪保存、恢复以及共享仓库任务。状态为 `QUEUED`、`RUNNING`、`SUCCEEDED`、`FAILED` 或 `CANCELED`，并记录当前阶段、完成量、可选总量和单位。不同阶段的单位不会被伪装成一个总百分比。

服务器最多保留 100 条近期结果；服务器关闭时取消仍在排队或执行的后台任务。

### 远程认证

远程 Git 使用服务端环境变量：

```text
GITPARCEL_GIT_USERNAME
GITPARCEL_GIT_TOKEN
```

受管远程默认只接受不含凭据、查询参数或片段的 HTTPS URL。凭据不会写入 URL、commit、操作记录、网络 DTO 或日志。理由：Git URL 会随 remote 配置、ref 记录和日志扩散，凭据一旦进入 URL 就几乎无法收回。

### 常见流程

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

## 内部 Git 仓库存储

### 仓库布局

每个 parcel 使用一个由 JGit 直接打开的独立 bare repository：

```text
<world>/gitparcel/parcels/<parcel-uuid>.git/
├── HEAD
├── config
├── objects/
├── refs/
└── packed-refs
```

选择 bare repository 而不是带工作树的仓库，原因如下：

- parcel 的实时工作状态在 Minecraft 世界中，不需要第二份长期工作树。
- 保存快照可以直接创建 Git blob、tree 和 commit，不需要 `git add`、index 或 checkout。
- 恢复可以直接读取指定 commit tree，不移动文件系统工作树。
- 不会留下"工作树已更新但尚未提交"或 index 污染等中间状态。
- 仓库目录仍是标准 Git 仓库，可以使用 `git fsck`、clone 和常规备份工具检查。

JGit 的文件仓库实现依赖默认文件系统和 `java.io.File`。因此内部 Git 仓库明确位于 Minecraft 世界目录所在的受支持本地文件系统中，不尝试通过自定义 `FileSystem`、SQLite 虚拟文件系统或临时仓库镜像来适配。格式工作区支持任意 NIO `FileSystem`，不代表 JGit 仓库也支持。

### Commit 和 tree

每个内部 commit 的根 tree 就是一份完整 parcel 快照：

```text
parcel.json
config.json
data/
```

`parcel.json` 保存可移植内容的格式、尺寸、锚点、数据版本和依赖等信息，但不得保存 Minecraft 存档中的 parcel UUID、维度、权限、视觉配置或内部仓库路径。理由：这些是运行时注册关系而非内容属性；快照应可以在任意世界导入而不携带源世界的管理信息。

保存时遍历已经校验的 NIO 工作区，用 `ObjectInserter` 流式插入普通文件 blob，再自底向上创建 tree。最后用 `CommitBuilder` 创建 commit：

- 首个快照没有父 commit。
- 其他快照只有当前基准这一个父 commit。
- author 表示发起保存的玩家，committer 表示实际执行操作的服务端身份。
- commit message 保存快照名称、说明和必要的不可变来源信息。
- commit 时间来自服务端，不来自客户端。

Git 自己负责对象内容寻址、相同 blob/tree 去重、zlib 压缩和后续 pack。模组不再实现第二套 blob、分块或摘要数据库。跨 parcel 仓库不共享对象库，以换取故障、并发和删除边界的完全隔离；如将来确有必要，可以评估 Git alternates，但它不属于初始设计。

### 模组私有 refs

内部仓库至少使用以下 ref namespace：

```text
refs/gitparcel/current
refs/gitparcel/snapshots/<commit-id>
refs/gitparcel/operations/<operation-id>
```

- `current` 指向当前基准快照。
- 每个正式快照都有一个保留 ref，防止从旧节点分叉后原分支被 Git GC 视为不可达。
- operation ref 指向 tree 中包含操作阶段、目标和保护快照清单的内部 operation commit，用于恢复中断诊断。

仓库 `HEAD` 是指向 `refs/gitparcel/current` 的 symbolic ref，方便标准 Git 工具检查当前基准，但模组业务代码仍应显式使用私有 ref，不能依赖进程级 checkout 状态。

基础界面的"快照树"由所有正式快照 commit 的父关系生成，而不是由 Git branch 名称生成。分叉无需玩家创建或命名 branch。

保存新快照时先写入对象和 commit，再创建快照保留 ref，最后以旧 `current` object ID 为 expected value 更新 `current`。如果 compare-and-set 失败，说明并发状态已经变化，操作必须失败而不能覆盖新基准。对象写入是追加式的；ref 更新失败最多留下不可达对象或尚未激活的保留 ref，不会破坏旧历史。启动检查负责识别并报告这种未完成状态。

不允许通过基础功能创建多父 commit、删除任意 ref 或把 `current` 指向仓库外对象。直接使用外部 Git 工具修改内部 refs 属于不受支持的操作；模组检测到不满足树不变量的历史时进入只读故障状态，而不是自行重写玩家可能仍需恢复的数据。理由：模组无法区分"外部工具的合理整理"与"破坏玩家历史"，只读故障把决定权还给管理员。

### 维护与恢复

- 每个仓库使用独立锁；不同 parcel 仓库可以并行进行对象读取和安全计算。
- 不在 Minecraft 主线程执行大规模对象插入、pack、GC 或 `fsck`。
- 世界读取和世界写入仍必须在允许访问 Minecraft 状态的线程完成。
- 打开仓库时验证 bare 配置、必需 refs、commit/tree 类型和单父不变量。
- 定期或按需运行 JGit 对象一致性检查；失败时进入只读故障状态，不自动初始化空仓库覆盖旧目录。
- 自动 GC 只能在没有 parcel 操作时运行，并且所有正式快照和未完成操作都必须由 ref 保持可达。
- 删除 parcel 默认只删除世界中的注册关系并保留仓库目录；永久删除仓库必须单独确认并优先备份。
- 世界备份需要先阻止新的 ref 更新，再复制完整仓库目录；只复制部分 loose objects 或 refs 不是有效备份。
- 服务器关闭时完成或取消排队任务，关闭 JGit repository handle，并保留未完成 operation ref 供下次诊断。

## 内容类型与任意 NIO FileSystem

### 内容存储边界

快照的可移植文件树采用以下逻辑结构：

```text
parcel.json
data/
  blocks/
  entities/
  attachments/
  <extension-id>/
```

运行时负责 `parcel.json`、`data/` 根目录和事务生命周期。领域层的 `ParcelContentType` 表示一种可独立保存、加载和版本化的内容。其字符串 ID 同时是唯一目录名，每个类型只在 `data/<id>/` 下读写；同 ID 注册多个版本时，最高版本用于新快照，旧版本仍可读取对应历史快照。`parcel.json` 的 `contents` 清单记录每个目录的版本和可选配置。

内置类型包括 `blocks`、`entities` 和 `attachments`，扩展可注册任意其他类型。方块类型固定使用调色板、空间 RLE 和独立方块实体记录；仅 section 边长可配置为 16 或 32。实体和附件按记录保存。类型可声明加载依赖，保存时采用反向顺序。

内容操作上下文持有该类型独占的目标根 `Path`。该 `Path` 自带所属 `FileSystem`，因此 API 不需要额外接收默认文件系统。所有派生路径必须通过根路径的 `resolve` 或同一 `FileSystem` 创建。

内容类型实现必须遵守：

- 只使用 `java.nio.file.Path`、`Files`、流或 channel，不使用 `java.io.File`、`Path#toFile()` 或默认文件系统的 `Path.of(...)`。
- 不创建、缓存或关闭调用者拥有的 `FileSystem`。
- 不访问目标根以外的路径，不接受绝对归档路径或 `..` 逃逸。
- 只产生目录和普通文件，不产生符号链接、硬链接、设备或可执行语义。
- 不依赖 POSIX 权限、文件锁、watch service、原子移动或提供者特有属性。
- 关闭自己打开的流、channel 和目录迭代器。
- 接受空工作区，或者由明确基线提交物化得到的非空工作区；允许读取已有内容以维持稳定 ID 和文件布局。
- 将独占目录协调成完整结果：旧文件只有被该类型明确保留或重新产生时才能进入新快照，不能残留已失效的数据。
- 对相同的"基线树、当前 parcel 数据和配置"写出确定性的结果。

理由：以上约束使内容类型可以在任意 NIO 提供者（含测试用 Jimfs/ZipFS）上行为一致，并且让目录内容可以被安全地单独校验与删除。

支持目标的最低能力是创建目录、读写普通文件、列出目录、删除文件和顺序流式 I/O。只读文件系统只能作为读取源。原子替换是工作区或最终存储层的职责，不是内容类型能力。

### 工作区与 Git 对象之间的桥接

JGit `FileRepository` 不支持任意 NIO `FileSystem`，内容类型也不应直接依赖 JGit。用例层通过 `SnapshotWorkspaceFactory` 获得临时根路径：

```text
世界内容 -> 内容类型 -> NIO 临时文件树 -> 校验 -> Git blob/tree/commit
世界内容 <- 内容类型 <- NIO 临时文件树 <- 校验/导出 <- Git commit tree
```

保存已有 parcel 时，运行时先把 `refs/gitparcel/current` 指向的 commit tree 导出到临时工作区，并记住该 commit 作为基线；各内容类型随后在这个非空工作区上更新自己的目录。提交阶段必须再次确认 current ref 仍等于该基线，新 commit 也必须以它为父提交。基线已经变化时丢弃本次工作区并报告并发更新，不能把基于旧树生成的结果改挂到新父提交。

工作区可以来自默认文件系统、Jimfs、ZipFS 或其他满足契约的提供者。JGit 仓库始终留在其固定世界目录中；这里只把单次快照的普通文件流式写成 Git 对象，或者把一个 commit tree 流式导出到工作区，绝不复制、checkout 或重建整个仓库。

临时工作区的成本只与当前 parcel 快照大小相关，不与历史大小或 Git 仓库大小相关。若磁盘临时工作区仍成为性能瓶颈，可以让 `SnapshotWorkspaceFactory` 选择内存文件系统或未来提供直接构造文件树的实现，但不能通过来回复制 Git 仓库解决。

测试至少覆盖默认文件系统、Jimfs 和 ZipFS。针对每个内置内容类型，同一测试向不同提供者写入并读取，验证逻辑内容和规范化文件摘要一致。还要用自定义提供者或故障注入验证不支持 `ATOMIC_MOVE`、部分写入和关闭失败时的行为。

### 记录处理器、声明式条目与附件

世界内容除方块与实体本身外，还包含需要特殊处理的数据：嵌套在世界 NBT 中的坐标、实体之间的 UUID 引用、随时间流逝而无语义累积的瞬态数据、以及引用世界外部存储的数据（如地图）。这些数据类别与承载机制一一对应，全部语义由 SEMANTICS.md 规约：

- 记录处理器（`ParcelRecordProcessor`）：无状态、服务器线程，在捕获/恢复时逐记录变换实体与方块实体 NBT，可声明 `runAfter`/`runBefore` 顺序。处理器私有的版本化侧数据通过 `SemanticData` 挂在记录上。用于无法用路径声明表达的变换（如地图物品的组件改写、画的方向）。
- 声明式条目：声明"该 NBT 路径具有何种语境语义"，由内置声明字段处理器在捕获与恢复时统一执行，模组无需自己编写处理器。分三类：
  - 坐标字段（`ParcelCoordinateField`）：随 parcel 正交变换（平移、旋转、镜像，parcel 系语义）；
  - 实体引用字段（`ParcelEntityRefField`）：在恢复分配新 UUID 时按批次重写，指向 parcel 外的引用保持不变；
  - 瞬态与派生声明：变更不携带语义的易变字段在捕获时消除、恢复时取默认值；对全局时间轴的绝对引用改写为快照内偏移，恢复时重写回绝对时刻（SEMANTICS.md 定义 2.5、规则 2.2、规则 2.3）。

  理由：声明式把语境语义知识还给语义拥有方（原版字段由本模组声明、模组字段由模组声明），搬运工具只执行通用引擎。在搬运工具里硬编码类型检查的路线覆盖面永远落后于原版与模组演进，投影类模组的实践已经验证了这一点。
- 附件（`ParcelAttachmentType` + `ParcelAttachmentCollector`）：捕获时处理器或贡献者把世界外部数据收集为附件记录，经内置 `attachments` 内容类型随快照持久化；恢复时附件类型把数据重新物化到目标世界并通过上下文 `resolve` 回传。引用编码进 NBT 时必须保留原版值作为降级路径（例如地图物品保留原 `map_id`）。附件恢复要求类型已注册且 schema 版本精确匹配；引用不可解析时显式失败。理由：外部存储的内容不在记录里，NBT 句柄单独搬运必然指向错误数据；精确 schema 匹配保证"宣称能恢复的数据一定正确"，降级路径保证缺失时行为可预期而非崩溃。
- 区域贡献者（`ParcelCaptureContributor`）：覆盖不被任何记录引用的区域性模组数据——捕获钩子在附件排放前运行，恢复钩子在所有内容（含延迟提交的实体批次）应用后运行，并能看到本次恢复的全部附件记录。贡献者收集的附件同样需要已注册的附件类型。

实体恢复采用批次提交：数据汇缓冲全部实体记录，在 `commit()` 时机统一分配新 UUID、重写声明引用并生成。加载失败不会到达 `commit`，因此不会生成半批实体。理由：引用可能指向批次中尚未处理到的记录，逐条分配身份在理论上就走不通（SEMANTICS.md 不变式 4.1）。

### 声明装载

声明式条目的装载机制与行为类扩展（处理器、附件类型、贡献者、内容类型）正交，但全部进入同一组注册表、服从同一套语义（SEMANTICS.md）。装载源有四个：

- 内置声明文件：本模组 jar 内的原版声明，以数据文件维护，与代码分离。
- 模组携带声明：其他模组在 jar 内约定路径放置声明文件，零代码集成。
- 外部声明包：`config/gitparcel/declarations/` 下由玩家或社区维护的声明文件，支持运行时重载，用于为不愿集成的模组补声明，并按已装载内容动态调整声明集。
- SPI 代码扩展：`GitParcelExtension`（ServiceLoader 发现，启动时注册并冻结），是行为类扩展的唯一入口。

理由：把 MC 新版本的原版字段兼容做成"补一条 JSON"，让 MC 升级后的适配不再要求发版；让模组作者用最低成本集成；社区可以为不愿集成的模组维护声明包；复杂逻辑（需要世界访问、事务语义、跨记录聚合）只能用代码表达，走 SPI。

约束：

- 数据源只能注册声明类条目；行为类扩展只能经 SPI 注册。声明文件不引入任何脚本或代码执行。理由：这是安全边界——声明包可能来自不可信的社区，允许数据触发任意逻辑等于开放代码执行；而所有已知声明需求都能用"路径 + 编码 + 策略"表达。
- 每条声明拥有稳定的完全限定 ID（`namespace:path`）；同一 ID 重复注册显式失败；ID 一经快照自述引用不可变更（SEMANTICS.md 规则 7.4）。
- 声明条目可声明适用的数据版本范围。原版声明只需覆盖当前 schema，因为 vanilla DataFixer 已先把旧快照升级到当前 schema；模组字段声明按模组版本的原始 schema 标注范围，模组 NBT 不受 vanilla DataFixer 保护。
- 运行时重载与进行中的 parcel 操作互斥，操作执行期间看到的注册快照不可变。重载新增的声明不追溯旧快照：恢复时声明解释以快照自述清单为准（SEMANTICS.md 规则 7.3）。理由：在捕获后补声明的场景下，旧快照里未声明字段保存的是世界绝对坐标，若按当前注册表解释会被当作 parcel 相对坐标错误换算，指向错误位置且不报错。

## 扩展指南

扩展经 `GitParcelExtension` SPI 注册（`java.util.ServiceLoader` 发现）。实现并注册：

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

通过 `@AutoService` 或手写 `META-INF/services/io.github.leawind.gitparcel.common.api.extension.GitParcelExtension` 资源文件完成发现。所有注册在启动时一次性完成并冻结；运行期不可再注册。该 API 处于 Beta 阶段，暂不承诺二进制兼容。

按要处理的数据选择通道：

| 你的数据                                                                 | 通道                                    |
| ------------------------------------------------------------------------ | --------------------------------------- |
| 一种全新的、可独立保存的内容（自有序列化格式）                           | 内容类型 `ParcelContentType`            |
| 实体/方块实体 NBT 中的世界坐标（如蜂巢 `flower_pos`）                    | 声明式坐标字段                          |
| 实体 NBT 中指向 parcel 内其他实体的 UUID（如拴绳）                       | 声明式实体引用字段                      |
| 变更无语义的易变字段、随世界时间派生的字段（受击闪烁、时间戳、冷却截止） | 瞬态与派生声明（规划中）                |
| 记录 NBT 的任意变换、私有侧数据                                          | 记录处理器 `ParcelRecordProcessor`      |
| 引用世界外部存储的数据（如地图画、模组侧库存）                           | 附件（处理器 + `ParcelAttachmentType`） |
| 不被任何记录引用的区域性数据（模组 per-region SavedData）                | 区域贡献者 `ParcelCaptureContributor`   |

### 声明式条目

只需声明"哪个 NBT 路径是什么"，内置处理器（`gitparcel:declared_fields`）在捕获与恢复时统一执行。路径语法为点分段：复合键、`[]` 列表通配、`[n]` 下标，如 `Items[].tag.waypoint`。

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

语义约定（完整规约见 SEMANTICS.md）：

- 坐标字段是 parcel 系引用，随 parcel 平移/镜像/旋转。目标语义是内指（指向 parcel 内）字段随 parcel 变换、外指字段保持恒等；当前实现对外指字段也做换算，范围判定见 SEMANTICS.md 第 9 节缺口登记。真正"世界外部"的语义应改用附件。
- 实体引用只在指向同一批次恢复的实体时重写；指向玩家或 parcel 外实体的引用保持原值。
- 声明条目同时作用于实体的 `Passengers` 子树（按嵌套实体自身的 `id` 过滤类型）。
- 瞬态与派生声明（规划中）：判据是"变更是否携带语义"，不是"能否重算"——栅栏连接形状这类可重算但有语义的字段不适用（SEMANTICS.md 规则 2.2）。

内置声明：蜂巢/蜂箱的 `flower_pos`、拴绳 `leash`（UUID 与坐标两种变体）。

### 声明文件与声明包

声明式条目不需要绑定 Java 代码，也可以 JSON 文件装载。三个来源：你的模组 jar 内约定路径（随模组分发，零代码集成）、`config/gitparcel/declarations/`（玩家或社区维护，支持运行时重载）、以及本模组内置的原版声明（规划中，内置声明将从代码迁移为数据文件）。

文件格式为带 schema version 的清单加声明数组（规划中，随首个实现版本定稿）：

```json
{
  "schema": 1,
  "declarations": [
    {
      "id": "examplemod:cooker_time",
      "kind": "time_field",
      "target": "block_entity",
      "type": "examplemod:cooker",
      "path": "next_process_time",
      "strategy": "relativize",
      "data_versions": "[3400,)"
    }
  ]
}
```

约束见"声明装载"一节。

### 附件：世界外部数据随快照旅行

以内置的地图支持为例（`MapItemProcessor` + `MapDataAttachmentType`）：

1. 捕获：处理器在 `captureEntity`/`captureBlockEntity` 中发现自己的物品/数据，调用 `context.requireCollector().collect(sourceIdentity, type, schemaVersion, required, payload)` 得到 `LocalAttachmentId`；把引用编码进 NBT（地图用的是物品 `minecraft:custom_data` 组件），并保留原版字段作为降级路径。
2. 持久化：附件记录由内置 `attachments` 内容类型写进 `data/attachments/`，按 `(type, sourceIdentity)` 去重——同一张地图被多个物品引用只存一份。
3. 恢复：附件先于方块/实体交付，`ParcelAttachmentType.restore` 把数据物化到目标世界（地图分配新的 `MapId` 并写入地图存储）并 `context.attachments().resolve(id, 值)`；随后处理器在 `restoreEntity`/`restoreBlockEntity` 里 `findResolved` 取回新值并改写 NBT。

恢复要求附件类型已注册且 `schemaVersion` 精确匹配，`required` 附件缺失即显式失败——升级 schema 时保留旧版本类型的注册，或接受旧快照不可读。

### 区域贡献者

数据不被任何实体/方块实体记录引用时（模组按区域存储的 SavedData 等），实现 `ParcelCaptureContributor`：

- `capture(context)`：在服务器线程运行，拿到 level、parcel 空间、世界包围盒与附件收集器；收集的附件照常持久化（同样需要已注册的附件类型）。
- `restore(context)`：在全部内容（包括延迟提交的实体批次）应用后运行；`context.attachments()` 是本次恢复的全部附件记录，按自己的类型过滤。

### 扩展已知限制

- 物品内嵌坐标（如探险家指南针的 `LodestonePos`）目前不在声明字段覆盖范围内——处理它的物品组件需要专门的处理器。
- 附件 schema 版本要求精确匹配，没有读路径迁移钩子；内容类型同理：旧版本保持注册即可读旧快照。
- 地图数据恢复到目标世界的 overworld 地图存储；跨维度语义以原数据为准，不做重定位。

## 大规模 Parcel 与操作进度

Parcel 可能小至 `1×1×1`，也可能达到 `512×384×512`，即超过一亿个方块位置。保存、恢复、导入和发布在大规模情况下必然是长时间操作，API 不能以"很快返回"为前提。

### 有界资源使用

- 世界内容、内容记录和 Git blob 必须按 section、实体或文件流式处理，不能把完整 parcel 同时保存在堆内存中。
- 内容类型的读取和写入应维持有文档说明的内存上限；配置中的 section size 也要考虑峰值内存，而不只考虑压缩率。
- NIO 工作区只包含当前一次操作的数据，成本与当前 parcel 大小相关，不随 Git 历史长度增长。
- Git 对象写入、tree 遍历和导出使用流或 channel，不使用 `readAllBytes` 处理潜在的大文件。
- 世界访问必须遵守 Minecraft 线程规则；可以安全移出主线程的内容编码、文件 I/O 和 Git 对象操作应在有界后台执行器中运行。

如果世界读取或放置需要跨多个 tick 分批进行，进度机制必须能够持续报告。这样的捕获表示一段时间内观察到的内容，不能谎称是 Minecraft 世界的原子瞬时快照；若未来实现区域冻结或一致性捕获，应作为单独能力明确声明。

### 进度协议

所有可能长时间运行的用例通过统一 `OperationManager` 注册，而不只登记远程 Git 操作。公开状态至少包含：

```text
operationId
kind
owner
parcel/repository target
state: queued | running | succeeded | failed | canceled
phase
completed
total?       # 未知时为空
unit?        # blocks、sections、entities、files、bytes 等
startedAt
updatedAt
result/error summary
```

保存和加载的顶层阶段由用例层报告，例如世界捕获、内容编码、Git 对象写入、Git tree 读取、内容解码和世界放置。不同单位不能直接相加成虚假的总百分比；只有用例明确知道各阶段权重时才提供整体百分比，否则 UI 展示当前阶段和不确定进度。

`ParcelContentType.SaveContext` 和 `ParcelContentType.LoadContext` 始终提供非空 `ProgressReporter`，不需要跟踪时由调用者传入无操作实现。内容类型可以报告子阶段、已处理数量和可选总量，也可以完全忽略 reporter；未报告时操作仍正常执行，外层阶段显示为不确定进度。`ParcelDataSource`、`ParcelDataSink` 和 Git 工作区桥接器也可以使用同一 reporter 建立子任务。

进度回调必须满足：

- 线程安全、非阻塞，不能直接发送网络消息或访问 Minecraft 客户端对象。
- 同一阶段的 `completed` 单调不减，已知 `total` 时不得超过总量。
- reporter 自身异常不能破坏保存或加载结果。
- 高频更新由 `OperationManager` 合并和限速，再向有权限的客户端推送；格式不负责网络节流。
- 操作结束后必须产生唯一终态，失败结果保留最后阶段和可诊断错误。

进度跟踪本身不等于取消。将来增加取消时，每个阶段还要明确是否可安全取消；已经开始修改世界或更新关键 ref 的阶段不能仅靠中断线程来取消。

## Git 能力与内容交换

### 共享的 Git 实现

内部仓库与共享仓库必须复用同一个 `GitRepositoryCore`，其中集中实现：

- 仓库打开、初始化、规范化路径和生命周期。
- commit/tree/blob 的创建、读取、遍历和安全导出。
- ref 的 compare-and-set 更新与错误分类。
- 历史分页、对象存在性和类型检查。
- 仓库级锁、凭据注入、远程地址校验和结构化结果。

上层通过 `RepositoryPolicy` 或等价能力集合限制操作，而不是复制实现。能力可以包括：

```text
READ_HISTORY
READ_TREE
CREATE_COMMIT
UPDATE_MANAGED_REFS
WORKTREE
BRANCHES
TAGS
REMOTES
MULTI_PARENT_COMMITS
HISTORY_REWRITE
```

内部策略只开放前四项，并对 commit 父节点和 ref namespace 施加额外约束。共享策略是它的超集，可以按服务器配置继续开放其余能力。低层 `GitRepositoryCore` 不能作为命令或网络 API 直接暴露，否则调用者可以绕过策略。

### 内部与共享仓库的职责

内部仓库和共享仓库都是真正的 Git 仓库，但使用规则不同：

- 内部仓库一一对应 parcel，是 bare repository，由模组生成单父 commit 和私有 refs，只提供基础快照功能。
- 共享仓库可以包含多个 parcel，通常有工作树、branch、tag 和 remote，用于发布、协作和常用 Git 操作。

共享仓库由 JGit 管理并提供：

- clone、status、fetch、fast-forward pull 和 push。
- 提交、历史、分支和标签。
- 在高级入口中展示真实 Git DAG，包括多父提交。
- 从指定 commit 和仓库相对路径导入快照。
- 将指定内部快照发布到工作树并创建提交。

合并、rebase、reset、强制推送和历史重写属于更高风险能力。可以后续加入，但必须与基础快照操作分开授权和展示。

### 发布与导入

发布总是发布一个已存在的内部 commit，而不是未保存的实时世界。系统不检测世界是否变化；UI 只需明确告诉玩家发布的是所选 commit，并提供一个独立的"先保存再发布"组合操作。发布把内部 commit tree 放到共享仓库的指定相对路径并创建共享仓库 commit。

导入先把指定 Git tree 中的 parcel 子树安全导出到 NIO 工作区，拒绝符号链接、特殊文件、绝对路径和路径逃逸。完成格式、几何和依赖校验后：

- 新建 parcel 时，导入内容成为内部仓库的根快照。
- 导入到已有 parcel 时，内容成为当前基准的单父子快照，并标记外部来源。

内部快照本身就是内部仓库的 Git commit，但它与共享仓库 commit 不是同一个对象。一个内部 commit 可以发布到多个共享 commit，一个共享 commit 也可以同时修改多个 parcel。需要追踪来源时，可把来源仓库、commit 和路径写入新 commit 的不可变 message trailer；这些信息不能决定内部父子关系。

pull 和 fetch 只更新外部仓库。把更新后的内容带入世界必须经过显式导入、预览和恢复流程。删除外部分支或仓库不会删除内部快照历史。

现有"把 parcel 绑定到共享工作树，之后直接在那里保存"的模型应删除。理由：它让普通保存动作受到工作树脏状态、分支、远程变化和多 parcel 提交的影响，也无法稳定提供独立的单父快照树。

### 可分发仓库规范

可供模组发现和导入的 Git 仓库必须遵循与托管平台无关的布局规范：

```text
<repository>/
├── gitparcel.json
└── <one-or-more-parcel-paths>/
    ├── parcel.json
    ├── config.json
    └── data/
```

根目录 `gitparcel.json` 是带 schema version 的标志性元数据文件，并显式列出仓库包含的所有 parcel 路径。仓库中恰好存在 `parcel.json` 的目录不会因此被自动收录。清单中的路径必须规范化、唯一、不可互相包含，并与实际 tree 一致；修改内容和清单必须进入同一个 Git commit。理由：显式清单让导入端可以校验清单与内容一致，避免"目录恰好长得很像"的误收录与路径注入。

清单描述的是仓库内作品及其相对路径，不保存导入后由某个 Minecraft 世界分配的 parcel UUID。一个仓库可以包含任意多个 parcel，一个 parcel 也可以位于任意安全的仓库相对目录。

远程来源模型只依赖标准 Git 仓库 URI、可选 ref/commit 和 parcel 路径，不假定 GitHub 或任何特定托管平台。HTTPS 之外的协议是否允许由服务器安全策略决定。仓库管理器使用逻辑名称引用本地仓库，并允许管理员或玩家在授权范围内选择本地克隆目录；持久化数据不能写死某个平台的 URL 结构。

## 服务端权威与并发

服务端负责：

- parcel 注册、空间关系和权限。
- 世界内容捕获与放置。
- 当前基准、快照树、Git 对象和私有 refs。
- 格式能力和数据迁移。
- 外部仓库目录、路径安全、凭据和 Git 操作。
- 所有结构化结果和客户端同步。

每个 parcel 使用独立用例锁，保存、恢复、删除、导入、GC 等写操作串行执行。不同 parcel 的仓库操作、世界捕获和纯计算可以并行，但不能并发写同一 parcel，也不能在后台线程直接访问不允许异步访问的 Minecraft 状态。

外部 Git 仓库使用规范化仓库标识的仓库级锁。fetch、pull、push、发布和高级 Git 写操作不能并发破坏同一工作树或索引。网络任务由有界执行器运行，状态包括排队、运行、成功、失败和取消；任何需要修改世界的后续步骤必须返回服务端用例层。

`OperationManager` 统一登记内部 parcel 操作和共享仓库操作。锁决定是否允许并发，manager 负责排队、进度、近期结果和服务器关闭语义；两者不能互相替代。声明重载也与操作互斥（见"声明装载"）。

## 权限设计

世界权限区分列出/创建/删除 parcel、查看共享仓库、发布与导入，以及管理远程 Git 操作。parcel 权限独立区分：

| 权限           | 默认等级 | 用途                            |
| -------------- | -------: | ------------------------------- |
| VIEW           |        1 | 查看 parcel 和快照树            |
| SAVE           |        3 | 保存快照                        |
| RESTORE        |        3 | 恢复快照                        |
| MANAGE_HISTORY |        4 | 后续的删除、squash 等破坏性能力 |
| CONFIG         |        4 | 修改 parcel 的运行时配置        |

`save-first` 恢复同时要求 SAVE 和 RESTORE。发布要求 VIEW 及世界的发布/导入权限。

至少区分以上能力，理由：写入世界（恢复）、写入历史（保存）、重写历史（删除、squash）与修改管理配置的风险完全不同，不能因为拥有普通保存权限就自动获得全部授权。恢复、删除、squash、强制同步等操作不能因为用户拥有普通保存权限而自动获得授权。涉及多个 parcel 的批量操作先检查全部目标和前置条件，再开始修改。

## 网络与 UI 边界

命令和 Modern UI 调用相同的结构化服务端用例，不互相调用。理由：命令文本不是稳定 API，UI 需要结构化结果（错误类别、进度、树增量）而不是解析文本；两入口共用用例层还能保证权限与互斥语义只有一份实现。客户端发送意图和不透明 ID，服务端重新解析目标、检查权限并返回结构化结果。

客户端可以按 operation ID 查询或订阅进度。服务端只向操作发起者和拥有相应查看权限的玩家发送摘要，并对更新频率限流；断开连接时客户端必须清空旧操作镜像。

普通界面首先提供：

- parcel 列表和当前状态。
- 一个主要的"保存快照"动作。
- 快照树、当前基准和分叉可视化。
- 快照名称、说明和来源。
- "直接恢复"与"先保存再恢复"的明确选择和确认。
- 操作阶段、确定或不确定进度、成功、部分失败和恢复建议。

外部仓库和高级 Git 使用独立页面或高级模式。基础快照树不能用截断第一父链的方式展示 Git merge，因为那会隐藏真实历史关系。

树查询返回节点、父 ID、是否为当前基准、摘要和稳定游标。服务端可以分页返回祖先、子树或可见窗口；协议不能只返回已经排版好的坐标，具体布局由客户端决定。

## 路径与内容安全

所有来自格式、Git tree、命令、共享仓库和网络的路径都视为不可信。归档路径必须使用规范化相对表示，并拒绝：

- 空路径、绝对路径和 `..`。
- NUL、平台歧义名称和无法往返编码的路径。
- `.git` 等保留路径。
- 重复路径、文件与其子路径同时存在等树冲突。
- 符号链接、硬链接和其他特殊文件。
- 超出配置限制的文件数量、单文件大小、总展开大小或目录深度。

理由：导入与发布的来源包括社区仓库与远程服务器，路径逃逸与特殊文件是这类数据最直接的攻击面；在物化前一次性校验完整清单，比边校验边写文件更容易保证不留下半写状态。

从 Git tree 物化前先验证完整清单，再创建目标文件。写入和读取对象时校验 Git object ID，加载到世界前还要验证格式、数据版本、尺寸、锚点、依赖和实体策略。

远程凭据不得进入 Git URL、提交、operation commit、网络 DTO 或日志。受管远程默认只接受不含凭据的 HTTPS URL；更宽松的协议必须由管理员显式配置。

## 一致性与失败语义

系统优先显式失败，不静默猜测或覆盖：

- 任一内容类型保存失败时不创建快照。
- Git 对象或 ref 更新失败不覆盖旧的当前基准。
- 玩家明确保存时允许创建 tree 未变化的新 commit，不在保存前额外扫描世界。
- 恢复校验失败时不开始修改世界。
- 世界恢复中断时保留 operation ref 和已有的恢复前 commit，不报告成功。
- Git 仓库无法通过完整性检查时不初始化空仓库替代。
- 外部仓库脏、分叉或缺少上游时，pull/push 明确失败。
- pull 成功也不自动应用内容到世界。
- 删除逻辑 parcel 不隐式删除其历史或外部仓库内容。

理由：快照工具的信任基础是"恢复后的世界就是快照记录的世界"；每一处静默猜测都会侵蚀这条承诺，失败并留下可诊断状态总是优于带着错误继续。

所有结果应有稳定的错误类别和面向玩家的说明，日志中再记录技术异常。命令输出文本不是服务端 API。

## 当前实现评估

当前架构中值得保留的方向包括：

- 服务端权威和客户端只读镜像。
- `ParcelDataSource` / `ParcelDataSink` 与内容类型的编解码分离。
- 内容操作上下文使用 `Path`，内置内容类型主要使用 `Files` API。
- `GitRepo` 已经集中了一部分内部和共享仓库可复用的 JGit 操作。
- 恢复 Git 历史时直接导出子树而不 checkout 工作树。
- 共享仓库锁、异步远程任务、路径校验和结构化历史查询。
- Fabric/NeoForge 平台适配与公共逻辑分离。

与目标设计冲突、应允许破坏性替换的部分包括：

- `save` 只写工作树、`commit` 才形成版本的两步用户模型。
- 每个内部 parcel 使用一个带工作树和 index 的非 bare 仓库，而不是直接构造 Git 对象的 bare repository。
- `ParcelLocation` 把直接路径或共享仓库位置写进 parcel 领域数据。
- bind/unbind 让外部工作树成为后续保存的权威目标。
- 恢复固定在默认临时文件系统创建工作区。
- 在非默认文件系统路径旁使用默认 `Path.of(...)`，存在 provider mismatch 风险。
- JGit `FileRepository` 依赖 `Path#toFile()`，不能代表任意 NIO 文件系统能力。
- 长时间保存和加载没有贯穿内容编解码与世界读写阶段的统一进度模型。
- 恢复前若要判断未保存变化只能额外完整捕获，当前架构不应继续引入这类 dirty 查询。

因此，当前代码适合作为内容存储、世界读写、Git 交换和安全规则的原型，但不适合继续把 Git 工作树作为内部快照领域的中心。下一步应先替换领域用例和存储边界，再继续扩充命令或最终 UI。

## 实施顺序

建议按以下顺序开发：

- 定义不向普通 UI 暴露 Git 细节的 `SnapshotId`、`SnapshotNode`、树查询、保存/恢复请求和结构化结果。
- 拆分 `Parcel` 当前定义、快照清单和外部发布关系，删除 `ParcelLocation` 的权威存储职责。
- 明确 `ParcelContentType` 的 NIO 契约，修复跨 provider 路径构造，并为内置内容类型增加 Jimfs/ZipFS 一致性测试。
- 从现有 `GitRepo` 提取 `GitRepositoryCore` 和服务端能力策略，由内部与共享仓库共同使用。
- 实现内部 bare repository 布局、私有 refs、`ObjectInserter` tree/commit 写入和启动完整性检查。
- 将 `GitOperationManager` 泛化为所有长时间用例共用的 `OperationManager`，并把可选 `ProgressReporter` 接入内容操作上下文。
- 实现原子的 `SaveSnapshot`，替换普通 `save`/`commit` 两步命令。
- 实现树查询、当前基准、显式"先保存再恢复"、未完成恢复日志和分叉测试。
- 更新网络 DTO 与基础命令，再以同一用例实现 Modern UI。
- 为共享仓库加入版本化根清单规范，并实现按 commit 发布与导入。
- 删除旧内部工作树/index、bind/unbind 和直接路径兼容代码；开发期数据不迁移。
- 在核心链路稳定后再加入 compare、删除、squash 和高风险 Git 操作。

理由：顺序遵循"先替换被证明错误的存储中心，再扩展表层能力"——领域用例与存储边界不稳时，命令与 UI 上的任何投入都会在重构中重做。

## 测试策略

- 领域测试：快照与 commit 一一对应、单根、单父、恢复后分叉、相同 tree 重复保存和稳定分页。
- 格式测试：默认文件系统、Jimfs、ZipFS 的读写一致性、确定性输出、进度可选性和路径逃逸。
- 内部 Git 测试：bare repository、对象直写、单父约束、私有 ref 可达性、CAS 失败、GC、fsck 和备份恢复。
- 故障注入：格式半途失败、磁盘写满、对象插入失败、ref 更新失败、恢复中断和服务器重启。
- 进度测试：未知总量、分阶段单调性、高频合并、格式不报告进度以及失败终态。
- 性能测试：`1×1×1` 到 `512×384×512` 的峰值内存、捕获耗时、对象写入、pack 后仓库体积、去重率和树查询延迟。
- 共享 Git 测试：公共实现复用、能力子集不可绕过、根清单、发布/导入、快进约束、分支/标签和真实 merge DAG。
- Minecraft GameTest：首次保存、相同内容重复保存、恢复旧节点、先保存再恢复、从旧节点保存成分叉和重试恢复；以及 SEMANTICS.md 第 8 节的 P1（往返不变性）、P2（迁移正确性）与 P5（替换幂等）。
- 多加载器构建：所有目标 Fabric/NeoForge 版本编译、测试和打包，并验证目标 JGit 版本的 bare repository 行为。

涉及数据丢失、路径逃逸、权限绕过、跨 parcel 引用或并发写入的修复必须附带回归测试。仅通过编译不能证明存储设计安全。
