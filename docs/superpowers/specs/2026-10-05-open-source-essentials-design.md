# 设计文档：开源项目基础文件补充

**日期**：2026-10-05
**状态**：已批准

## 背景与目标

BabyPluginFramework 托管于 GitHub（github.com/zpvan/BabyPluginFramework），是一个 Android 插件化框架的**学习记录/技术演示**项目（用户明确定位，非正式开源框架）。当前缺失全部开源基础设施文件（README、LICENSE 等），仅有 `docs/` 下 3 篇中文技术文档。

**目标**：补充最小但完整的开源项目基础文件，让他人能看懂项目、合法使用代码。

## 决策记录（用户确认）

1. **项目定位**：学习记录/技术演示 —— 只需最小化补充，不上 CI / CONTRIBUTING / issue 模板
2. **许可证**：Apache 2.0（Android 生态主流，含专利授权）
3. **README 语言**：中英双语 —— `README.md` 中文主版 + `README_EN.md` 英文版，互挂链接
4. **方案**：极简三件套 + 轻量规范文件（CHANGELOG.md + NOTICE）

## 文件清单与职责

| 文件 | 职责 |
|---|---|
| `LICENSE` | Apache 2.0 许可证全文（官方标准文本），copyright 为 `zpvan`，年份 2026 |
| `NOTICE` | Apache 2.0 配套署名文件：项目名 BabyPluginFramework + copyright 声明 |
| `README.md` | 中文主版，GitHub 项目门面 |
| `README_EN.md` | 英文版，与中文版内容一一对应 |
| `CHANGELOG.md` | Keep a Changelog 格式，v0.1.0 从已有 15 个功能提交整理 |

## README.md 结构（中文，英文版同构）

1. **项目名 + 一句话简介** + 语言切换链接（中文 | English）
2. **项目简介** — Android 插件化框架学习记录项目，明确定位（学习/演示，非生产可用）
3. **架构** — 三模块划分：`:app` 宿主、`:pluginapk` 插件、`:pluginlibrary` 接口库（IPlugin 通信契约，ISP 隔离），Bridge 模式实现 Host-Plugin 通信
4. **技术演进** — 四阶段，每阶段链接到 `docs/` 对应中文技术文档：
   - ClassLoader 加载（含插件 APK 瘦身 45MB→29MB、Copy Task）
   - 资源加载（插件 string 资源）
   - 组件启动（manifest 声明方式启动插件 Activity/Service）
   - Stub Activity 与资源冲突解决（插件/宿主资源 ID 冲突处理）
5. **构建指南** — JDK 17 + Android SDK 35（compileSdk=35, minSdk=30），需设置 `JAVA_HOME`、`ANDROID_HOME` 环境变量（项目无 local.properties），执行 `./gradlew assembleDebug`
6. **已知限制** — 从 commit 记录提炼（如资源 ID 冲突导致宿主布局被显示的问题现状、SDK 30 真机实测记录）
7. **License** — Apache 2.0，指向 LICENSE 文件

## CHANGELOG.md 内容

遵循 Keep a Changelog 格式：`## [0.1.0] - <日期>`（日期取 15 个功能提交中最新的提交日期），按 `Added` 分组，将 15 个功能提交按四阶段**提炼**为用户可读条目（不逐条照抄 commit message）。

## 明确不做（YAGNI）

- 不设 CI / GitHub Actions
- 不加 CONTRIBUTING.md / issue 模板 / PR 模板 / Code of Conduct / SECURITY.md
- 不发 GitHub Release、不打 tag
- 不修改任何代码或构建配置
- 不为 superpowers 流程文档（docs/superpowers/）做特殊处理 —— 保留现状

## 验证标准

- 5 个文件均存在于仓库根目录并提交到 main
- `README.md` 中所有相对链接（docs/ 文档、LICENSE、README_EN.md）在 GitHub 上可点击跳转（本地用 `test -f` 验证链接目标存在）
- LICENSE 为 Apache 2.0 官方标准文本（与 apache.org 发布版本一致，仅替换 copyright 占位行）
- 推送到 origin/main 后 GitHub 仓库页正确渲染中文 README
