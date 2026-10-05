# 合并 dev 分支功能进 main 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 4 个远程 dev 分支（dev-ClassLoader / dev-Resources / dev-Component / dev-StubActivity）的全部功能合并进 main，构建验证通过后推送到 origin。

**Architecture:** 4 个 dev 分支位于同一条线性提交链上，dev-StubActivity 为链顶（领先 main 基点 15 个 commit，SHA `a4cc028`）。由于 main 上存在两个未推送的 docs 提交（设计文档 + 本计划），采用 `git rebase origin/dev-StubActivity` 将它们重放到链顶，效果与 fast-forward 等价 —— 15 个功能提交的 SHA 全部不变，历史保持线性。随后执行 `./gradlew assembleDebug` 验证 3 个 Gradle 模块（`:app` 宿主、`:pluginapk` 插件、`:pluginlibrary` 通信库）均可编译，最后 push。

**Tech Stack:** Git（rebase/ff 语义）、Gradle Wrapper（Android 构建）。

**Spec:** `docs/superpowers/specs/2026-10-05-merge-dev-branches-design.md`

---

### Task 1: 同步远程状态并确认拓扑未变化

**Files:** 无（纯 git 只读操作）

- [ ] **Step 1: Fetch 远程最新状态**

```bash
cd /Users/knox/Documents/GitWorkSpace/BabyPluginFramework
git fetch origin
```

Expected: 无输出或仅进度信息；无新分支、无强制更新提示。

- [ ] **Step 2: 确认 dev-StubActivity 链顶 SHA 未变**

```bash
git rev-parse origin/dev-StubActivity
```

Expected: 输出以 `a4cc028` 开头。若不同，说明远程有新提交 —— **停止执行并报告**，不要继续后续 Task。

- [ ] **Step 3: 确认 3 个早期 dev 分支仍是 dev-StubActivity 的祖先**

```bash
git merge-base --is-ancestor origin/dev-ClassLoader origin/dev-StubActivity && echo "ClassLoader OK"
git merge-base --is-ancestor origin/dev-Resources origin/dev-StubActivity && echo "Resources OK"
git merge-base --is-ancestor origin/dev-Component origin/dev-StubActivity && echo "Component OK"
```

Expected: 三行均输出 `OK`。任何一个失败说明分支发生分叉 —— **停止执行并报告**。

- [ ] **Step 4: 确认工作区干净且在 main 上**

```bash
git status --short --branch
```

Expected: 首行显示 `## main...origin/main [ahead 2]`，无未跟踪/未提交文件。ahead 的 2 个提交即设计文档与实施计划两个 docs 提交。

---

### Task 2: Rebase main 到 dev-StubActivity 链顶

**Files:** 无（纯 git 历史操作；2 个 docs 提交仅新增 `docs/superpowers/` 下文件，与 15 个功能提交无文件交集，预期零冲突）

- [ ] **Step 1: 执行 rebase**

```bash
git rebase origin/dev-StubActivity
```

Expected: 输出 `Successfully rebased and updated refs/heads/main.` 或提示 main 已是最新。
若出现 CONFLICT —— **不要手动解决**，执行 `git rebase --abort` 并停止报告（预期不会发生）。

- [ ] **Step 2: 验证 15 个功能提交 SHA 全部保留**

```bash
git log --oneline origin/dev-StubActivity | head -15
git log --oneline main | head -17
```

Expected: main 的最近 17 个提交中，第 3~17 个与 dev-StubActivity 的 15 个提交 **SHA 逐一相同**；main 最新 2 个提交是 docs 提交（新 SHA）。

- [ ] **Step 3: 验证 dev-StubActivity 是 main 的祖先（线性）**

```bash
git merge-base --is-ancestor origin/dev-StubActivity main && echo "linear OK"
```

Expected: 输出 `linear OK`。

- [ ] **Step 4: 确认工作区文件已切换到链顶内容**

```bash
ls app pluginapk pluginlibrary
git log --oneline -3
```

Expected: 三个模块目录均存在（rebase 前 main 上只有 `app/`）；最近提交显示 docs 提交在最顶。

---

### Task 3: 构建验证

**Files:** 无（只执行构建，不修改代码）

- [ ] **Step 1: 执行全量 Debug 构建**

```bash
cd /Users/knox/Documents/GitWorkSpace/BabyPluginFramework
./gradlew assembleDebug
```

Expected: 输出 `BUILD SUCCESSFUL`，`:app:assembleDebug`、`:pluginapk:assembleDebug`、`:pluginlibrary:assembleDebug`（或对应 library 的 assemble 任务）均成功；插件 APK 由 Copy Task 拷贝至 `app/src/main/assets/`（日志中可见 copy 任务执行）。

若构建失败 —— **不要修改代码、不要 push**。记录完整错误输出并停止报告（构建失败说明 dev 链顶代码本身有问题，超出本计划范围）。

- [ ] **Step 2: 确认构建未污染工作区（产物均被 .gitignore 覆盖）**

```bash
git status --short
```

Expected: 无输出（干净），或仅 `app/src/main/assets/` 下插件 APK 等已被跟踪文件的预期变化。若出现未跟踪文件 —— 列出并停止报告，不要提交。

---

### Task 4: 推送并收尾验证

**Files:** 无

- [ ] **Step 1: 推送 main 到 origin**

```bash
git push origin main
```

Expected: 远程接受推送（main 是 fast-forward 推进：origin/main 原在 `e9600a5`，新位置包含其全部历史，无 rejected）。若提示 non-fast-forward —— **不要加 `--force`**，停止报告。

- [ ] **Step 2: 验证远端对齐**

```bash
git fetch origin
git rev-parse main origin/main
```

Expected: 两个 SHA 相同。

- [ ] **Step 3: 验证 4 个 dev 分支保持原样**

```bash
git branch -r
```

Expected: `origin/dev-ClassLoader`、`origin/dev-Resources`、`origin/dev-Component`、`origin/dev-StubActivity` 均仍存在，未被删除或移动。

- [ ] **Step 4: 输出最终验证摘要**

```bash
echo "=== main 领先原基点的提交数 ==="
git rev-list --count e9600a5..main
echo "=== 功能提交 SHA 一致性抽查 ==="
git merge-base --is-ancestor origin/dev-StubActivity main && echo "all 15 feature commits present"
```

Expected: 提交数为 17（15 功能 + 2 docs）；输出 `all 15 feature commits present`。

---

## 回滚预案

- Task 2 执行后、Task 4 推送前如需撤销：先执行 `git reflog` 找到 rebase 前 main 的位置（即 plan 提交 `2026-10-05-merge-dev-branches.md` 对应的提交），然后 `git reset --hard <该SHA>`。
- Task 4 推送后如需撤销：联系仓库所有者评估（涉及已推送历史，超出本计划自动处理范围）。
