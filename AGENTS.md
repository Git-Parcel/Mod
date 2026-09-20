# Agent 指南

## 最高约束

“最高约束”这一二级标题下的内容由用户（人类开发者）亲自编写，Agent 不得擅动；本文件其他二级标题下的内容 Agent 可以修改。

- 目标：把世界中的长方体区域抽象为 parcel，并使用 Git 保存和管理 parcel 的每一个快照。
- 目标用户包括不了解或不愿意深入了解 Git 的普通玩家
- 目前仍处于 Beta 阶段，允许进行大规模重构，并应彻底删除废弃的类、方法、字段、测试和文档，不为旧接口保留兼容层

### 文档

Agent 指南文件分工：

- `AGENTS.md`（本文件，入库）：项目约束、编码指南与踩坑记录
- `AGENTS-LOCAL.md`（本地，不纳入版本管理）：本机开发环境相关的经验，包含现成的 Minecraft 源码存放位置
- `BACKLOG.md`（入库）：已识别、应当完成但由人类决定时机或亲自执行的事项。Agent 不得自行启动其中事项，除非人类在对话中明确指示；发现新的缺口时向人类提出，经确认后记入

文档：

- 本仓库中的文档一律使用简体中文，使用符合中国大陆规范的标点符号
- 本模组的设计细节（含命令参考与扩展指南）位于 `docs/DESIGN.md`，语境语义的形式化规约位于 `docs/SEMANTICS.md`；docs/ 下其他文件通常不需要访问，仅在用户明确要求时作为参考。
- 仓库中的文档代表目标，可超前于代码实现，不可滞后
- Agent 工作时应先更新文档，再编写实现
- 如果执行任务时发现现有文档存在错误、矛盾、过时、缺失等问题而应当修改，但用户没预料到，应向用户请示，不能编写超前于文档的实现或擅自修改文档
- 执行任务时应适时提交更改，而非完成全部任务后一次性提交
- 若踩到值得记录的坑，可在任务完成后报告

### 提交

- 修改构建脚本或源码后应运行测试，通过后才可提交
- 测试基线必须保持全绿：不得以环境差异或“与本次改动无关”为由长期容忍失败的测试。发现平台相关失败（如 Windows 文件系统语义）时先定位根因——可修复的照常修复；测试前提在该平台确实无法成立的（如 NTFS 上构造不出大小写歧义路径），改用条件跳过（如 `@DisabledOnOs`）并注明原因，同时尽量以不依赖该前提的形式（如直接向被测逻辑喂入数据）保留对同一逻辑的覆盖
- 测试夹具不得隐式依赖宿主平台：跨平台行为必须显式固定（例如 Jimfs 夹具显式传 `Configuration.unix()`，而非跟随平台的无参构造）；验证回归时以“失败集合为空”为基准，不再与既有失败清单对比
- 不保留无意义的提交

### 依赖管理

依赖版本（fabricApi、ModMenu、各加载器等）在各 `versions/*-*/gradle.properties` 中手动管理。这是有意采用的策略，不引入 Renovate、Dependabot 等自动化依赖更新工具。

### Mixin

1. 禁止使用 `@Redirect`：它会排他地占用调用点，容易与其他模组冲突；优先使用可组合的注入器
2. 禁止使用 `@ModifyArgs`（以及注入 `Args` 参数）：Mixin 0.8.5 会在 `org.spongepowered.asm.synthetic.args` 动态生成 `Args$N`，部分启动器（如 ModLauncher）无法加载该包，造成目标类链接时的 `NoClassDefFoundError`。需要修改多个参数时，使用多个 `@ModifyArg` 或合适的 MixinExtras 注入器

## 编码指南

### 代码风格

#### 格式化

- Java
  - 遵循 `google-java-format` 规范
  - 缩进：2 个空格
  - 大括号：K&R 风格（左括号不换行）
  - 导入语句：禁止使用通配符导入（如 `import java.util.*`）
- md, yml, json, toml 等文件用 deno 进行格式化：`deno fmt`

### Stonecutter 指南

当前激活的 Minecraft 版本可以在 `stonecutter.gradle.kts` 文件中找到。

在代码中可以根据 Minecraft 版本、加载器等条件编译代码，示例：

```java
/*? if >=26.1 {*/
@Unique private static final String SETUP_CAMERA_METHOD = "alignWithEntity";
/*? } else {*/
/*@Unique private static final String SETUP_CAMERA_METHOD = "setup";
*//*? } */
```

不符合当前条件的代码使用 `/*  */` 包裹。

尽量避免深层嵌套，如果必须嵌套，符合当前条件的代码中的条件仍然用 `/* */`，被注释的其他版本的代码中的条件用 `/^ ^/`。

例如，如果当前版本是 26.1 或以上：

```java
/*? if >=26.1 {*/
@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
private void beforeCameraUpdate(float partialTicks, CallbackInfo ci) {
  Camera camera = (Camera) (Object) this;
  cameraSetupContext.setup(camera, partialTicks);
}
/*? } else {*/
  /*@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
  private void beforeMoveCamera(
    /^? if >= 1.21.11 {^/
    net.minecraft.world.level.Level blockGetter,
    /^? } else {^/
    /^net.minecraft.world.level.BlockGetter blockGetter,
    ^//^? }^/
    net.minecraft.world.entity.Entity entity,
    CallbackInfo ci) {
    cameraSetupContext.setup((Camera) (Object) this, partialTicks);
  }
*//*? } */
```

如果当前版本是 1.21.11：

```java
/*? if >=26.1 {*/
/*@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
private void beforeCameraUpdate(float partialTicks, CallbackInfo ci) {
  Camera camera = (Camera) (Object) this;
  cameraSetupContext.setup(camera, partialTicks);
}*//*? } else {*/
  @Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
  private void beforeMoveCamera(
    /*? if >= 1.21.11 {*/
    net.minecraft.world.level.Level blockGetter,
    /*? } else {*/
    /*net.minecraft.world.level.BlockGetter blockGetter,
    *//*? }*/
    net.minecraft.world.entity.Entity entity,
    CallbackInfo ci) {
    cameraSetupContext.setup((Camera) (Object) this, partialTicks);
  }
/*? } */
```

#### 风格

- 当需要使用 else、else-if 时，尽量用 `>=` 条件，不要用 `<` 或 `<=`
- 如果一个方法体中使用了 Stonecutter 条件编译，且需要通过注释说明为什么需要条件编译，应将该注释写在方法的 Javadoc 中，而不是方法体中

正确示例：

```java
/*? if >=1.21.11 {*/
return currentVersion().dataVersion().version();
/*? } else {*/
/*return currentVersion().getDataVersion().getVersion();
 *//*? }*/
```

错误示例：

```java
/*? if <1.21.11 {*/
/*return currentVersion().getDataVersion().getVersion();
 *//*? } else {*/
return currentVersion().dataVersion().version();
/*? }*/
```

#### 格式化提示

合并相邻的条件编译块时（即 `*/` 后紧跟 `/*?`），应确保它们紧邻而非被空白分隔：

```
查找：(\s|^)\*/(\s|\n)+/\*\?
替换：*//*?
```

#### 字符串替换

stonecutter 的 `replacements.string` 是双向替换：条件为 true 时按 `replace(from, to)` 正向替换，为 false 时反向替换。因此源代码写任意一侧的名称都会被替换为正确值，旧版本条件分支中出现的旧类名（或看似未导入的类）不是错误，不要"修复"。建议共享源码统一使用新版本名称（如 `Identifier`、`GuiGraphicsExtractor`）：当前最高版本无需替换，旧版本自动反向替换；两个条件编译块的唯一区别是被替换的类型名时，可以合并为一个块。

#### NBT 读取接缝

- 共享代码读取 NBT 一律经 `common/minecraft/logic/portable/NbtReads`：CompoundTag getter 的 Optional/原生形态差异只存在于该工具类内部，语义统一为"键缺席或类型不符时回退（或返回 null）"。直接调用 `tag.getXxx(...)` 仅允许出现在已被版本条件块包住的单版本分支内。
- 实体 NBT 树的 `Passengers` 递归与类型 id 解析经同包的 `EntityTrees`，不要在各处理器中复制遍历逻辑。
- 测试源集断言 Optional 形态经 `common/testutils/TestNbt`（镜像 26.x 的 Optional 签名）；gametest 源集读 NBT 直接用主源集的 `NbtReads`。

## 踩坑记录

实现中踩过并确认的坑，供后续任务避让；条目应写清现象与结论，不罗列排查过程。

- `ParcelTransform`/`ParcelSpace` 的 `BlockPos` 重载在旋转下带 −1 修正（方块网格到方块网格的映射），`Vec3` 重载是纯点映射。parcel 锚点是格点而非方块索引：断言或换算锚点自身必须走 `Vec3` 语义（`transform.translation()`），用 `BlockPos` 重载往返锚点会在带旋转的朝向下偏移一格。
- Stonecutter 条件块的假分支是"注释包裹"语义：激活时 stonecutter 剥离 `/*` 与 `*/` 标记还原代码。多行假分支必须是 `/*` 开头、裸续行（不得加 javadoc 风格的 `*` 前缀）、`*/` 结尾的单块注释，否则剥离标记后会残留 `*`，生成非法 Java 导致"非法的类型开始"编译错误。
- stonecutter 0.9.8 处理**所有**源集（含 `src/test` 与动态创建的 `gametest` 源集）：`replacements.string` 与 `/*? */` 条件块在附加源集同样生效，各节点的生成树位于 `versions/<node>/build/generated/stonecutter/<源集>/`。此前"替换只作用于主源集"的记录已过时。
- 1.20.1 的 `ListTag.getFloat/getDouble(int)` 严格按 tag id 匹配（Double 元素取 float 返回 0），26.x 对任意数值做强制转换：列表元素数值读取必须经 `NbtReads`（其 1.20.1 分支从 `NumericTag` 直接强转），不得直接调用原版 ListTag getter。
- CompoundTag 键迭代：26.x 实现了 Map（`keySet()`/`entrySet()`），1.20.1 只有 `getAllKeys()` 且不是 Map——共享代码迭代键要经接缝方法（如 gametest 的 `GameTestUtils.keySetOf`）。
- `ResourceKey` 取命名空间 id：26.x 为 `.identifier()`，1.20.1 为 `.location()`，共享代码用条件块收敛在单点。
- gametest 源集 1.20.1 适配要点：fabric-gametest-api 1.2.x 只有 `FabricGameTest` 入口接口（没有 26.x 的 fabric `@GameTest` 注解与 `CustomTestMethodInvoker`），配原版注解 `@GameTest(template=, timeoutTicks=)`（26.x fabric 注解属性名为 `structure=`/`maxTicks=`）；结构模板数据包目录 26.x 为单数 `structure/`、1.20.1 为复数 `structures/`（fixture 双份存放）；1.20.1 节点是 Java 17，禁用模式 switch 等 21+ 语法，也禁用对静态类型已确定的表达式写 `instanceof CompoundTag x`（无条件模式，`-source 17` 报错）——两版本分支产出类型不同时，先在各自分支内收敛类型再判断；26.x 把多数实体类拆进子包（`animal.turtle`、`monster.skeleton`、`decoration.painting`、`projectile.arrow`、`npc.villager`），跨版本实体类引用经 `GameEntityTypes` 接缝收敛；原版 NBT 键名随版本成对漂移（1.20.1 PascalCase/平铺三 int/X-Y-Z 复合 vs 26.x snake_case/编码器形态），测试注入与断言必须用目标版本的写侧键名，否则测试与原版序列化脱钩形成假阳性。
- ModStitch 锁定旧版 ModDevGradle，新 MC 版本发布后 NFRT 会因不识别版本号而 recompile 失败（日志先报 `Failed to parse MC version`，最终 `Node action for recompile failed`）。升级 ModStitch 常不足以跟进，需在 `stonecutter.gradle.kts` 的 plugins 块显式声明新版 `net.neoforged.moddev`（buildscript classpath 对同一模块取最高版本）。
- `vcsVersion` 节点直接编译共享源文件而不经 stonecutter 处理：源文件必须始终保持为 vcs 节点（当前 26.3-fabric）的可编译形态。为其他版本准备的代码绝不能以裸代码形式写在"当前版本为假"的 `if` 主分支里，必须写成注释包裹的假分支（else 或 `/*? if 旧条件 {*/*...*//*?}*/`）。
- 条件块包裹方法时，方法上的注解（如 `@VersionSensitive`）必须随方法一起进块；注解留在块外而方法被注释掉的节点上，注解悬空会产生"非法的类型开始"编译错误。
- stonecutter 对嵌套条件块按内外条件组合求值，外层假分支内嵌套块的激活会导致注释结构破坏并生成非法 Java。版本×版本的两维差异要平铺为 `/*? if >=A {*//*?} else if >=B {*//*...*//*?}*/` 链，不做嵌套。
- `replacements.string` 的文本替换无法区分同形调用链（如 DataResult 与 inventory-java Result 都有 `result()`），也会作用于条件注释内的代码；共享源码应统一写两版本皆可编译的形态（如 DataResult 用 `result().orElseThrow()` 而非 26.x 新增的无参 `getOrThrow()`），只对类名/方法名级别的纯文本差异使用替换。
- Forge 1.20.1 适配要点：`NetworkRegistry.newSimpleChannel` 的 client/server 接受参数是 `Predicate<String>` 而非 `Supplier`；事件总线无 `addListener(Class, consumer)` 重载，用 `addListener(方法引用)` 或 `@Mod.EventBusSubscriber` + `@SubscribeEvent`；Forge 不捆绑 MixinExtras，需 `mixinextras-common` 注解处理器加 `mixinextras-forge` 运行时并 `modstitchJiJ` 打包。
