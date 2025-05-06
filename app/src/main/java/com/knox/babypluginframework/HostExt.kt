package com.knox.babypluginframework

import android.content.Context
import android.util.Log
import com.knox.pluginlibrary.HostBridge
import com.knox.pluginlibrary.IPlugin
import com.knox.pluginlibrary.PluginBridge
import dalvik.system.PathClassLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "HostExt"

/**
 * 将 assets 目录中的插件 APK 拷贝到应用私有目录
 * @param context 上下文
 * @param assetApkName assets 中的 APK 文件名
 * @return 拷贝后的 APK 文件对象
 */
internal fun extractPluginApkFromAssets(
    context: Context?,
    assetApkName: String = "pluginapk-debug.apk"
): Result<File> {
    context ?: return Result.failure(IllegalArgumentException("Context is null"))
    try {
        // 创建插件目录
        val pluginDir = File(context.filesDir, "plugins")
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }

        // 目标 APK 文件路径
        val targetApkFile = File(pluginDir, assetApkName)

        // 如果文件已存在且不需要更新，直接返回
//        if (targetApkFile.exists()) {
//            return Result.success(targetApkFile)
//        }

        // 打开 assets 中的文件
        context.assets.open(assetApkName).use { inputStream ->
            // 创建输出流
            FileOutputStream(targetApkFile).use { outputStream ->
                // 缓冲区
                val buffer = ByteArray(8192)
                var length: Int

                // 拷贝文件
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }

                // 确保数据写入
                outputStream.flush()
            }
        }

        return Result.success(targetApkFile)
    } catch (e: IOException) {
        e.printStackTrace()
        return Result.failure(IllegalStateException("Copy2FilesDir failed. msg=${e.message}"))
    }
}

// 使用示例: 从插件APK中加载类并调用其方法
internal fun loadPluginThenInvokeGetNameMethod(context: Context, pluginApkFile: File): String? {
    try {
        // 创建类加载器
        val dexClassLoader = PathClassLoader(
            pluginApkFile.absolutePath,
            null,
            context.classLoader
        )

        // 加载插件中的PluginAndroid类
        val pluginClass = dexClassLoader.loadClass("com.knox.pluginapk.BabyPlugin")

        // 创建实例 (假设有无参构造函数或默认参数)
        val constructor = pluginClass.getDeclaredConstructor()
        // 强转成接口
        val babyPlugin = constructor.newInstance() as IPlugin

        // 获取name
        Log.d(TAG, "BabyPlugin name=${babyPlugin.name}")
        return babyPlugin.name
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

internal fun loadPluginBridge(context: Context, pluginApkFile: File) {
    try {
        // 创建类加载器
        val dexClassLoader = PathClassLoader(
            pluginApkFile.absolutePath,
            null,
            context.classLoader
        )

        // 加载插件中的PluginAndroid类
        val pluginClass = dexClassLoader.loadClass("com.knox.pluginapk.PluginBridgeImpl")

        // 创建实例 (假设有无参构造函数或默认参数)
        val constructor = pluginClass.getDeclaredConstructor(HostBridge::class.java)

        val hostBridgeImpl = HostBridgeImpl()
        val pluginBridge = constructor.newInstance(hostBridgeImpl) as PluginBridge

        val response1 = hostBridgeImpl.pullFromPlugin("pluginStats")
        println("pluginStats1=${response1}")
        val response2 = pluginBridge.onPullData("pluginStats")
        println("pluginStats2=${response2}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}