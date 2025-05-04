package com.knox.babypluginframework

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File

/**
 * 插件管理类
 */
object PluginManager {
    private var dexClassLoader: DexClassLoader? = null
    private var pluginResources: Resources? = null
    private var pluginContext: Context? = null

    /**
     * 加载插件APK
     */
    fun loadPlugin(context: Context, pluginApkFile: File) {
        try {
            // 1. 创建插件的DexClassLoader
            val optimizedDir = context.getDir("dex", Context.MODE_PRIVATE).absolutePath
            dexClassLoader = DexClassLoader(
                pluginApkFile.absolutePath,  // APK路径
                optimizedDir,                // 优化后的dex存放路径
                null,                        // 本地库搜索路径
                context.classLoader          // 父ClassLoader
            )

            // 2. 创建插件的Resources
            val assetManager = AssetManager::class.java.newInstance()
            val addAssetPathMethod = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
            addAssetPathMethod.isAccessible = true
            addAssetPathMethod.invoke(assetManager, pluginApkFile.absolutePath)

            pluginResources = Resources(
                assetManager,
                context.resources.displayMetrics,
                context.resources.configuration
            )

            // 3. 创建用于插件的Context
            val pluginPackageInfo = context.packageManager.getPackageArchiveInfo(
                pluginApkFile.absolutePath, PackageManager.GET_ACTIVITIES
            )

            val pluginPackageName = pluginPackageInfo?.packageName ?: "com.knox.pluginapk"
            val contextImpl = Class.forName("android.app.ContextImpl")
            val createPackageContextMethod = contextImpl.getDeclaredMethod(
                "createPackageContext", String::class.java, Int::class.javaPrimitiveType
            )
            createPackageContextMethod.isAccessible = true

            val baseContext = context.createPackageContext(
                context.packageName, Context.CONTEXT_INCLUDE_CODE
            )

            Log.d("PluginManager", "成功加载插件: ${pluginApkFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("PluginManager", "加载插件失败", e)
        }
    }

    // 获取插件ClassLoader
    fun getPluginClassLoader(): ClassLoader? = dexClassLoader

    // 获取插件Resources
    fun getPluginResources(): Resources? = pluginResources

    // 获取插件Context
    fun getPluginContext(): Context? = pluginContext
}