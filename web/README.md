# Git Parcel Web 控制台（前端）

Vue 3 + TypeScript + Vite 单页应用，构建产物由 Gradle 打包进模组 jar（`gitparcel/web/`），在游戏内执行 `/parcel web start` 后由模组内置 HTTP 服务托管。

## 开发

前置：Node.js 20.19+（建议 22+）。

```bash
npm install
npm run dev
```

Vite 开发服务器默认运行在 5173 端口，`/api` 请求代理到本机 5639 端口——先在游戏内执行 `/parcel web start`，再从其输出的链接中取出 token 访问 `http://localhost:5173/?token=<token>`。

## 无游戏联调

```bash
npm run build
npm run mock
```

`npm run mock` 启动一个内存 mock 服务器（5640 端口），实现与真实控制台相同的令牌语义与全部 `/api` 端点（含分支快照历史、失效游标、异步操作进度），并伺服 `dist/`。打开其输出的带 token URL 即可在浏览器中走查全部页面。

## 测试与检查

```bash
npm test          # vitest 单元测试（纯逻辑：格式化、操作跟踪、快照树重建）
npm run check-i18n  # 中英键位对齐 + 代码引用键存在性审计
npm run build     # vue-tsc 类型检查 + 打包
```

类型检查（`vue-tsc`）通过后输出到 `dist/`。Gradle 的 `buildWebUi` 任务会自动执行这一步并把 `dist/` 打进 jar，无需手动构建。

## 结构

- `src/api/`：类型化 API 客户端（`client.ts` 传输与令牌、`types.ts` 后端 DTO；错误码约定见 `docs/DESIGN.md`）
- `src/views/`：总览、Parcel 列表、Parcel 详情、操作记录、共享仓库五个页面；详情页是薄壳，内容拆在 `src/components/parcel/` 的信息、配置、快照、管理卡片与创建/导入对话框中
- `src/components/`：通用组件（复制、维度/状态标签、进度、坐标输入、快照树）与 `parcel/` 子目录
- `src/composables/`：`apiData`（加载/失败/刷新样板）、`polling`（可见性感知轮询）、`operationsFeed`（共享操作轮询源 + 完成通知跟踪）、`dimensions`、`errorToast`、`trackedOperations`
- `src/locales/`：zh-CN / en-US 语言包；后端只返回稳定错误码，文案全部在前端渲染
- `src/i18n.ts`：语言检测（浏览器语言优先，手动切换持久化）与后端 id（操作类型、维度）的本地化回退
- `scripts/`：mock 服务器与 i18n 审计脚本

界面刷新采用轮询（页面可见时），不使用 WebSocket/SSE。`src/components.d.ts` 为 unplugin-vue-components 生成文件，随构建更新。
