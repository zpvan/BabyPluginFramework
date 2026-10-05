# 开源项目基础文件补充实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BabyPluginFramework 补充开源项目基础五件套：LICENSE（Apache 2.0）、NOTICE、中文 README、英文 README、CHANGELOG。

**Architecture:** 全部为仓库根目录新增文档文件，不改动任何代码或构建配置。LICENSE 从 apache.org 拉取官方标准文本并替换 copyright 占位行；双语 README 内容一一对应、互挂链接；CHANGELOG 按 Keep a Changelog 格式从 15 个功能提交提炼（最新功能提交日期 2025-05-15 作为 v0.1.0 日期）。

**Tech Stack:** Markdown、Apache License 2.0 文本、curl、git。

**Spec:** `docs/superpowers/specs/2026-10-05-open-source-essentials-design.md`

**关键事实（已探查确认，供各 Task 使用）：**
- 包名：宿主 `com.knox.babypluginframework`、插件 `com.knox.pluginapk`、接口库 `com.knox.pluginlibrary`
- 接口库内容：`IPlugin.kt`（插件契约：name、getStringForResId）、`Bridge.kt`（HostBridge/PluginBridge 双向通信接口）
- docs 文档：`docs/插件化技术总结.md`、`docs/Activity的插件化解决方案Sdk30.md`、`docs/资源冲突解决方案.md`
- 构建：JDK 17 + Android SDK 35（compileSdk=35, minSdk=30, targetSdk=35），需 `JAVA_HOME`、`ANDROID_HOME`，项目无 local.properties
- copyright：zpvan，年份 2026；CHANGELOG 版本日期：2025-05-15

---

### Task 1: LICENSE 与 NOTICE

**Files:**
- Create: `LICENSE`
- Create: `NOTICE`

- [ ] **Step 1: 拉取 Apache 2.0 官方标准文本**

```bash
cd /Users/knox/Documents/GitWorkSpace/BabyPluginFramework
curl -fsSL https://www.apache.org/licenses/LICENSE-2.0.txt -o LICENSE
wc -l LICENSE
```

Expected: `LICENSE` 下载成功，约 202 行；`head -3 LICENSE` 首行为 `Apache License`。若网络失败，重试一次；仍失败则停止报告。

- [ ] **Step 2: 替换附录中的 copyright 占位行**

LICENSE 文件末尾附录有一行：

```
   Copyright [yyyy] [name of copyright owner]
```

用编辑器将其替换为：

```
   Copyright 2026 zpvan
```

验证：

```bash
grep -n "Copyright 2026 zpvan" LICENSE
grep -c "\[yyyy\]" LICENSE
```

Expected: 第一条输出 1 行匹配；第二条输出 `0`（占位符已全部替换）。

- [ ] **Step 3: 创建 NOTICE**

创建 `NOTICE`，内容如下（完整内容，逐字写入）：

```text
BabyPluginFramework
Copyright 2026 zpvan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

- [ ] **Step 4: 提交**

```bash
git add LICENSE NOTICE
git commit -m "(docs) Add Apache License 2.0 and NOTICE"
```

Expected: 提交成功，2 个新文件。

---

### Task 2: CHANGELOG.md

**Files:**
- Create: `CHANGELOG.md`

- [ ] **Step 1: 创建 CHANGELOG.md**

创建 `CHANGELOG.md`，内容如下（完整内容，逐字写入）：

```markdown
# Changelog

本项目的所有重要变更将记录在此文件中。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [0.1.0] - 2025-05-15

### Added

- **ClassLoader 加载阶段**
  - 从宿主 assets 目录加载插件 APK
  - 基于 ISP 的接口隔离：宿主（:app）与插件（:pluginapk）仅依赖接口库（:pluginlibrary）的 `IPlugin` 契约
  - 基于 Bridge 模式的宿主-插件双向通信（`HostBridge` / `PluginBridge`，支持推/拉数据）
  - 插件 APK 瘦身（45MB → 29MB）
  - 注册 Copy Task：assemble 后自动将插件 APK 拷贝至宿主 assets 目录
- **资源加载阶段**
  - 从插件 APK 加载 string 资源
- **组件启动阶段**
  - 通过在宿主 AndroidManifest.xml 中声明的方式启动插件 Activity
  - 以同样方式启动插件 Service
- **Stub Activity 阶段**
  - 插件 AssetManager 优先加载插件资源，未命中时回落宿主资源
  - 重写 Activity.getResources 返回插件 Resources，使插件 UI 正确显示
  - 记录插件与宿主资源 ID 冲突问题：启动插件 TestActivity1 时错误显示宿主 ProxyActivity 布局
  - Samsung S20（SDK 30）真机适配：PackageManager 交互问题排查与解决记录
  - 重构 MainActivity 使主流程逻辑更清晰
  - 新增插件化技术文档（docs/ 目录）
```

- [ ] **Step 2: 提交**

```bash
git add CHANGELOG.md
git commit -m "(docs) Add CHANGELOG for v0.1.0 distilled from feature commits"
```

Expected: 提交成功，1 个新文件。

---

### Task 3: README.md（中文主版）

**Files:**
- Create: `README.md`

- [ ] **Step 1: 创建 README.md**

创建 `README.md`，内容如下（完整内容，逐字写入）：

````markdown
# BabyPluginFramework

**中文** | [English](README_EN.md)

> 一个 Android 插件化（Plugin）技术的学习记录项目：从零实现宿主 App 加载插件 APK，逐步覆盖 ClassLoader、资源加载、组件启动与占坑（Stub）方案。

⚠️ **本项目为学习/演示用途，非生产可用框架。**

## 项目简介

本项目记录了一个最小 Android 插件化框架的完整演进过程：宿主 App 在运行时将插件 APK（未安装）加载进自己的进程，启动其中的 Activity/Service 并正确加载其资源。每个阶段遇到的问题与解决方案都沉淀为 docs/ 下的技术文档和 git 提交历史。

## 架构

三个 Gradle 模块：

| 模块 | 包名 | 职责 |
|---|---|---|
| `:app` | `com.knox.babypluginframework` | 宿主 App：加载插件 APK、启动插件组件、资源代理 |
| `:pluginapk` | `com.knox.pluginapk` | 插件 APK：被宿主加载的测试插件，含 TestActivity1~5 与 TestService1 |
| `:pluginlibrary` | `com.knox.pluginlibrary` | 共享接口库：宿主与插件的通信契约 |

依赖关系：`:app` → `:pluginlibrary` ← `:pluginapk`（接口隔离原则 ISP：宿主与插件互不依赖，仅共同依赖接口库）

核心接口：

- `IPlugin` — 插件对宿主暴露的契约（插件名称、资源访问）
- `HostBridge` / `PluginBridge` — Bridge 模式双向通信：宿主推送数据给插件、插件从宿主拉取数据

## 技术演进

| 阶段 | 内容 | 文档 |
|---|---|---|
| 1. ClassLoader 加载 | 加载 assets 中的插件 APK；接口隔离与 Bridge 通信；插件瘦身 45MB→29MB；Copy Task 自动拷贝插件 APK | [插件化技术总结](docs/插件化技术总结.md) |
| 2. 资源加载 | 从插件 APK 加载 string 资源 | [插件化技术总结](docs/插件化技术总结.md) |
| 3. 组件启动 | 通过宿主 AndroidManifest.xml 声明方式启动插件 Activity/Service；SDK 30 真机适配 | [Activity 的插件化解决方案 Sdk30](docs/Activity的插件化解决方案Sdk30.md) |
| 4. Stub Activity 与资源冲突 | 插件 AssetManager 优先加载插件资源、回落宿主；重写 getResources 显示插件 UI；资源 ID 冲突分析 | [资源冲突解决方案](docs/资源冲突解决方案.md) |

各阶段的详细实现过程与问题排查记录见 git 提交历史（dev-ClassLoader → dev-Resources → dev-Component → dev-StubActivity 四个分支已合并至 main）。

## 构建指南

环境要求：

- **JDK 17**（AGP 8.9.1 + Gradle 8.11.1 要求；更高版本 JDK 未经测试）
- **Android SDK Platform 35**（compileSdk = 35，minSdk = 30，targetSdk = 35）

项目不含 `local.properties`，通过环境变量指定 JDK 与 SDK 路径：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

构建成功后，插件 APK 会由 Copy Task 自动拷贝至 `app/src/main/assets/`，直接安装宿主 App 即可体验插件加载。

## 已知限制

- 插件与宿主资源 ID 冲突时，可能错误显示宿主同 ID 资源（详见 [资源冲突解决方案](docs/资源冲突解决方案.md)）
- 仅在 Samsung S20（Android 11，SDK 30）真机实测过
- 插件 Activity/Service 需预先在宿主 AndroidManifest.xml 中声明，真正的占坑（Stub）免声明方案仍在探索中

## License

[Apache License 2.0](LICENSE) — Copyright 2026 zpvan
````

- [ ] **Step 2: 验证文档内相对链接的目标文件存在**

```bash
test -f docs/插件化技术总结.md && echo "link1 OK"
test -f docs/Activity的插件化解决方案Sdk30.md && echo "link2 OK"
test -f docs/资源冲突解决方案.md && echo "link3 OK"
test -f LICENSE && echo "link4 OK"
```

Expected: 四行均输出 OK。任一失败则修正 README 中的链接路径。

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "(docs) Add Chinese README with architecture, evolution stages and build guide"
```

Expected: 提交成功，1 个新文件。

---

### Task 4: README_EN.md（英文版）

**Files:**
- Create: `README_EN.md`

- [ ] **Step 1: 创建 README_EN.md**

创建 `README_EN.md`，内容如下（完整内容，逐字写入）：

````markdown
# BabyPluginFramework

[中文](README.md) | **English**

> A learning project for Android plugin technology: building from scratch a host app that loads a plugin APK, progressively covering ClassLoader, resource loading, component launching, and the stub (placeholder) approach.

⚠️ **This project is for learning/demo purposes only. It is NOT production-ready.**

## Introduction

This project documents the complete evolution of a minimal Android plugin framework: the host app loads an (uninstalled) plugin APK into its own process at runtime, launches its Activities/Services, and loads its resources correctly. Problems and solutions at each stage are captured in the technical documents under docs/ (in Chinese) and in the git history.

## Architecture

Three Gradle modules:

| Module | Package | Responsibility |
|---|---|---|
| `:app` | `com.knox.babypluginframework` | Host app: loads the plugin APK, launches plugin components, proxies resources |
| `:pluginapk` | `com.knox.pluginapk` | Plugin APK: a test plugin loaded by the host, with TestActivity1~5 and TestService1 |
| `:pluginlibrary` | `com.knox.pluginlibrary` | Shared interface library: the communication contract between host and plugin |

Dependencies: `:app` → `:pluginlibrary` ← `:pluginapk` (Interface Segregation Principle: host and plugin never depend on each other, only on the shared contract)

Core interfaces:

- `IPlugin` — the contract a plugin exposes to the host (plugin name, resource access)
- `HostBridge` / `PluginBridge` — Bridge-pattern bidirectional communication: host pushes data to plugin; plugin pulls data from host

## Evolution Stages

| Stage | Content | Document (Chinese) |
|---|---|---|
| 1. ClassLoader | Load plugin APK from assets; interface isolation and Bridge communication; plugin slimming 45MB→29MB; Copy Task for automatic APK deployment | [插件化技术总结](docs/插件化技术总结.md) |
| 2. Resources | Load string resources from the plugin APK | [插件化技术总结](docs/插件化技术总结.md) |
| 3. Components | Launch plugin Activity/Service via declarations in the host's AndroidManifest.xml; on-device adaptation for SDK 30 | [Activity 的插件化解决方案 Sdk30](docs/Activity的插件化解决方案Sdk30.md) |
| 4. Stub Activity & Resource Conflicts | Plugin AssetManager loads plugin resources first, falls back to host; override getResources to render plugin UI; analysis of resource ID conflicts | [资源冲突解决方案](docs/资源冲突解决方案.md) |

See the git history for the detailed implementation journey (the four branches dev-ClassLoader → dev-Resources → dev-Component → dev-StubActivity have all been merged into main).

## Build

Requirements:

- **JDK 17** (required by AGP 8.9.1 + Gradle 8.11.1; newer JDK versions are untested)
- **Android SDK Platform 35** (compileSdk = 35, minSdk = 30, targetSdk = 35)

The project has no `local.properties`; point Gradle at your JDK and SDK via environment variables:

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

After a successful build, a Copy Task places the plugin APK into `app/src/main/assets/` automatically — just install and run the host app to see plugin loading in action.

## Known Limitations

- When plugin and host resource IDs collide, the host's resource with the same ID may be displayed instead (see [资源冲突解决方案](docs/资源冲突解决方案.md))
- Only tested on a Samsung S20 (Android 11, SDK 30) physical device
- Plugin Activities/Services must be pre-declared in the host's AndroidManifest.xml; a true declaration-free stub approach is still being explored

## License

[Apache License 2.0](LICENSE) — Copyright 2026 zpvan
````

- [ ] **Step 2: 提交**

```bash
git add README_EN.md
git commit -m "(docs) Add English README mirroring the Chinese version"
```

Expected: 提交成功，1 个新文件。

---

### Task 5: 全量验证与推送

**Files:** 无（只读验证 + push）

- [ ] **Step 1: 验证五件套齐全且 README 链接目标均存在**

```bash
ls LICENSE NOTICE CHANGELOG.md README.md README_EN.md
test -f README_EN.md && test -f README.md && test -f LICENSE && \
test -f docs/插件化技术总结.md && test -f docs/Activity的插件化解决方案Sdk30.md && \
test -f docs/资源冲突解决方案.md && echo "all link targets OK"
grep -c "Copyright 2026 zpvan" LICENSE NOTICE README.md README_EN.md
```

Expected: 5 个文件全部列出；输出 `all link targets OK`；grep 输出 4 个文件各至少 1 处匹配。

- [ ] **Step 2: 验证 LICENSE 与官方文本一致（除 copyright 行外）**

```bash
curl -fsSL https://www.apache.org/licenses/LICENSE-2.0.txt | diff - LICENSE | head -10
```

Expected: diff 仅显示 copyright 占位行一处差异（原 `[yyyy] [name of copyright owner]` vs `2026 zpvan`）。若有其他差异，检查并修正 LICENSE。

- [ ] **Step 3: 推送到 origin**

```bash
git push origin main
git fetch origin && git rev-parse main origin/main
```

Expected: 推送成功（fast-forward）；两个 SHA 相同。

- [ ] **Step 4: 确认工作区干净**

```bash
git status --short --branch
```

Expected: 无未提交文件，`## main...origin/main`（无 ahead/behind）。
