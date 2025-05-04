package com.knox.babypluginframework.hookactivity

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

private const val TAG = "PackageHook"

/**
 * Hook ActivityThread 的 sPackageManager 字段
 */
fun hookPackageManager(context: Context) {
    try {
        // 1. 获取 ActivityThread 类
        // ActivityThread 是应用程序的主线程，通过反射其静态方法 currentActivityThread() 获取
        val activityThreadClass = Class.forName("android.app.ActivityThread")

        // 2. 获取 currentActivityThread 方法
        val currentActivityThreadMethod =
            activityThreadClass.getDeclaredMethod("currentActivityThread")
        currentActivityThreadMethod.isAccessible = true
        val currentActivityThread = currentActivityThreadMethod.invoke(null)

        // 3. 获取 sPackageManager 字段
        val sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager")
        sPackageManagerField.isAccessible = true

        // 4. 获取原始的 sPackageManager 对象
        val originalPackageManager = sPackageManagerField.get(null)

        // 5. 获取 IPackageManager 接口类
        val iPackageManagerClass = Class.forName("android.content.pm.IPackageManager")

        // 6. 创建动态代理
        val packageManagerProxy = Proxy.newProxyInstance(
            context.classLoader,
            arrayOf(iPackageManagerClass),
            object : InvocationHandler {
                override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                    // 拦截 getActivityInfo 方法
                    if (method.name == "getActivityInfo" && args != null && args.isNotEmpty()) {
                        val componentName = args[0] as ComponentName
                        Log.d(TAG, "拦截 getActivityInfo: ${componentName.className}")

                        // 如果是我们要处理的目标插件 Activity
                        if (componentName.className.contains("TargetActivity")) {
                            try {
                                // 先尝试原始调用
//                                return method.invoke(
//                                    originalPackageManager,
//                                    *(args ?: emptyArray())
//                                )
                                return createFakeActivityInfo(context, componentName)
                            } catch (e: InvocationTargetException) {
                                Log.d(TAG, "getActivityInfo 发生异常, message=${e.message}, targetMessage=${e.targetException.message}")
                                // 代码中捕获不到 NameNotFoundException, 但日志会印
                                if (e.targetException is PackageManager.NameNotFoundException) {
                                    Log.d(
                                        TAG,
                                        "为 ${componentName.className} 创建伪造的 ActivityInfo"
                                    )

                                    // 创建一个伪造的 ActivityInfo
                                    return createFakeActivityInfo(context, componentName)
                                }
                                throw e.targetException
                            }
                        }
                    }

                    // 其他方法直接转发给原始对象
                    return try {
                        method.invoke(originalPackageManager, *(args ?: emptyArray()))
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                }
            }
        )

        // 7. 替换 ActivityThread 的 sPackageManager 字段
        sPackageManagerField.set(null, packageManagerProxy)

        // 8. 替换 ApplicationPackageManager 的 mPM 字段
        val pmField = context.packageManager.javaClass.getDeclaredField("mPM")
        pmField.isAccessible = true
        pmField.set(context.packageManager, packageManagerProxy)

        Log.d(TAG, "成功 Hook PackageManager")

    } catch (e: Exception) {
        Log.e(TAG, "Hook PackageManager 失败", e)
        e.printStackTrace()
    }
}

/**
 * 创建伪造的 ActivityInfo
 */
private fun createFakeActivityInfo(context: Context, componentName: ComponentName): ActivityInfo {
    // 获取一个已存在的 Activity 的信息作为模板
    val stubActivityInfo = context.packageManager.getActivityInfo(
        ComponentName(context.packageName, "com.knox.babypluginframework.hookactivity.ProxyActivity"),
        PackageManager.GET_META_DATA
    )

    // 创建并配置新的 ActivityInfo
    return ActivityInfo().apply {
        applicationInfo = stubActivityInfo.applicationInfo
        packageName = componentName.packageName
        name = componentName.className
        theme = stubActivityInfo.theme
        launchMode = ActivityInfo.LAUNCH_MULTIPLE
        exported = true
        flags = stubActivityInfo.flags
        metaData = stubActivityInfo.metaData
    }
}