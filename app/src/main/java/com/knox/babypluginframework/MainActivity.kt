package com.knox.babypluginframework

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.knox.babypluginframework.ui.theme.BabyPluginFrameworkTheme
import java.io.File

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var pluginApkFile: File? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        printApkRelatedDirectoryPaths(newBase)
        // 每次启动时, 将Assets中的pluginApk拷贝到/data/user/0/com.knox.babypluginframework/files/plugins/
        pluginApkFile = extractPluginApkFromAssets(newBase).onFailure { e ->
            println("extractPluginApkFromAssets failed. msg=${e.message}")
        }.getOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hostCtx = baseContext

        setContent {
            BabyPluginFrameworkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        pluginApkFile?.let { file -> InvokePluginMethod(hostCtx, file, padding) }
                        pluginApkFile?.let { file -> GetPluginResources(hostCtx, file, padding) }
                        pluginApkFile?.let { file ->
                            StartPluginServiceBasic(
                                hostCtx,
                                file,
                                padding
                            )
                        }
                        pluginApkFile?.let { file ->
                            StartPluginActivityBasic(
                                hostCtx,
                                file,
                                padding
                            )
                        }
                        // 启动HostApp中但在Manifest未声明的TargetActivity
                        StartHostActivityUndefinedInManifest(hostCtx, padding)
                        // 启动PluginApp中的PluginTargetActivity
                        StartPluginActivityIntermediate(hostCtx, padding)
                    }
                }
            }
        }
        // pluginApkFile=/data/user/0/com.knox.babypluginframework/files/plugins/pluginapk-debug.apk
        pluginApkFile?.let { file ->
            loadPluginBridge(this, file)
            // 加载插件APK (也可以放到Application中)
            PluginManager.loadPlugin(this, file)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun InvokePluginMethod(
    hostContext: Context,
    pluginApkFile: File,
    innerPadding: PaddingValues
) {
    Greeting(
        name = loadPluginThenInvokeGetNameMethod(hostContext, pluginApkFile) ?: "HostName",
        modifier = Modifier.padding(innerPadding)
    )
}

@Composable
fun GetPluginResources(
    hostContext: Context,
    pluginApkFile: File,
    innerPadding: PaddingValues
) {
    Greeting(
        name = loadPluginThenGetRes(hostContext, pluginApkFile) ?: "Android",
        modifier = Modifier.padding(innerPadding)
    )
}

@Composable
fun StartPluginServiceBasic(
    hostCtx: Context,
    pluginApkFile: File,
    padding: PaddingValues
) {
    Button(
        onClick = {
            // 点击按钮时的操作
            val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
            Log.d(TAG, clickMessage) // 打印到日志
            startPluginTestService1(hostCtx, pluginApkFile)
        },
        modifier = Modifier.padding(padding)
    ) {
        Text("Start TestService1")
    }
}

@Composable
fun StartPluginActivityBasic(
    hostCtx: Context,
    pluginApkFile: File,
    padding: PaddingValues
) {
    Button(
        onClick = {
            // 点击按钮时的操作
            val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
            Log.d(TAG, clickMessage) // 打印到日志
            startPluginTestActivity1(hostCtx, pluginApkFile)
        },
        modifier = Modifier.padding(padding)
    ) {
        Text("Start TestActivity1")
    }
}

@Composable
fun StartHostActivityUndefinedInManifest(
    hostCtx: Context,
    padding: PaddingValues
) {
    Button(
        onClick = {
            // 点击按钮时的操作
            val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
            Log.d(TAG, clickMessage) // 打印到日志

            val intent = Intent()
            val activityName =
                "com.knox.babypluginframework.hookactivity.TargetActivity"
            intent.setComponent(
                ComponentName(
                    "com.knox.babypluginframework",
                    activityName
                )
            )
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
            hostCtx.startActivity(intent)

            Log.d(TAG, "StartHostActivityUndefinedInManifest($activityName)")
        },
        modifier = Modifier.padding(padding)
    ) {
        Text("Start TargetActivity")
    }
}

@Composable
fun StartPluginActivityIntermediate(
    hostCtx: Context,
    padding: PaddingValues
) {
    Button(
        onClick = {
            // 点击按钮时的操作
            val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
            Log.d(TAG, clickMessage) // 打印到日志

            val intent = Intent()
            val activityName =
                "com.knox.pluginapk.hookactivity.PluginTargetActivity"
            intent.setComponent(
                ComponentName(
                    "com.knox.pluginapk",
                    activityName
                )
            )
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
            hostCtx.startActivity(intent)

            /*
             * Issue_2:
             * java.lang.ClassNotFoundException: com.knox.pluginapk.hookactivity.PluginTargetActivity
             *                                   at java.lang.Class.classForName(Native Method)
             *                                   at java.lang.Class.forName(Class.java:454)
             *                                   at java.lang.Class.forName(Class.java:379)
             *                                   at com.knox.babypluginframework.hookactivity.ProxyActivity.onCreate(ProxyActivity.kt:32)
             *                                   at android.app.Activity.performCreate(Activity.java:8207)
             *                                   at android.app.Activity.performCreate(Activity.java:8191)
             *                                   at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1309)
             *                                   at com.knox.babypluginframework.hookactivity.ActivityHookKt$hookInstrumentation$customInstrumentation$1.callActivityOnCreate(ActivityHook.kt:209)
             *
             * 要加载和启动插件APK中的Activity，需要执行以下几个核心步骤：
             * 1.加载插件APK，提取其中的资源和类
             * 2.创建用于加载插件类的ClassLoader
             * 3.使用这个ClassLoader来加载插件Activity类
             * 4.创建插件Activity实例，并通过反射设置其上下文、资源等
             * 5.调用其生命周期方法
             *
             * Closed_2:
             *
             */

            Log.d(TAG, "startActivity($activityName)")
        },
        modifier = Modifier.padding(padding)
    ) {
        Text("Start PluginTargetActivity")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BabyPluginFrameworkTheme {
        Greeting("Android")
    }
}

private fun printApkRelatedDirectoryPaths(context: Context?) {
    val ctx = context ?: return
    Log.d(
        // /data/user/0/com.knox.babypluginframework/files
        TAG, "filesDir=${ctx.filesDir}\n" +
            // /storage/emulated/0/Android/obb/com.knox.babypluginframework
            "obbDir=${ctx.obbDir}\n" +
            // /data/user/0/com.knox.babypluginframework
            "dataDir=${ctx.dataDir}\n" +
            // /data/user/0/com.knox.babypluginframework/cache
            "cacheDir=${ctx.cacheDir}\n" +
            // /data/user/0/com.knox.babypluginframework/code_cache
            "codeCacheDir=${ctx.codeCacheDir}\n" +
            // /storage/emulated/0/Android/data/com.knox.babypluginframework/cache
            "externalCacheDir=${ctx.externalCacheDir}\n" +
            // /data/user/0/com.knox.babypluginframework/no_backup
            "noBackupFilesDir=${ctx.noBackupFilesDir}"
    )
}




