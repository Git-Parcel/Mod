<div align="center">
<img src="src/main/resources/logo.png" alt="Git Parcel" style="image-rendering:pixelated;height:6em;">

# Git Parcel

**使用 Git 管理 Minecraft 世界中的 Parcel**

</div>

## 简介

Git Parcel 是一个 Minecraft Mod，使用 Git（通过 JGit）来管理游戏世界中的 Parcel——即世界中的轴对齐长方体区域。每个 Parcel 都有唯一的 UUID 标识，并包含元数据、变换、权限配置以及可插拔的存储格式。

> [!NOTE]
>
> 本模组正在开发中，尚无稳定的公共API。

## 核心特性

- **Git 集成**：基于 JGit 实现，将 Parcel 数据存储在 Git 仓库中
- **多种存储格式**：支持 Parcella D32/D16、Structure Template 等格式，将来可支持更多格式
- **锚点机制**：使用锚点锚定世界中的特定位置，移动、调整 parcel 边界时仅影响边界附近的数据，未移动的子 parcel 可不受影响
- **变换支持**：加载到世界中时，可以指定旋转、镜像、平移变换
- **权限系统**：细粒度的 Parcel 权限控制
- **多加载器支持**：Fabric、NeoForge、Forge
- **现代 UI**：基于 ModernUI 的图形界面

## 命令

### `/parcels`

- `parcels`
  - `create <from> <to> [name] [mirror] [rotation]` 在当前维度中创建新的 Parcel
  - `formats` 列出服务端支持的所有 ParcelFormat

### `/parcel`

- `parcel`
  - `<parcel>` 查看指定的parcel的基本信息
    - `teleport` 将执行者传送到 parcel
    - `config` 管理 parcel 属性
      - `set` 设置属性值
        - `meta.format <save_format>`
        - `meta.name <word>`
        - `meta.author <word>`
        - `meta.description [string]`
        - `meta.excludeEntities [bool]`
        - `visual.showWireframe <bool>`
        - `visual.showAnchor <bool>`
    - `save [ignore_entities]` 保存指定 parcel 到其工作区
    - `delete` 删除 parcel

### `/parcel_debug`

调试命令，仅在开发环境下可用

- `parcel_debug`
  - `load <to> <path> [mirror] [rotation]` 从磁盘中加载特定位置的 parcel 到世界
  - `save <from> <to> <path> [<format>] [ignore_entities] [mirror] [rotation]`将世界中parcel的当前状态保存到磁盘
  - `clear_data`
    - `world` 清除当前存档中的所有parcel相关数据
    - `level [dimension]` 清除特定维度中所有parcel相关数据
  - `storage` 查看磁盘中各种类型数据存储位置
    - `world`
    - `game`
    - `system`

## 存储格式

| 格式               | ID                     | 说明                         |
| ------------------ | ---------------------- | ---------------------------- |
| MVP                | `mvp:0`                | 仅保存（无加载支持）         |
| Structure Template | `structure_template:0` | 基于原版结构模板，不支持变换 |
| Parcella D32       | `parcella_d32:0`       | 主格式，支持完整功能         |
| Parcella D16       | `parcella_d16:0`       | 16x16x16 网格变体            |

### Parcella D32 格式关键技术

Parcella D32 是本项目的主存储格式，采用多种优化技术在git仓库中实现高效的数据存储：

#### 子 Parcel 网格划分

将 Parcel 按 32×32×32 的网格划分为多个子 Parcel（Subparcel），每个子 Parcel 独立存储。网格对齐到锚点，便于增量加载和 Git 差异对比。

#### Z-Order（Morton）曲线空间索引

使用 Z-Order 曲线将子 Parcel 的 3D 坐标映射为 1D 索引，保持空间局部性。支持有符号坐标（所有 8 个象限），并内置缓存优化热点访问。

```
坐标 (x, y, z) → Z-Order 索引 → 文件路径
```

#### Volumetric RLE 3D 压缩

三维行程编码（Run-Length Encoding）算法，沿 Y→X→Z 轴贪心扩展相同方块的长方体区域，显著压缩大面积相同方块的数据。

```
格式: <minXYZ>~<palette_id>    (使用调色板)
      <minXYZ><maxXYZ>=<state>  (内联方块状态)
```

#### 方块调色板（Block Palette）

双向映射表，将方块状态（BlockState）映射为整数 ID。

#### Radix Tree 文件路径

使用基数树结构将 Z-Order 索引转换为层级目录路径，避免单目录文件过多的问题。

```
索引 0x12345678 → 12/34/56/78.blk
```

#### FLAT 编码（可选）

简单顺序编码，将子 Parcel 内方块按 X→Y→Z 顺序逐行存储，方便处理 git 差异。

#### 实体存储

按实体类型（namespace/path）分目录存储，支持 NBT 格式。保存时记录相对坐标，加载时应用变换恢复世界坐标。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。
