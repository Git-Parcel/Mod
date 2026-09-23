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

## 结构

- `src/api/`：类型化的 API 客户端（`client.ts` 传输与令牌、`types.ts` 后端 DTO、错误码约定见 `docs/DESIGN.md`）
- `src/views/`：总览、Parcel 列表、Parcel 详情、操作记录、共享仓库五个页面
- `src/components/`：复制组件、维度标签、状态标签、进度单元格、坐标输入、快照树（递归树形分支图）
- `src/locales/`：zh-CN / en-US 语言包；后端只返回稳定错误码，文案全部在前端渲染
- `src/i18n.ts`：语言检测（浏览器语言优先，手动切换持久化）与后端 id（操作类型、维度）的本地化回退

界面刷新采用轮询（页面可见时），不使用 WebSocket/SSE。`src/components.d.ts` 为 unplugin-vue-components 生成文件，随构建更新。
