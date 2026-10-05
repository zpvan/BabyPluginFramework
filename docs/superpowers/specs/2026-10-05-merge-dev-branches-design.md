# 设计文档：将 dev 分支功能合并进 main

**日期**：2026-10-05
**状态**：已批准

## 背景与目标

仓库存在 4 个远程功能分支，均为 Android 插件化框架（BabyPluginFramework）不同阶段的开发成果：

| 分支 | 领先 main 的 commit 数 | 内容 |
|---|---|---|
| `origin/dev-ClassLoader` | 5 | 加载 assets 插件 APK、IPlugin 接口隔离（ISP）、Bridge 模式 Host-Plugin 通信、APK 瘦身（45MB→29MB）、Copy Task 注册 |
| `origin/dev-Resources` | 7 | + 从插件 APK 加载 string 资源 |
| `origin/dev-Component` | 9 | + 通过 hostApp AndroidManifest 声明方式启动插件 Activity |
| `origin/dev-StubActivity` | 15 | + Stub/占坑 Activity 方案、插件与宿主资源 ID 冲突处理、插件技术文档 |

**目标**：让 `main` 分支合并全部 dev 分支的功能。

## 关键发现

4 个 dev 分支位于**同一条线性提交链**上，彼此为祖先关系，无任何分叉：

```
e9600a5 (main) ── ... ── da0205f (dev-ClassLoader) ── ... ── 01e98da (dev-Resources)
              ── ... ── 16e55f0 (dev-Component) ── ... ── a4cc028 (dev-StubActivity)
```

`dev-StubActivity` 是链的顶端，包含其他 3 个分支的全部提交。因此：

- **不需要 cherry-pick** —— 逐个 cherry-pick 会产生 15 个内容重复但 SHA 不同的提交，污染历史
- **不需要 squash** —— 线性历史本身已清晰，fast-forward 保留全部原始提交信息
- **fast-forward 合并**即可让 main 获得所有分支功能，且 `origin/main` 与 `origin/dev-StubActivity` 指向同一 commit

## 决策记录（用户确认）

1. **合并方式**：Fast-forward 合并（`git merge --ff-only`）
2. **分支清理**：保留全部 dev 分支，不删除
3. **验证策略**：合并后先执行 `./gradlew assembleDebug` 构建验证，通过后再 push

## 执行流程

1. **同步远程状态** — `git fetch origin`，确认 4 个 dev 分支 tip 无新增提交
2. **对齐 main 到 dev 链顶端** — 由于本设计文档的提交（`(docs) Add design spec...`）在 main 上但不在 dev-StubActivity 链中，纯 `--ff-only` 无法执行。改为在 `main` 上执行 `git rebase origin/dev-StubActivity`：将该 docs 提交（仅本地存在、未推送）重放到 `a4cc028` 之上，结果与 fast-forward 完全等价 —— 线性历史，main 包含全部 15 个功能提交 + docs 提交，原始 15 个提交的 SHA 保持不变
   - 若 rebase 出现冲突（docs 提交只新增 `docs/` 下文件，预期零冲突），停止并重新评估
   - 若执行时尚未提交设计文档，则直接 `git merge --ff-only origin/dev-StubActivity`
3. **构建验证** — 执行 `./gradlew assembleDebug`，确认 hostApp 与 pluginApk 均编译通过（含 Copy Task 将插件 APK 拷贝至宿主 assets 目录）
4. **推送** — `git push origin main`
5. **收尾确认** — 验证 `origin/main` 与 `origin/dev-StubActivity` 指向同一 commit；dev 分支保持原样

## 验证标准

- `git log --oneline main` 包含全部 15 个功能提交（原始 SHA 不变）+ 设计文档提交，覆盖 ClassLoader → Resources → Component → StubActivity 四个阶段
- `./gradlew assembleDebug` 输出 BUILD SUCCESSFUL
- `git rev-parse origin/main` == main 本地 HEAD（push 后），且 15 个功能提交与 `origin/dev-StubActivity` 完全一致

## 明确不做（YAGNI）

- 不 cherry-pick 任何提交
- 不 squash / 不合并为单个提交
- 不删除任何 dev 分支（本地或远程）
- 不改写任何**已推送**的提交历史（15 个功能提交的 SHA 全部保持不变；仅重放本地未推送的 docs 提交）
- 不修改任何代码内容 —— 本任务为纯 git 历史操作
