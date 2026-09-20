# 用于 gametest 的结构体

文件同时存在于 `structure/` 与 `structures/` 两处：26.x 的原版模板目录为单数 `structure`，1.20.1 为复数 `structures`，两目录内容保持一致。

结构文件由 26.x 生成；1.20.1 读取高 DataVersion 的模板时 DFU 静默透传，palette 中 1.20.1 后新增的方块会解析为空气。往返类测试自洽（捕获什么就恢复什么），不影响正确性断言。

## `normal-48x48x48.nbt`

一个中等大小的结构，取自自然生成的地形

## `swamp_hut-7x8x9.nbt`

沼泽小屋，包含箱子、画、物品展示框、盔甲架、活塞、酿药锅等

## `layers_6-9x30x10.nbt`

垂直划分为6层，每层高5格，仅含方块
