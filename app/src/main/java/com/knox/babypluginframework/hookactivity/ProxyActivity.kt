package com.knox.babypluginframework.hookactivity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// /Users/knox/Library/Android/sdk/sources/android-30/android/app
private const val TAG = "ProxyActivity"

class ProxyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取目标Activity类名
        val targetActivityName = ActivityHook.getOriginalTargetActivity(intent)

        if (targetActivityName != null) {
            Log.d(TAG, "代理 $targetActivityName")
            Log.d(TAG, "该设备sdk=${Build.VERSION.SDK_INT}")

            try {
                // 加载目标Activity类
                val targetActivityClass = Class.forName(targetActivityName)

                // 创建目标Activity实例
                val targetActivity = targetActivityClass.newInstance() as Activity

                // 反射获取 Activity.attach 方法
                // 35
//                val attachMethod35 = Activity::class.java.getDeclaredMethod(
//                    "attach",
//                    Context::class.java,
//                    Class.forName("android.app.ActivityThread"),
//                    Class.forName("android.app.Instrumentation"),
//                    IBinder::class.java,
//                    Int::class.javaPrimitiveType,
//                    Application::class.java,
//                    Intent::class.java,
//                    Class.forName("android.content.pm.ActivityInfo"),
//                    CharSequence::class.java,
//                    Activity::class.java,
//                    String::class.java,
//                    Class.forName("android.app.Activity\$NonConfigurationInstances"),
//                    Class.forName("android.content.res.Configuration"),
//                    String::class.java,
//                    Class.forName("com.android.internal.app.IVoiceInteractor"),
//                    Class.forName("android.view.Window"),
//                    Class.forName("android.view.ViewRootImpl\$ActivityConfigCallback"),
//                    IBinder::class.java,
//                    IBinder::class.java,
//                    IBinder::class.java
//                )

                //30
                val attachMethod = Activity::class.java.getDeclaredMethod(
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

                attachMethod.isAccessible = true

                // 获取必要的参数
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentActivityThreadMethod =
                    activityThreadClass.getDeclaredMethod("currentActivityThread")
                currentActivityThreadMethod.isAccessible = true
                val activityThread = currentActivityThreadMethod.invoke(null)

                val instrumentationField = activityThreadClass.getDeclaredField("mInstrumentation")
                instrumentationField.isAccessible = true
                val instrumentation = instrumentationField.get(activityThread)

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

                // 调用attach方法
                attachMethod.invoke(targetActivity, *params)

                // 调用目标Activity的onCreate
                val onCreateMethod = Activity::class.java.getDeclaredMethod(
                    "onCreate",
                    Bundle::class.java
                )
                onCreateMethod.isAccessible = true
                onCreateMethod.invoke(targetActivity, savedInstanceState)

                // 将目标Activity的布局设置到代理Activity
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

    // 重写生命周期方法，传递给目标Activity
    // ...
}