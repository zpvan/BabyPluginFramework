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
