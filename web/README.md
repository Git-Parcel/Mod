# Git Parcel Web 控制台（前端）

Vue 3 + TypeScript + Vite 单页应用，构建产物由 Gradle 打包进模组 jar（`gitparcel/web/`），在游戏内执行 `/parcel web start` 后由模组内置 HTTP 服务托管。

## 开发

前置：Node.js 20.19+（建议 22+）。

```bash
npm install
npm run dev
```

Vite 开发服务器默认运行在 5173 端口，`/api` 请求代理到本机 5639 端口——先在游戏内执行 `/parcel web start`，再从其输出的链接中取出 token 访问 `http://localhost:5173/?token=<token>`。

修改后端 API 结构时同步更新 `src/api.ts` 中的类型定义。

## 构建

```bash
npm run build
```

类型检查（`vue-tsc`）通过后输出到 `dist/`。Gradle 的 `buildWebUi` 任务会自动执行这一步并把 `dist/` 打进 jar，无需手动构建。

界面布局与管理功能另行设计，当前仅是验证链路的脚手架页面。
