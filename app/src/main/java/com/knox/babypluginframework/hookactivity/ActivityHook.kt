package com.knox.babypluginframework.hookactivity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import java.lang.reflect.Proxy

private const val TAG = "ActivityHook"

/**
 * ActivityHook 工具类
 */
public object ActivityHook {
    /**
     * 安装 Activity Hook
     * @param context 上下文
     * @param proxyActivityClass 代理Activity类
     */
    fun install(context: Context, proxyActivityClass: Class<*>) {
        try {
            // 1. 优先尝试 Hook Instrumentation
            hookInstrumentation(context, proxyActivityClass) // NG

            // 2. 备选方案：根据系统版本 Hook ActivityManager 或 ActivityTaskManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hookActivityTaskManager(context, proxyActivityClass) // OK
            } else {
                hookActivityManager(context, proxyActivityClass)
            }

            Log.d(TAG, "Activity Hook 安装完成")
        } catch (e: Exception) {
            Log.e(TAG, "安装Activity Hook失败", e)
        }
    }

    // 实现前面的 hookInstrumentation, hookActivityManager, hookActivityTaskManager 方法
    // ...

    /**
     * 配置需要替换的Activity列表
     */
    private val targetActivities = mutableSetOf<String>()

    /**
     * 添加需要替换的目标Activity
     */
    fun addTargetActivity(activityClassName: String) {
        targetActivities.add(activityClassName)
        Log.d(TAG, "添加目标Activity: $activityClassName")
    }

    /**
     * 判断是否需要替换指定Activity
     */
    private fun needReplaceActivity(activityName: String): Boolean {
        return targetActivities.contains(activityName) ||
            activityName.startsWith("com.knox.babypluginframework.hookactivity.")
    }

    /**
     * 根据Intent获取原始目标Activity
     */
    fun getOriginalTargetActivity(intent: Intent): String? {
        return intent.getStringExtra("target_activity")
    }

    /**
     * 获取原始Intent
     */
    fun getOriginalIntent(intent: Intent): Intent? {
        return intent.getParcelableExtra("original_intent")
    }
}

/**
 * Hook Instrumentation 来拦截 startActivity
 */
fun hookInstrumentation(context: Context, proxyActivityClass: Class<*>) {
    try {
        // 1. 获取 ActivityThread 的 currentActivityThread 方法
        /*
         * frameworks/base/core/java/android/app/ActivityThread.java
         *
         * @UnsupportedAppUsage
         * public static ActivityThread currentActivityThread() {
         *     return sCurrentActivityThread;
         * }
         */
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentActivityThreadMethod =
            activityThreadClass.getDeclaredMethod("currentActivityThread")
        currentActivityThreadMethod.isAccessible = true
        val currentActivityThread = currentActivityThreadMethod.invoke(null)

        // 2. 获取 ActivityThread 的 mInstrumentation 字段
        /*
         * frameworks/base/core/java/android/app/ActivityThread.java
         *
         * @UnsupportedAppUsage
         * Instrumentation mInstrumentation;
         */
        val mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation")
        mInstrumentationField.isAccessible = true
        val mInstrumentation = mInstrumentationField.get(currentActivityThread) as Instrumentation

        // 3. 创建自定义 Instrumentation
        val customInstrumentation = object : Instrumentation() {
            // sdk-35 没有这支api了
            fun execStartActivity(
                context: Context,
                who: IBinder?,
                contextThread: Int,
                token: IBinder?,
                target: Activity?,
                intent: Intent,
                requestCode: Int,
                options: Bundle?
            ): ActivityResult? {
                val originalComponent = intent.component

                if (originalComponent != null) {
                    val targetActivityName = originalComponent.className
                    if (needReplaceActivity(targetActivityName)) {
                        // 创建代理 Intent
                        val proxyIntent = Intent(context, proxyActivityClass)
                        // 保存原始 Intent
                        proxyIntent.putExtra("original_intent", intent)
                        // 保存原始目标类名
                        proxyIntent.putExtra("target_activity", targetActivityName)

                        Log.d(
                            "ActivityHook",
                            "Instrumentation拦截: $targetActivityName -> $proxyActivityClass"
                        )

                        // 替换为代理 Intent
                        intent.component = ComponentName(context, proxyActivityClass)
                    }
                }

                // 调用原始的 execStartActivity 方法
                try {
                    val method = Instrumentation::class.java.getDeclaredMethod(
                        "execStartActivity",
                        Context::class.java,
                        IBinder::class.java,
                        Int::class.javaPrimitiveType,
                        IBinder::class.java,
                        Activity::class.java,
                        Intent::class.java,
                        Int::class.javaPrimitiveType,
                        Bundle::class.java
                    )
                    method.isAccessible = true
                    return method.invoke(
                        mInstrumentation,
                        context, who, contextThread, token, target, intent, requestCode, options
                    ) as? ActivityResult
                } catch (e: Exception) {
                    Log.e("ActivityHook", "调用原始execStartActivity失败", e)
                }

                return null
            }

            override fun startActivitySync(intent: Intent, options: Bundle?): Activity {
                val originalComponent = intent.component
                Log.d(
                    TAG,
                    "hookInstrumentation startActivitySync, originalComponent=${originalComponent?.className}"
                )
                return mInstrumentation.startActivitySync(intent, options)
            }

            // 重写 newActivity 方法，用于恢复原始 Intent
            override fun newActivity(
                cl: ClassLoader, className: String,
                intent: Intent
            ): Activity {
                val originalIntent = intent.getParcelableExtra<Intent>("original_intent")
                return if (originalIntent != null) {
                    Log.d(TAG, "hookInstrumentation newActivity, 使用原始Intent创建Activity")
                    // 调用原始方法，但使用原始Intent
                    mInstrumentation.newActivity(cl, className, originalIntent)
                } else {
                    Log.d(
                        TAG,
                        "hookInstrumentation newActivity, 使用当前Intent创建Activity: $className"
                    )
                    mInstrumentation.newActivity(cl, className, intent)
                }
            }
        }

        // 4. 替换 ActivityThread 中的 mInstrumentation
        mInstrumentationField.set(currentActivityThread, customInstrumentation)

        Log.d(TAG, "成功Hook Instrumentation")
    } catch (e: Exception) {
        Log.e(TAG, "Hook Instrumentation 失败", e)
    }
}

/**
 * Hook ActivityTaskManager (Android 10.0+)
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun hookActivityTaskManager(context: Context, proxyActivityClass: Class<*>) {
    try {
        // 1. 获取 ActivityTaskManager 中的 IActivityTaskManagerSingleton
        /*
         * frameworks/base/core/java/android/app/ActivityTaskManager.java
         *
         * @UnsupportedAppUsage(trackingBug = 129726065)
         * private static final Singleton<IActivityTaskManager> IActivityTaskManagerSingleton =
         *  new Singleton<IActivityTaskManager>() {
         *      @Override
         *      protected IActivityTaskManager create() {
         *          final IBinder b = ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE);
         *          return IActivityTaskManager.Stub.asInterface(b);
         *      }
         *  };
         */
        val activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager")
        val singletonField =
            activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton")
        singletonField.isAccessible = true
        val singleton = singletonField.get(null)

        // 2. 获取 Singleton 的 mInstance 字段
        /*
         * frameworks/base/core/java/android/util/Singleton.java
         *
         * @UnsupportedAppUsage
         * private T mInstance;
         */
        val singletonClass = Class.forName("android.util.Singleton")
        val mInstanceField = singletonClass.getDeclaredField("mInstance")
        mInstanceField.isAccessible = true
        val mInstance = mInstanceField.get(singleton)
        Log.d(TAG, "hookActivityTaskManager, singleton=${singleton}, mInstance=${mInstance}")

        // 3. 创建动态代理
        val iActivityTaskManagerClass = Class.forName("android.app.IActivityTaskManager")
        val proxy = Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(iActivityTaskManagerClass)
        ) { proxy, method, args ->
            // 拦截 startActivity 方法
            if (method.name == "startActivity") {
                Log.d(TAG, "hookActivityTaskManager startActivity, 拦截成功")
                Log.d(TAG, "hookActivityTaskManager startActivity, AOP, proxy=${proxy?.hashCode()}, method=${method?.hashCode()}, args=${args?.hashCode()}")
            }

            if (method.name == "startActivity") {
                // 查找 Intent 参数的位置
                var intentIndex = -1
                for (i in args.indices) {
                    if (args[i] is Intent) {
                        intentIndex = i
                        break
                    }
                }

                if (intentIndex != -1) {
                    // 获取原始 Intent
                    val originalIntent = args[intentIndex] as Intent
                    val originalComponent = originalIntent.component

                    if (originalComponent != null) {
                        // 检查是否需要替换
                        val targetActivityName = originalComponent.className
                        if (needReplaceActivity(targetActivityName)) {
                            // 创建代理 Intent
                            val proxyIntent = Intent(context, proxyActivityClass)
                            // 保存原始 Intent
                            proxyIntent.putExtra("original_intent", originalIntent)
                            // 保存原始目标类名
                            proxyIntent.putExtra("target_activity", targetActivityName)

                            // 替换原始 Intent
                            args[intentIndex] = proxyIntent

                            Log.d(TAG, "拦截到startActivity，目标: $targetActivityName，已替换为代理")
                        }
                    }
                }
            }

            // 调用原始方法
//            method.invoke(mInstance, *args)
            // 调用原始方法时也需要处理 null 情况
            if (args == null) {
                method.invoke(mInstance)  // 无参数调用
            } else {
                method.invoke(mInstance, *args)  // 有参数调用
            }
        }

        // 4. 将代理对象设置回 Singleton 中
        mInstanceField.set(singleton, proxy)

        Log.d(TAG, "成功Hook ActivityTaskManager")
    } catch (e: Exception) {
        Log.e(TAG, "Hook ActivityTaskManager 失败", e)
    }
}

/**
 * Hook ActivityManager 来拦截 startActivity 调用 (Android 9.0 及以下)
 */
fun hookActivityManager(context: Context, proxyActivityClass: Class<*>) {
    try {
        // 1. 获取 ActivityManager 中的 IActivityManagerSingleton 字段
        val activityManagerClass = Class.forName("android.app.ActivityManager")

        // 获取 IActivityManagerSingleton 字段名
        // Android 8.0 以下使用 "gDefault"，8.0+ 使用 "IActivityManagerSingleton"
        val fieldName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "IActivityManagerSingleton"
        } else {
            "gDefault"
        }

        val singletonField = activityManagerClass.getDeclaredField(fieldName)
        singletonField.isAccessible = true
        val singleton = singletonField.get(null)

        // 2. 获取 Singleton 的 mInstance 字段 (即 IActivityManager 实例)
        val singletonClass = Class.forName("android.util.Singleton")
        val mInstanceField = singletonClass.getDeclaredField("mInstance")
        mInstanceField.isAccessible = true
        val mInstance = mInstanceField.get(singleton)

        // 3. 创建动态代理
        val iActivityManagerClass = Class.forName("android.app.IActivityManager")
        val proxy = Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(iActivityManagerClass)
        ) { proxy, method, args ->
            // 拦截 startActivity 方法
            if (method.name == "startActivity") {
                // 查找 Intent 参数的位置
                var intentIndex = -1
                for (i in args.indices) {
                    if (args[i] is Intent) {
                        intentIndex = i
                        break
                    }
                }

                if (intentIndex != -1) {
                    // 获取原始 Intent
                    val originalIntent = args[intentIndex] as Intent
                    val originalComponent = originalIntent.component

                    if (originalComponent != null) {
                        // 检查是否需要替换
                        val targetActivityName = originalComponent.className
                        if (needReplaceActivity(targetActivityName)) {
                            // 创建代理 Intent
                            val proxyIntent = Intent(context, proxyActivityClass)
                            // 保存原始 Intent
                            proxyIntent.putExtra("original_intent", originalIntent)
                            // 保存原始目标类名
                            proxyIntent.putExtra("target_activity", targetActivityName)

                            // 替换原始 Intent
                            args[intentIndex] = proxyIntent

                            Log.d(
                                "ActivityHook",
                                "拦截到startActivity，目标: $targetActivityName，已替换为代理"
                            )
                        }
                    }
                }
            }

            // 调用原始方法
            method.invoke(mInstance, *args)
        }

        // 4. 将代理对象设置回 Singleton 中
        mInstanceField.set(singleton, proxy)

        Log.d("ActivityHook", "成功Hook ActivityManager")
    } catch (e: Exception) {
        Log.e("ActivityHook", "Hook ActivityManager 失败", e)
    }
}

/**
 * 判断是否需要替换指定Activity
 */
private fun needReplaceActivity(activityName: String): Boolean {
    // 根据需求实现，例如检查是否为插件中的Activity
    return activityName.startsWith("com.knox.babypluginframework.hookactivity.")
}