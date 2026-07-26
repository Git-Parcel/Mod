# Git Parcel 命令参考

## 文档范围

本文记录当前正式命令的语法和行为，以实际命令实现为准。开发环境中的 `parceldebug` 调试命令不在本文范围内。

Git Parcel 的命令分为两组：

- `/parcels`：创建或导入 parcel、查看格式、管理共享仓库和后台 Git 任务。
- `/parcel <selector>`：查看或操作当前维度中已有的 parcel。

命令由服务端执行。权限、parcel 状态、文件和 Git 仓库均以服务端为准。

## 语法约定

- `<参数>` 表示必填参数。
- `[参数]` 表示可选参数。
- 包含空格的名称或路径需要使用双引号，例如 `"Main House"`。
- `<from>`、`<to>` 和 `<at>` 使用 Minecraft 方块坐标语法，支持绝对坐标和 `~` 相对坐标。
- `<mirror>` 使用 `none`、`left_right` 或 `front_back`。
- `<rotation>` 使用 `none`、`clockwise_90`、`clockwise_180` 或 `counterclockwise_90`。
- 如果需要指定 rotation 而不需要镜像，仍需先把 mirror 写为 `none`。
- `<revision>` 可以是 Git 提交哈希、分支、标签或 `HEAD` 等不含空格的 Git revision。
- `<message>` 和 `<description>` 会读取该行剩余的全部文本，不需要为了空格添加引号。

## parcel 选择器

`/parcel` 的第一个参数用于选择当前维度中的一个或多个 parcel。

### 按名称或 UUID 选择

```text
/parcel "Main House"
/parcel dd12be42-52a9-4a91-a8a1-11c01849e498
```

名称进行完整、区分大小写的匹配，不支持通配符。parcel 名称可以重复；如果要求单个 parcel 的命令按名称匹配到多个结果，命令会失败。命令补全只建议当前唯一的名称。

### 使用选择器

| 选择器 | 含义 |
| --- | --- |
| `#a` | 当前维度中的所有 parcel |
| `#p` | 距命令执行位置最近的一个 parcel |
| `#s` | 执行者视线命中的第一个 parcel；执行者位于 parcel 内部时也会命中 |

选择器支持以下选项：

| 选项 | 含义 |
| --- | --- |
| `name=<名称>` | 只匹配名称完全相同的 parcel，并把结果限制为最多一个 |
| `name=!<名称>` | 排除该名称，并把结果限制为最多一个 |
| `limit=<数量>` | 限制结果数量，必须至少为 `1` |

示例：

```text
/parcel #a
/parcel #p
/parcel #s save
/parcel #a[limit=3] save true
/parcel #a[name="Main House"]
/parcel #a[limit=5] delete
```

使用 `#a`、`#p` 或 `#s` 需要“列出 parcel”世界权限。直接使用名称或 UUID 时，仍会检查具体子命令所需的权限。

## 权限

权限等级与 Minecraft 原版权限等级对应：

| 名称 | 等级 |
| --- | ---: |
| 所有人 | 0 |
| MODERATORS | 1 |
| GAMEMASTERS | 2 |
| ADMINS | 3 |
| OWNERS | 4 |

世界权限的默认要求如下：

| 权限 | 默认等级 | 使用位置 |
| --- | ---: | --- |
| 列出格式 | 1 | `/parcels formats` |
| 列出 parcel | 1 | 查看 parcel 信息和使用 parcel 选择器 |
| 创建 parcel | 4 | `/parcels create`、`/parcels import` |
| 删除 parcel | 4 | `/parcel ... delete` |
| 配置 parcel | 4 | `/parcel ... config set ...` |
| 查看共享仓库 | 1 | `/parcels repositories [list]` |
| 管理共享仓库 | 4 | 创建、克隆和同步仓库，查看任务，以及 publish、bind |

每个 parcel 还具有独立权限，默认要求如下：

| 权限 | 默认等级 | 使用位置 |
| --- | ---: | --- |
| SAVE | 3 | `save` |
| LOAD | 3 | `history`、`restore` |
| COMMIT | 3 | `commit`、`publish` |
| CONFIG | 4 | `config`、`bind`、`publish`、`unbind` |

创建或导入 parcel 时，会复制当前世界的默认 parcel 权限。之后该 parcel 的权限独立存在。

## `/parcel <selector>`

```text
/parcel <selector>
```

显示匹配 parcel 的 UUID、名称、格式、尺寸、中心、边界、变换和存储位置。

需要“列出 parcel”世界权限。

示例：

```text
/parcel #p
/parcel "Main House"
```

## `/parcels create <from> <to> <name> [mirror] [rotation]`

```text
/parcels create <from> <to> <name> [mirror] [rotation]
```

使用两个角点创建 parcel。边界包含 `from` 和 `to` 指向的方块。创建时会校验最大体积以及与已有 parcel 的空间关系，并采用服务端当前的默认写入格式和默认 parcel 权限。

创建命令只注册 parcel，不会自动保存世界内容或创建 Git 提交。通常接着执行 `save` 和 `commit`。

需要“创建 parcel”世界权限。

示例：

```text
/parcels create ~ ~ ~ ~15 ~7 ~15 "Main House"
/parcels create 0 64 0 15 79 15 Tower front_back clockwise_90
```

## `/parcels formats`

```text
/parcels formats
```

列出服务端当前注册的 parcel 格式写入器和读取器。格式通常显示为 `<id>:<version>`。

需要“列出格式”世界权限。

## `/parcel <selector> save [ignore_entities]`

```text
/parcel <selector> save [ignore_entities]
```

把 parcel 当前范围内的方块、方块实体以及允许保存的实体写入工作树快照。`ignore_entities` 默认为 `false`；设为 `true` 时不保存普通实体。如果 parcel 元数据中的 `meta.excludeEntities` 为 `true`，即使命令参数为 `false` 也不会保存实体。

`save` 不会创建 Git 提交。保存和提交是两个独立步骤。

对所有目标 parcel 都需要 SAVE 权限。命令会先检查全部目标的权限，再开始保存；保存过程中遇到错误后会停止，之前已经成功保存的其他目标不会自动回滚。

示例：

```text
/parcel #p save
/parcel #a[limit=3] save true
```

## `/parcel <selector> commit [message]`

```text
/parcel <selector> commit [message]
```

提交 parcel 工作树中的当前快照。parcel 必须至少成功保存过一次。省略 message 时，默认说明为 `Save parcel <UUID>`。

没有变化时不会创建空提交。共享仓库中的提交只包含目标 parcel 路径和仓库的 `meta.json`，不会顺带提交其他 parcel 的更改。

对所有目标 parcel 都需要 COMMIT 权限。

示例：

```text
/parcel #p commit
/parcel "Main House" commit Add second floor
```

## `/parcel <selector> history [limit]`

```text
/parcel <selector> history [limit]
```

按从新到旧的顺序列出影响目标 parcel 路径的 Git 提交。`limit` 默认为 `10`，允许范围为 `1` 至 `50`。

对所有目标 parcel 都需要 LOAD 权限。

示例：

```text
/parcel #p history
/parcel "Main House" history 25
```

## `/parcel <selector> restore <revision> [ignore_entities]`

```text
/parcel <selector> restore <revision> [ignore_entities]
```

从指定 Git revision 读取 parcel 快照并应用到当前世界位置。该操作不会 checkout 仓库、移动 HEAD、修改工作树或自动创建新提交。

历史快照的尺寸和锚点必须与当前注册的 parcel 兼容。`ignore_entities` 默认为 `false`；命令参数或 parcel 元数据任一方要求排除实体时，都不会恢复普通实体。

恢复后的世界内容如需成为新的当前版本，需要再次执行 `save` 和 `commit`。

对所有目标 parcel 都需要 LOAD 权限。

示例：

```text
/parcel #p restore HEAD~1
/parcel "Main House" restore a1b2c3d4 true
```

## `/parcel <selector> config set <key> <value>`

配置命令的通用形式为：

```text
/parcel <selector> config set <key> <value>
```

支持的 key 如下：

| key | value | 作用 |
| --- | --- | --- |
| `meta.format` | `<save_format>` | 设置后续 `save` 使用的格式写入器 |
| `meta.name` | `<name>` | 设置 parcel 名称；包含空格时需要引号 |
| `meta.author` | `<author>` | 设置作者；当前参数不能包含空格 |
| `meta.description` | `<description>` | 设置描述，读取该行剩余文本 |
| `meta.excludeEntities` | `<bool>` | 设置是否始终排除普通实体 |
| `visual.showWireframe` | `<bool>` | 设置客户端是否显示边框线框 |
| `visual.showAnchor` | `<bool>` | 设置客户端是否显示锚点 |

需要“配置 parcel”世界权限，并且对所有目标 parcel 都需要 CONFIG 权限。

改变 `meta.format` 只修改元数据，不会立即转换磁盘上的旧快照。下一次 `save` 会使用新格式生成完整快照。

`meta.format` 的参数使用格式 id，例如 `parcella_d32`，不填写 `:version`；同一 id 存在多个版本时选择服务端注册的最高版本。

示例：

```text
/parcel #p config set meta.name "Main House"
/parcel #p config set meta.description Main base near spawn
/parcel #p config set meta.excludeEntities true
/parcel #a config set visual.showWireframe false
```

## `/parcel <selector> delete`

```text
/parcel <selector> delete
```

从当前维度的服务端注册表中删除匹配的 parcel，并通知客户端。该命令不会删除世界内部 Git 仓库、共享仓库或已有快照文件。

需要“删除 parcel”世界权限。当前不额外检查 parcel 自身权限。

示例：

```text
/parcel #p delete
/parcel #a[name=Temporary] delete
```

## `/parcel <selector> teleport [players]`

```text
/parcel <selector> teleport [players]
```

把命令执行玩家或指定玩家传送到单个 parcel 边界中心的底部位置。该命令必须恰好匹配一个 parcel；控制台执行时必须显式提供 players。

当前实现没有为 teleport 增加额外的 Git Parcel 权限检查。使用 parcel 选择器时仍需要“列出 parcel”权限，Minecraft 对玩家参数和命令来源的其他限制仍然适用。

示例：

```text
/parcel #p teleport
/parcel "Main House" teleport @a
```

## 共享仓库概念

共享仓库由服务端登记和管理。仓库中的 `meta.json` 明确列出可供 Git Parcel 使用的 parcel 路径；仅在磁盘上存在某个目录并不足以让它被 bind 或 import。

仓库名称只能使用字母、数字、点、下划线和连字符，必须以字母或数字开头，最长 64 个字符。parcel 路径必须留在仓库内，不能是绝对路径，不能包含 `.git`，也不能与仓库根部的 `meta.json` 冲突。

## `/parcels repositories [list]`

```text
/parcels repositories
/parcels repositories list
```

两种写法等价。输出仓库名称、类型、远程地址和最近同步时间。

需要“查看共享仓库”世界权限。

## `/parcels repositories create <repository>`

```text
/parcels repositories create <repository>
```

异步创建一个没有受管远程地址的本地共享 Git 仓库。目标名称和目录必须尚未存在。

需要“管理共享仓库”世界权限。

示例：

```text
/parcels repositories create community-builds
```

## `/parcels repositories clone <repository> <remote_url>`

```text
/parcels repositories clone <repository> <remote_url>
```

异步克隆远程仓库并登记为共享仓库。受管远程地址必须是无内嵌凭据、无查询参数和片段的 HTTPS URL；不接受 HTTP、SSH 或本地文件地址。

需要“管理共享仓库”世界权限。

示例：

```text
/parcels repositories clone community-builds https://example.com/team/builds.git
```

## `/parcels repositories fetch|pull|push <repository>`

```text
/parcels repositories fetch <repository>
/parcels repositories pull <repository>
/parcels repositories push <repository>
```

这些命令在后台 Git 工作线程中执行，只适用于由 `clone` 登记、具有受管远程地址的仓库。

- `fetch`：更新 `origin` 的远程跟踪引用。
- `pull`：要求工作树干净，并且只接受 fast-forward 更新。
- `push`：要求工作树干净、当前分支有效，并将当前分支推送到 `origin` 的同名分支。

发生分叉、冲突、脏工作树或远程配置缺失时，操作会失败，不会自动合并或强制覆盖。

`pull` 只更新仓库文件，不会自动修改实时世界。需要显式使用 `import`；对于已绑定的 parcel，可以在确认内容后使用 `restore HEAD` 把当前提交应用到世界。

需要“管理共享仓库”世界权限。

## `/parcels operations [id]`

```text
/parcels operations
/parcels operations <id>
```

不带 id 时列出最近 10 个任务；带 id 时显示指定任务。任务状态包括：

- `QUEUED`：正在排队。
- `RUNNING`：正在执行。
- `SUCCEEDED`：执行成功。
- `FAILED`：执行失败。
- `CANCELLED`：服务器停止时被取消。

服务端最多保留 100 个近期任务记录。创建、克隆、fetch、pull 和 push 都通过该任务系统运行。

需要“管理共享仓库”世界权限。

## `/parcel <selector> publish <repository> <path> [message]`

```text
/parcel <selector> publish <repository> <path> [message]
```

把一个使用世界内部存储的 parcel 保存到共享仓库的新路径，更新共享仓库 `meta.json`，将两者提交为同一个 Git 提交，并把 parcel 绑定到该共享位置。

该命令必须恰好匹配一个 parcel。目标路径不能已登记或已存在。省略 message 时，默认说明为 `Publish parcel <UUID>`。

发布在提交前采用回滚保护：保存、元数据更新或提交失败时，会删除新快照、恢复原 `meta.json` 并清理暂存项；parcel 只有在提交成功后才改变绑定。

需要“管理共享仓库”世界权限，并需要目标 parcel 的 SAVE、CONFIG 和 COMMIT 权限。

示例：

```text
/parcel #p publish community-builds spawn/main-house Initial publication
```

## `/parcel <selector> bind <repository> <path>`

```text
/parcel <selector> bind <repository> <path>
```

把一个已注册的 parcel 绑定到共享仓库中已登记的快照。该命令必须恰好匹配一个 parcel，并校验共享快照的 `parcel.json`、尺寸和锚点。

bind 只改变后续保存和 Git 操作使用的位置，不会把共享快照自动加载到世界。

需要“管理共享仓库”世界权限，并需要目标 parcel 的 CONFIG 权限。

示例：

```text
/parcel "Main House" bind community-builds spawn/main-house
```

## `/parcel <selector> unbind`

```text
/parcel <selector> unbind
```

让一个 parcel 的后续保存和 Git 操作重新使用世界内部仓库。该命令必须恰好匹配一个 parcel。

unbind 不会复制或删除共享仓库中的旧快照，也不会立即创建新的世界内部快照；解除后应执行 `save`，再按需执行 `commit`。

需要目标 parcel 的 CONFIG 权限。

## `/parcels import <repository> <path> <at> [mirror] [rotation]`

```text
/parcels import <repository> <path> <at> [mirror] [rotation]
```

从共享仓库当前工作树中的已登记路径读取快照，在指定位置创建新的 parcel UUID，并立即把内容加载到世界。新 parcel 保持绑定到共享仓库，并复制当前世界的默认 parcel 权限。

导入前会校验格式、最大体积和空间关系。`at` 是 parcel 变换的放置位置。

需要“创建 parcel”世界权限。当前实现不额外要求“查看共享仓库”或“管理共享仓库”权限。

示例：

```text
/parcels import community-builds spawn/main-house ~ ~ ~
/parcels import community-builds spawn/tower 100 64 100 none clockwise_90
```

## 远程认证

远程 Git 命令从服务端进程环境读取认证信息：

```text
GITPARCEL_GIT_USERNAME
GITPARCEL_GIT_TOKEN
```

未配置 token 时使用匿名 HTTPS。配置 token 但未配置 username 时，username 默认为 `git`。凭据不会写入命令参数、共享仓库目录、Git 配置或任务详情。不要把 token 放进 `remote_url`。

## 常见工作流

### 创建并建立第一个版本

```text
/parcels create ~ ~ ~ ~15 ~7 ~15 "Main House"
/parcel "Main House" save
/parcel "Main House" commit Initial version
```

### 修改后创建新版本

```text
/parcel "Main House" save
/parcel "Main House" commit Add storage room
/parcel "Main House" history 10
```

### 恢复历史内容并形成新版本

```text
/parcel "Main House" history
/parcel "Main House" restore a1b2c3d4
/parcel "Main House" save
/parcel "Main House" commit Restore previous design
```

### 发布并同步共享内容

```text
/parcels repositories clone team https://example.com/team/builds.git
/parcels operations
/parcel "Main House" publish team spawn/main-house Initial publication
/parcels repositories push team
/parcels operations
```

### 拉取并导入共享内容

```text
/parcels repositories pull team
/parcels operations
/parcels import team spawn/main-house ~ ~ ~
```
