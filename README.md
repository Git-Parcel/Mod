<div align="center">
<img src="src/main/resources/logo.png" alt="Git Parcel" style="image-rendering:pixelated;height:6em;">

# Git Parcel

用 git 管理 Minecraft 世界中的方块和实体

</div>

## 简介

Git Parcel 是一个服务端权威的 Minecraft Mod，用不可变快照管理游戏世界中的 Parcel——即世界中的轴对齐长方体区域。每个 Parcel 拥有独立的树状历史；底层使用 JGit 和 bare 仓库实现，但普通玩家无需理解 Git 工作树或暂存区。

> [!NOTE]
>
> 本模组正在开发中，尚无稳定的公共API。

## Web 管理控制台

服务端可按需启动浏览器管理界面：游戏内执行 `/parcel web start`（默认端口 5639，仅本机可见），用输出的链接在浏览器中管理各维度的 Parcel——创建、配置、保存与恢复快照、发布与导入等。设计细节见 `docs/DESIGN.md`，前端开发见 `web/README.md`。
