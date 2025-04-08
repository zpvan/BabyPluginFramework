package com.knox.pluginlibrary

// 共享模块（Host 和 Plugin 都依赖）
interface HostBridge {
    // Host 推送给 Plugin 的方法
    fun pushToPlugin(data: Any): Boolean
    // Plugin 从 Host 拉取数据的方法
    fun pullFromHost(key: String): Any?
    // 注册 Plugin 桥接接口
    fun registerPlugin(bridge: PluginBridge)
}

interface PluginBridge {
    // Plugin 接收来自 Host 推送的数据
    fun onReceiveData(data: Any): Boolean
    // Host 从 Plugin 拉取数据的方法
    fun onPullData(key: String): Any?
}