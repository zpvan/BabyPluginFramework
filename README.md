# BabyPluginFramework

[![build](https://github.com/zpvan/BabyPluginFramework/actions/workflows/build.yml/badge.svg)](https://github.com/zpvan/BabyPluginFramework/actions/workflows/build.yml)

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
