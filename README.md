# BabyPluginFramework

[![build](https://github.com/zpvan/BabyPluginFramework/actions/workflows/build.yml/badge.svg)](https://github.com/zpvan/BabyPluginFramework/actions/workflows/build.yml)

**English** | [中文](README_CN.md)

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
