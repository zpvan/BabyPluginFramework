针对于Activity的插件化解决方案有很多种，大致分为两个方向：

* 以张勇的**DroidPlugin框架**为代表的**动态替换**方案，提供对Android底层的各种类进行Hook，来达到加载插件中的四大组件的目的。
* 以任玉刚的**that框架**为代表的**静态代理**方案，通过ProxyActivity统一加载插件中的所有Activity。



宿主App如何能加载到插件中的类，大致有如下三种方案：

* 为每个插件创建对应的ClassLoader
* 把所有dex合并到一个数组中
* 直接把系统的ClassLoader替换为MyClassLoader



# 插件ClassLoader的经典示例

## 调用插件类方法

1. new出PluginApk中的类的实例，并且调用其中的方法

```kotlin
import android.content.Context
import android.util.Log
import com.knox.pluginlibrary.IPlugin
import dalvik.system.PathClassLoader
import java.io.File

private const val TAG = "HostExt"

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
```

## 获取插件的资源

1. 创建AssetManager对象并调用addAssetPath方法来加载pluginApkFile。

2. 再创建Resources对象来包装assetManager，通过调用resources.getString(R.string.XXX)来获取插件的资源。

```kotlin
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.knox.pluginlibrary.IPlugin
import dalvik.system.PathClassLoader
import java.io.File

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

// PluginApk的代码
package com.knox.pluginapk

class BabyPlugin : IPlugin {
  
    override val name: String
        get() = "BabyPlugin"

    @OptIn(ExperimentalStdlibApi::class)
    override fun getStringForResId(resources: Resources): String {
        return resources.getString(R.string.hello_resources).also {
            Log.d(TAG, "getStringForResId(${R.string.hello_resources.toHexString()})=$it")
        }
    }
}
```

## 启动插件定义的Service（初级）

1. 合并所有插件的dex，来解决插件的类的加载问题。

2. 预先在宿主的AndroidManifest文件中声明插件中的四大组件。

3. 使用被修改过ClassLoader的context来startService

```kotlin
import android.content.Context
import android.content.Intent
import android.util.Log
import com.knox.pluginlibrary.IPlugin
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import java.io.File

internal fun startPluginTestService1(context: Context, pluginApkFile: File) {
    // 1. 合并 DEX 到宿主 ClassLoader
    mergeDexToHostClassLoader(context, pluginApkFile)

    // 2. 启动插件服务
    val intent = Intent()
    val serviceName = "com.knox.pluginapk.TestService1"
    intent.setClassName(context, serviceName)
    context.startService(intent)
}

fun mergeDexToHostClassLoader(context: Context, pluginApkFile: File) {
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
        System.arraycopy(oldDexElements, 0, newDexElements, pluginDexElements.size, oldDexElements.size)

        // 将新的 dexElements 设置回宿主的 pathList
        dexElementsField.set(pathList, newDexElements)

        Log.d(TAG, "Successfully merged plugin DEX to host ClassLoader")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to merge DEX", e)
        throw RuntimeException("Failed to merge plugin DEX to host ClassLoader", e)
    }
}
```

## 启动插件定义的Activity（初级）

1. 合并所有插件的dex，来解决插件的类的加载问题。
2. 预先在宿主的AndroidManifest文件中声明插件中的四大组件。当然，这样做对于插件中上百个Activity是件很麻烦的事情。
3. 把插件中的所有资源一次性地合并到宿主的资源中。当然，这可能会导致资源id冲突。

```kotlin
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
}

private fun loadPluginResources(context: Context, pluginApkFile: File): Resources {
    try {
        val assetManager = AssetManager::class.java.newInstance()
        val addAssetPathMethod = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
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
        Log.e("PluginLoader", "Failed to load plugin resources", e)
        throw RuntimeException("Failed to load plugin resources", e)
    }
}

// PluginLibrary的代码
package com.knox.pluginlibrary

interface IPlugin {
    companion object {
        var bigResources : Resources? = null
    }
}

// PluginApk的代码
package com.knox.pluginapk

class TestActivity1 : AppCompatActivity() {

    override fun getResources(): Resources {
        Log.d(TAG, "getResources ${IPlugin.bigResources?.hashCode() ?: "NULL"}")
        return IPlugin.bigResources ?: super.getResources()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate baseContext=${this.baseContext.hashCode()}")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test1)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
```

## 启动插件定义的Activity（中级）

1. 相较于初级做法，该Activity不用在HostApk的Manifest中声明
2. HostApk需声明一个ProxyActivity，用来欺骗AMS，启动插件Apk中的各种Activity
3. Hook startActivity 的上半场，静态代理 hook Instrumentation 的 execStartActivity、startActivitySync 方法
4. ProxyActivity 在其生命周期回调函数中调用插件Activity对应的生命周期回调函数
5. Hook startActivity 的下半场，静态代理 hook Instrumentation 的 newActivity、callActivityOnCreate 方法
6. Hook PackageManager 处理各种找不到缓存的问题，因为插件Activity未在Manifest中声明，所以很多时候系统会因为找不到缓存而抛出异常（android.content.pm.PackageManager$NameNotFoundException）



### MyApplication.kt

```kotlin
class MyApplication : Application() {
  override fun onCreate() {
    // 安装Activity Hook
    ActivityHook.install(this, ProxyActivity::class.java)
    // 添加需要代理的目标Activity
    ActivityHook.addTargetActivity("com.knox.pluginapk.hookactivity.PluginTargetActivity")
    
    hookPackageManager(this)
  }
}
```

### ActivityHook.kt

```kotlin
object ActivityHook {
  fun install(context: Context, proxyActivityClass: Class<*>) {
        // 静态代理方案，通过ProxyActivity统一加载插件中的所有Activity
        Log.d(TAG, "该设备sdk=${Build.VERSION.SDK_INT}")

        try {
            // 1. 优先尝试 Hook Instrumentation
            hookInstrumentation(context, proxyActivityClass)

            // 2. 备选方案：根据系统版本 Hook ActivityManager 或 ActivityTaskManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hookActivityTaskManager(context, proxyActivityClass)
            } else {
                hookActivityManager(context, proxyActivityClass)
            }

            Log.d(TAG, "Activity Hook 安装完成")
        } catch (e: Exception) {
            Log.e(TAG, "安装Activity Hook失败", e)
        }
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
         *
         * Hook ActivityThread的mInstrumentation比Activity的mInstrumentation要好,
         * 虽然Activity的mInstrumentation都是由ActivityThread的mInstrumentation赋值的,
         * 但是ActivityThread是单例,Hook mInstrumentation只需要做一次只需要做一次, 而且必须是MainActivity启动前就要Hook
         * 如果Hook Activity的mInstrumentation, 那么就需要每个Activity的mInstrumentation都要Hook, 很麻烦
         */
        val mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation")
        mInstrumentationField.isAccessible = true
        val mInstrumentation = mInstrumentationField.get(currentActivityThread) as Instrumentation

        /*
         * 3. 创建自定义 Instrumentation
         *
         * 静态代理, new了一个customInstrumentation包住原mInstrumentation
         * 改写 execStartActivity, startActivitySync, newActivity 这3支api的原有逻辑
         * 其中
         * execStartActivity是Activity的startActivity和Context的startActivity的必经之路
         * Instrumentation.execStartActivity会调用ActivityTaskManager.getService().startActivity
         */
        val customInstrumentation = object : Instrumentation() {

            /*
             * frameworks/base/core/java/android/app/Instrumentation.java
             *
             * public ActivityResult execStartActivity(
             *     Context who, IBinder contextThread, IBinder token, Activity target,
             *     Intent intent, int requestCode, Bundle options)
             */
            fun execStartActivity(
                who: Context?,
                contextThread: IBinder?,
                token: IBinder?,
                target: Activity?,
                intent: Intent,
                requestCode: Int,
                options: Bundle?
            ): ActivityResult? {
                Log.d(TAG, "Evil Instrumentation execStartActivity")
                var newIntent = intent
                val originalComponent = intent.component

                if (originalComponent != null) {
                    val targetActivityName = originalComponent.className
                    if (ActivityHook.needReplaceActivity(targetActivityName)) {
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
//                        intent.component = ComponentName(context, proxyActivityClass)
                        newIntent = proxyIntent
                    }
                }
                Log.d(
                    TAG,
                    "hookInstrumentation 上半场, intent=${newIntent.hashCode()}, originalIntent=${intent.hashCode()}, targetActivityName=${
                        newIntent.getStringExtra("target_activity")
                    }"
                )

                // 调用原始的 execStartActivity 方法
                try {
                    val method = Instrumentation::class.java.getDeclaredMethod(
                        "execStartActivity",
                        Context::class.java,
                        IBinder::class.java,
                        IBinder::class.java,
                        Activity::class.java,
                        Intent::class.java,
                        Int::class.javaPrimitiveType,
                        Bundle::class.java
                    )
                    method.isAccessible = true
                    return method.invoke(
                        mInstrumentation,
                        who, contextThread, token, target, newIntent, requestCode, options
                    ) as? ActivityResult
                } catch (e: Exception) {
                    Log.e(TAG, "调用原始execStartActivity失败", e)
                }

                return null
            }

            override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?) {
                Log.d(
                    TAG,
                    "hookInstrumentation callActivityOnCreate, activity=${activity?.componentName}"
                )
                mInstrumentation.callActivityOnCreate(activity, icicle)
            }

            override fun callActivityOnCreate(
                activity: Activity?,
                icicle: Bundle?,
                persistentState: PersistableBundle?
            ) {
                Log.d(
                    TAG,
                    "hookInstrumentation callActivityOnCreate with persistentState, activity=${activity?.componentName}"
                )
                mInstrumentation.callActivityOnCreate(activity, icicle, persistentState)
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
                /*
                 * 如果没有插件化, 这里的cl对象就是宿主App原生的ClassLoader
                 * 如果是插件化, 也就是要加载插件中的Activity类, 这个cl对象就应该是插件对应的ClassLoader
                 */
                cl: ClassLoader,
                className: String,
                intent: Intent
            ): Activity {
                val originalIntent = intent.getParcelableExtra<Intent>("original_intent")
                Log.d(
                    TAG,
                    "hookInstrumentation 下半场, intent=${intent.hashCode()}, originalIntent=${originalIntent?.hashCode()}"
                )
                Log.d(
                    TAG,
                    "hookInstrumentation 下半场, target1=${intent.getStringExtra("target_activity")}, target2=${
                        originalIntent?.getStringExtra("target_activity")
                    }"
                )
                return if (originalIntent != null) {
                    Log.d(TAG, "hookInstrumentation newActivity, 使用原始Intent创建Activity")
                    // 调用原始方法，但使用原始Intent
                    mInstrumentation.newActivity(cl, className, intent)
//                    mInstrumentation.newActivity(cl, intent.getStringExtra("target_activity"), originalIntent)
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

        /*
         * Issue_1:
         * TargetActivity : android.app.Activity()
         * android.content.pm.PackageManager$NameNotFoundException: ComponentInfo{com.knox.babypluginframework/com.knox.babypluginframework.hookactivity.TargetActivity}
         *               com.knox.babypluginframework         W  	at android.app.ApplicationPackageManager.getActivityInfo(ApplicationPackageManager.java:565)
         *               com.knox.babypluginframework         W  	at com.android.internal.policy.PhoneWindow.installDecor(PhoneWindow.java:3017)
         *               com.knox.babypluginframework         W  	at com.android.internal.policy.PhoneWindow.setContentView(PhoneWindow.java:531)
         *               com.knox.babypluginframework         W  	at android.app.Activity.setContentView(Activity.java:3525)
         *               com.knox.babypluginframework         W  	at com.knox.babypluginframework.hookactivity.TargetActivity.onCreate(TargetActivity.kt:11)
         *               com.knox.babypluginframework         W  	at com.knox.babypluginframework.hookactivity.ProxyActivity.onCreate(ProxyActivity.kt:104)
         *
         * Closed_1:
         * 动态代理ActivityThread中的sPackageManager对象
         */

        Log.d(TAG, "成功Hook Instrumentation")
    } catch (e: Exception) {
        Log.e(TAG, "Hook Instrumentation 失败", e)
    }
}
```

### ProxyActivity.kt

```kotlin
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
import com.knox.babypluginframework.PluginManager
import java.lang.reflect.Method

private const val TAG = "ProxyActivity"

class ProxyActivity : AppCompatActivity() {

    private var pluginActivity: Activity? = null
    private var pluginClassLoader: ClassLoader? = null

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
```

### PackageHook.kt

```kotlin
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
```

### PluginManager.kt

```kotlin
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
```

## 启动插件定义的Activity（高级）

1. 相较于中级做法，支持Activity的LaunchMode

