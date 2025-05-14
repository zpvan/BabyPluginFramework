package com.knox.babypluginframework

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.knox.pluginlibrary.IPlugin
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import java.io.File

private const val TAG = "ComponentExt"

/*
 介绍一种最简单的插件化解决方案，其对于四大组件都是适用的
 1）合并所有插件的dex，来解决插件的类的加载问题。
 2）预先在宿主的AndroidManifest文件中声明插件中的四大组件。当然，这样做对于插件中上百个Activity是件很麻烦的事情。
 3）把插件中的所有资源一次性地合并到宿主的资源中。当然，这可能会导致资源id冲突。
 */

internal fun startPluginTestService1(context: Context, pluginApkFile: File) {
    // 1. 合并 DEX 到宿主 ClassLoader
    mergeDexToHostClassLoader(context, pluginApkFile)

    // 2. 启动插件服务
    val intent = Intent()
    val serviceName = "com.knox.pluginapk.TestService1"
    intent.setClassName(context, serviceName)
    context.startService(intent)
}

internal fun startPluginTestActivity1(context: Context, pluginApkFile: File) {
    // 1. 合并 DEX 到宿主 ClassLoader
    mergeDexToHostClassLoader(context, pluginApkFile)
    // 2. 造一个"超级"Resources保存在全局变量中
    IPlugin.bigResources = loadPluginResources(context, pluginApkFile)

    // 3. 启动插件Activity
    val intent = Intent()
    val activityName = "com.knox.pluginapk.TestActivity1"
    intent.setComponent(ComponentName("com.knox.babypluginframework", activityName))
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK)

    Log.d(TAG, "startPluginTestActivity1 baseContext=${context.hashCode()}")
    context.startActivity(intent)

    /*
     * Issue:
     * pluginApk中com.knox.pluginapk.TestActivity1使用的layout
     * 与com.knox.babypluginframework.hookactivity.ProxyActivity使用的layout发生了id冲突
     * 导致启动了TestActivity1, 却看到ProxyActivity的界面
     *
     * Closed:
     *
     */
}

/**
 * 将插件的 DEX 动态添加到宿主的 ClassLoader
 */
private fun mergeDexToHostClassLoader(context: Context, pluginApkFile: File) {
    try {
        // 获取当前的 PathClassLoader
        val hostClassLoader = context.classLoader as PathClassLoader

        // 反射获取 BaseDexClassLoader 的 pathList 字段
        val pathListField = PathClassLoader::class.java.superclass
            ?.getDeclaredField("pathList")
        pathListField?.isAccessible = true
        val pathList = pathListField?.get(hostClassLoader)

        // 反射获取 DexPathList 的 dexElements 字段
        val dexElementsField = pathList?.javaClass?.getDeclaredField("dexElements")
        dexElementsField?.isAccessible = true
        val oldDexElements = dexElementsField?.get(pathList) as Array<*>

        // 创建插件的临时 ClassLoader 来加载插件 DEX
        val optimizedDirectory = context.getDir("dex_opt", Context.MODE_PRIVATE).absolutePath
        val pluginClassLoader = DexClassLoader(
            pluginApkFile.absolutePath,
            optimizedDirectory,
            null,
            hostClassLoader
        )

        // 获取插件 ClassLoader 的 pathList
        val pluginPathListField = DexClassLoader::class.java.superclass
            ?.getDeclaredField("pathList")
        pluginPathListField?.isAccessible = true
        val pluginPathList = pluginPathListField?.get(pluginClassLoader)

        // 获取插件的 dexElements
        val pluginDexElementsField = pluginPathList?.javaClass?.getDeclaredField("dexElements")
        pluginDexElementsField?.isAccessible = true
        val pluginDexElements = pluginDexElementsField?.get(pluginPathList) as Array<*>

        // 合并两个 dexElements 数组
        val newDexElementsLength = oldDexElements.size + pluginDexElements.size
        val newDexElementsClass = oldDexElements.javaClass
        val newDexElements = java.lang.reflect.Array.newInstance(
            newDexElementsClass.componentType,
            newDexElementsLength
        ) as Array<*>

        // 先放入插件的 dexElements（优先加载插件中的类）
        System.arraycopy(pluginDexElements, 0, newDexElements, 0, pluginDexElements.size)
        // 再放入原来的 dexElements
        System.arraycopy(
            oldDexElements,
            0,
            newDexElements,
            pluginDexElements.size,
            oldDexElements.size
        )

        // 将新的 dexElements 设置回宿主的 pathList
        dexElementsField.set(pathList, newDexElements)

        Log.d(TAG, "Successfully merged plugin DEX to host ClassLoader")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to merge DEX", e)
        throw RuntimeException("Failed to merge plugin DEX to host ClassLoader", e)
    }
}

/**
 * 加载插件资源
 */
private fun loadPluginResources(context: Context, pluginApkFile: File): Resources {
    try {
        val assetManager = AssetManager::class.java.newInstance()
        val addAssetPathMethod =
            AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
        addAssetPathMethod.isAccessible = true

        // !!!添加host的资源!!!
        addAssetPathMethod.invoke(assetManager, context.packageResourcePath)
        // !!!添加plugin的资源!!!
        addAssetPathMethod.invoke(assetManager, pluginApkFile.absolutePath)

        // 创建插件 Resources 对象
        return Resources(
            assetManager,
            context.resources.displayMetrics,
            context.resources.configuration
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load plugin resources", e)
        throw RuntimeException("Failed to load plugin resources", e)
    }
}