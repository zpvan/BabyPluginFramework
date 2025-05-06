package com.knox.babypluginframework

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.knox.pluginlibrary.IPlugin
import dalvik.system.PathClassLoader
import java.io.File

/**
 * Android资源文件分为两类:
 *
 * 第一类是res目录下存放的可编译的资源文件
 * 访问这种资源使用Context的getResources方法, 得到Resources对象, 进而通过Resources的getXXX方法得到各种资源
 * ```kotlin
 * val resources = context.resources
 * val appName = resources.getString(R.string.app_name)
 * ```
 *
 * 第二类是assets目录下存放的原始资源文件
 * 不能通过R.xx的方式访问它们, 也不能通过该资源的绝对路径访问(因为apk下载后不会解压到本地)
 * 只能借助AssetManager类的open方法来获取assets目录下的文件资源了, AssetManager又来源于Resources类的getAssets方法
 * ```kotlin
 * val resources = context.resources
 * val am = resources.assets
 * val inputStream = am.open("filename")
 * ```
 *
 */

private const val TAG = "ResourceExt"

internal fun loadPluginThenGetRes(context: Context, pluginApkFile: File): String? {
    try {
        // 1. 获取插件的Resources对象
        // 通过反射, 创建AssetManager对象, 调用addAssetPath方法, 把pluginApkFile的路径添加到这个AssetManager对象中
        val assetManager = AssetManager::class.java.newInstance()
        val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
        addAssetPath.invoke(assetManager, pluginApkFile.absolutePath)
        // Issue: 能创建出只属于plugin的Resources对象, 如何能创建只属于plugin的Context呢?
        val pluginResources = Resources(assetManager,
            context.resources.displayMetrics,
            context.resources.configuration)

        // 2. 创建类加载器
        val dexClassLoader = PathClassLoader(
            pluginApkFile.absolutePath,
            null,
            context.classLoader
        )

        // 3. 加载插件中的BabyPlugin类
        val pluginClass = dexClassLoader.loadClass("com.knox.pluginapk.BabyPlugin")

        // 4. 创建实例 (假设有无参构造函数或默认参数)
        val constructor = pluginClass.getDeclaredConstructor()
        // 5. 强转成接口
        val babyPlugin = constructor.newInstance() as IPlugin

        // 6. 获取Resource
        val res = babyPlugin.getStringForResId(pluginResources)

        Log.d(TAG, "BabyPlugin res=$res")
        return res
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}