package com.knox.pluginapk

import com.knox.pluginlibrary.HostBridge
import com.knox.pluginlibrary.PluginBridge

// Plugin 端实现
class PluginBridgeImpl(private val hostBridge: HostBridge) : PluginBridge {
    init {
        // 向 Host 注册自己
        hostBridge.registerPlugin(this)
    }

    override fun onReceiveData(data: Any): Boolean {
        // 处理从 Host 推送来的数据
        return when(data) {
            is String -> {
                // 处理用户信息
                true
            }
            else -> false
        }
    }

    override fun onPullData(key: String): Any? {
        // 响应 Host 的数据拉取请求
        return when(key) {
            "pluginStats" -> "Ready"
            else -> null
        }
    }

    // Plugin 主动从 Host 拉取数据
    fun getDataFromHost(key: String): Any? {
        return hostBridge.pullFromHost(key)
    }

    // Plugin 主动推送数据到 Host
    fun pushToHost(data: Any): Boolean {
        // 通过反射调用 Host 的接收方法
        return try {
            val hostClass = hostBridge.javaClass
            val method = hostClass.getMethod("receivePluginData", Any::class.java)
            method.invoke(hostBridge, data) as Boolean
        } catch (e: Exception) {
            false
        }
    }
}