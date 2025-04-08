package com.knox.babypluginframework

import com.knox.pluginlibrary.HostBridge
import com.knox.pluginlibrary.PluginBridge

// Host端实现
class HostBridgeImpl : HostBridge {
    private val pluginBridges = mutableListOf<PluginBridge>()

    override fun registerPlugin(bridge: PluginBridge) {
        pluginBridges.add(bridge)
    }

    override fun pushToPlugin(data: Any): Boolean {
        return pluginBridges.any { it.onReceiveData(data) }
    }

    override fun pullFromHost(key: String): Any? {
        // Host 侧实现数据查询逻辑
        return when (key) {
            "userInfo" -> String
            "appConfig" -> String
            else -> null
        }
    }

    // Host 从 Plugin 拉取数据
    fun pullFromPlugin(key: String): Any? {
        for (bridge in pluginBridges) {
            val result = bridge.onPullData(key)
            if (result != null) return result
        }
        return null
    }
}