# Agent 指南

## 最高约束

“最高约束”这一二级标题下的内容由用户（人类开发者）亲自编写，Agent 不得擅动；本文件其他二级标题下的内容 Agent 可以修改。

- 目标：把世界中的长方体区域抽象为 parcel，并使用 Git 保存和管理 parcel 的每一个快照。
- 目标用户包括不了解或不愿意深入了解 Git 的普通玩家
- 目前仍处于 Beta 阶段，允许进行大规模重构，并应彻底删除废弃的类、方法、字段、测试和文档，不为旧接口保留兼容层

## 其他

- 不保留无意义的提交

### 文档和 Agent 指南

Agent 指南文件分工：

- `AGENTS.md`（本文件，入库）：项目约束、编码指南与踩坑记录
- `AGENTS-LOCAL.md`（本地，不纳入版本管理）：本机开发环境相关的经验，包含现成的 Minecraft 源码存放位置

文档：

- 本仓库中的文档一律使用简体中文，使用符合中国大陆规范的标点符号
- 本模组的设计细节位于 `docs/DESIGN.md`，docs/ 下其他文件通常不需要访问，仅在用户明确要求时作为参考。
- 仓库中的文档代表目标，可超前于代码实现，不可滞后
- Agent 工作时应先更新文档，再编写实现
- 如果执行任务时发现现有文档存在错误、矛盾、过时、缺失等问题而应当修改，但用户没预料到，应向用户请示，不能编写超前于文档的实现或擅自修改文档
- 执行任务时应适时提交更改，而非完成全部任务后一次性提交
- 若踩到值得记录的坑，可在任务完成后报告

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

## 踩坑记录

实现中踩过并确认的坑，供后续任务避让；条目应写清现象与结论，不罗列排查过程。
