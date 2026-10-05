# 设计文档：GitHub Actions 最小 CI

**日期**：2026-10-05
**状态**：已批准

## 背景与目标

BabyPluginFramework 定位为学习记录/技术演示项目。补充最小化 CI 的核心价值：在干净环境中持续验证"按 README 构建指南确实能编译通过"，让构建指南成为不会失效的活文档。

## 决策记录（用户确认）

1. **触发范围**：仅 push 到 `main` + 所有 PR（dev 分支不触发）
2. **README 徽章**：双语 README 顶部加 build 状态徽章
3. **方案**：build + lint + 单元测试 三 job（用户选择超出最小版的方案 C）

## 关键事实（本地已验证，2026-10-05）

- `./gradlew testDebugUnitTest` —— **通过**（3 个模块的 ExampleUnitTest，BUILD SUCCESSFUL）
- `./gradlew lintDebug` —— **失败**：`MissingClass` 错误。宿主 `app/src/main/AndroidManifest.xml` 声明了插件组件（如 `com.knox.pluginapk.TestService1`），这些类不在宿主 classpath 中 —— 这是插件化框架的固有机制（类运行时从插件 APK 加载），属于**误报**
- `settings.gradle.kts` 中阿里云镜像排在 google()/mavenCentral() 之前，GitHub runner（海外）访问较慢但能通，失败自动回落，首次 CI 运行偏慢属正常
- `gradle/wrapper/gradle-wrapper.jar` 已提交，CI 可直接使用 wrapper

## 设计

### 1. Workflow 结构（`.github/workflows/build.yml`）

触发：`on: push: branches: [main]` + `on: pull_request`

| Job | 步骤 | 失败策略 |
|---|---|---|
| `build` | checkout → setup-java（Temurin 17，启用 Gradle 缓存）→ android-actions/setup-android → `./gradlew assembleDebug` | 严格 |
| `test` | 同环境 → `./gradlew testDebugUnitTest` | 严格 |
| `lint` | 同环境 → `./gradlew lintDebug` | 严格（依赖第 2 节的配置修复） |

三个 job 相互独立、并行运行（无 needs 依赖），各自完成环境搭建。

### 2. lint 误报处理

修改 `app/build.gradle.kts` 的 `android {}` 块，新增 lint 配置（3 行）：

```kotlin
lint {
    disable += "MissingClass"
}
```

仅禁用 `MissingClass` 单项，其余 lint 检查保持严格。不用 `continue-on-error` —— 永久黄标的 lint 没有价值，绿色的 lint 才是有效检查。

### 3. README 徽章

`README.md` 与 `README_EN.md` 顶部（标题下、语言切换行前）加 GitHub Actions workflow 状态徽章，链到 Actions 页面：

```markdown
[![build](https://github.com/zpvan/BabyPluginFramework/actions/workflows/build.yml/badge.svg)](https://github.com/zpvan/BabyPluginFramework/actions/workflows/build.yml)
```

## 明确不做（YAGNI）

- 无 instrumented 测试（需模拟器，慢且 flaky）
- 无多 JDK/多 SDK 测试矩阵
- 无 APK artifact 上传、无发布流程
- 除 `app/build.gradle.kts` 的 lint 配置外，不修改任何代码或构建配置

## 验证标准

- 本地（JDK 17 + SDK 35 环境）：`./gradlew lintDebug` 加配置后 BUILD SUCCESSFUL（实施时先本地验证再提交）
- push 到 main 后，GitHub Actions 三个 job（build / test / lint）全部绿色
- README 徽章正确渲染并显示 passing
