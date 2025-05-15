package com.knox.babypluginframework.hookactivity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.knox.babypluginframework.PluginManager
import java.lang.reflect.Method

// /Users/knox/Library/Android/sdk/sources/android-30/android/app
private const val TAG = "ProxyActivity"

class ProxyActivity : AppCompatActivity() {

    private var pluginActivity: Activity? = null
    private var pluginClassLoader: ClassLoader? = null
    private var pluginResources: Resources? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取目标Activity类名
        val targetActivityName = ActivityHook.getOriginalTargetActivity(intent)
        Log.d(
            TAG,
            "Proxy onCreate, intent=${intent.hashCode()}, targetActivityName=${
                intent.getStringExtra("target_activity")
            }"
        )

        if (targetActivityName != null) {
            Log.d(TAG, "代理 $targetActivityName")

            try {
                // 1. 获取插件ClassLoader
                pluginClassLoader = PluginManager.getPluginClassLoader()
                    ?: throw IllegalStateException("插件ClassLoader未初始化，请先调用PluginManager.loadPlugin")

                pluginResources = PluginManager.getPluginResources()

                // 2. 加载插件Activity类
                val targetActivityClass = pluginClassLoader!!.loadClass(targetActivityName)

                // 3. 创建目标Activity实例
                val targetActivity = targetActivityClass.newInstance() as Activity

                Log.d(TAG, "尝试加载插件Activity: $targetActivityName")

                // 4. 反射获取 Activity.attach 方法
                val attachMethod = when (Build.VERSION.SDK_INT) {
                    30 -> attachMethod30()
                    35 -> attachMethod35()
                    else -> attachMethod35()
                }

                attachMethod.isAccessible = true

                // 5. 获取必要的参数
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentActivityThreadMethod =
                    activityThreadClass.getDeclaredMethod("currentActivityThread")
                currentActivityThreadMethod.isAccessible = true
                val activityThread = currentActivityThreadMethod.invoke(null)

                val instrumentationField = activityThreadClass.getDeclaredField("mInstrumentation")
                instrumentationField.isAccessible = true
                val instrumentation = instrumentationField.get(activityThread)

                /*
                 * Activity的mInstrumentation和ActivityThread的mInstrumentation是同一个吗?
                 */
                val instrumentationFieldInActivity =
                    Activity::class.java.getDeclaredField("mInstrumentation")
                instrumentationFieldInActivity.isAccessible = true
                val instrumentationInActivity = instrumentationFieldInActivity.get(this)
                Log.d(
                    TAG,
                    "Check instrumentationInActivityThread=${instrumentation.hashCode()}, instrumentationInActivity=${instrumentationInActivity.hashCode()}"
                )

                // 获取当前Activity的mToken
                val tokenField = Activity::class.java.getDeclaredField("mToken")
                tokenField.isAccessible = true
                val token = tokenField.get(this)

                // 获取当前Activity的mActivityInfo
                val activityInfoField = Activity::class.java.getDeclaredField("mActivityInfo")
                activityInfoField.isAccessible = true
                val activityInfo = activityInfoField.get(this)

                // 获取当前Activity的mCurrentConfig
                val currentConfigField = Activity::class.java.getDeclaredField("mCurrentConfig")
                currentConfigField.isAccessible = true
                val currentConfig = currentConfigField.get(this)

                // 创建参数数组
                val params = arrayOfNulls<Any>(18)
                params[0] = this // context
                params[1] = activityThread
                params[2] = instrumentation
                params[3] = token
                params[4] = 0 // ident
                params[5] = application
                params[6] = ActivityHook.getOriginalIntent(intent) ?: intent
                params[7] = activityInfo
                params[12] = currentConfig
                // 其余参数留空或默认值

                // 6. 调用attach方法
                attachMethod.invoke(targetActivity, *params)
                Log.d(TAG, "attachMethod invoke End, targetActivity=$targetActivity")

                // 7. 调用目标Activity的onCreate
                val onCreateMethod = Activity::class.java.getDeclaredMethod(
                    "onCreate",
                    Bundle::class.java
                )
                onCreateMethod.isAccessible = true
                Log.d(TAG, "onCreateMethod invoke Begin, targetActivity=$targetActivity")
                onCreateMethod.invoke(targetActivity, savedInstanceState)

                // 8. 将目标Activity的布局设置到代理Activity
                val decorView = targetActivity.window.decorView
                setContentView(decorView)

                Log.d(TAG, "成功代理 $targetActivityName")
            } catch (e: Exception) {
                Log.e(TAG, "代理失败", e)

                // 显示错误信息
                setContentView(TextView(this).apply {
                    text = "代理失败(SDK=${Build.VERSION.SDK_INT})：${e.message}"
                    setPadding(50, 50, 50, 50)
                })
            }
        } else {
            // 没有目标，显示默认内容
            setContentView(TextView(this).apply {
                text = "未指定目标Activity"
                setPadding(50, 50, 50, 50)
            })
        }
    }

    override fun getResources(): Resources {
        return pluginResources ?: super.getResources()
    }

    // 重写生命周期方法，传递给目标Activity
    // ...
}

private fun attachMethod30(): Method {
    return Activity::class.java.getDeclaredMethod(
        "attach",
        Context::class.java,
        Class.forName("android.app.ActivityThread"),
        Class.forName("android.app.Instrumentation"),
        IBinder::class.java,
        Int::class.javaPrimitiveType,
        Application::class.java,
        Intent::class.java,
        Class.forName("android.content.pm.ActivityInfo"),
        CharSequence::class.java,
        Activity::class.java,
        String::class.java,
        Class.forName("android.app.Activity\$NonConfigurationInstances"),
        Class.forName("android.content.res.Configuration"),
        String::class.java,
        Class.forName("com.android.internal.app.IVoiceInteractor"),
        Class.forName("android.view.Window"),
        Class.forName("android.view.ViewRootImpl\$ActivityConfigCallback"),
        IBinder::class.java
    )
}

private fun attachMethod35(): Method {
    return Activity::class.java.getDeclaredMethod(
        "attach",
        Context::class.java,
        Class.forName("android.app.ActivityThread"),
        Class.forName("android.app.Instrumentation"),
        IBinder::class.java,
        Int::class.javaPrimitiveType,
        Application::class.java,
        Intent::class.java,
        Class.forName("android.content.pm.ActivityInfo"),
        CharSequence::class.java,
        Activity::class.java,
        String::class.java,
        Class.forName("android.app.Activity\$NonConfigurationInstances"),
        Class.forName("android.content.res.Configuration"),
        String::class.java,
        Class.forName("com.android.internal.app.IVoiceInteractor"),
        Class.forName("android.view.Window"),
        Class.forName("android.view.ViewRootImpl\$ActivityConfigCallback"),
        IBinder::class.java,
        IBinder::class.java
    )
}