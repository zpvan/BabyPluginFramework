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
import com.knox.babypluginframework.hookactivity.ActivityHook
import com.knox.babypluginframework.hookactivity.ProxyActivity
import dalvik.system.PathClassLoader

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var pluginApkFile: File? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        pluginApkFile = extractPluginApkFromAssets(newBase).onFailure { e ->
            println("extractPluginApkFromAssets failed. msg=${e.message}")
        }.getOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hostContext = this.baseContext

        setContent {
            BabyPluginFrameworkTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
                    Greeting(
                        name = pluginApkFile?.let { loadPluginAndGetRes(this, it) } ?: "Android",
                    )
                }) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Greeting(
                            name = pluginApkFile?.let { loadPluginAndGetName(hostContext, it) }
                                ?: "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                        Button(
                            onClick = {
                                // 点击按钮时的操作
                                val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
                                Log.d(TAG, clickMessage) // 打印到日志
                                pluginApkFile?.let {
                                    startPluginTestService1(hostContext, it)
                                    Log.d(
                                        TAG,
                                        "pluginApkFile is exist at ${System.currentTimeMillis()}"
                                    )
                                }

                            },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Text("Start TestService1")
                        }
                        Button(
                            onClick = {
                                // 点击按钮时的操作
                                val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
                                Log.d(TAG, clickMessage) // 打印到日志

                                val intent = Intent()
                                val activityName = "com.knox.babypluginframework.HostTestActivity"
                                intent.setComponent(ComponentName("com.knox.babypluginframework", activityName))
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK)

                                Log.d(TAG, "startHostTestActivity baseContext=${hostContext.hashCode()}")
                                hostContext.startActivity(intent)
                            },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Text("Start HostTestActivity")
                        }
                        Button(
                            onClick = {
                                // 点击按钮时的操作
                                val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
                                Log.d(TAG, clickMessage) // 打印到日志
                                pluginApkFile?.let {
                                    startPluginTestActivity1(hostContext, it)
                                    Log.d(
                                        TAG,
                                        "pluginApkFile is exist at ${System.currentTimeMillis()}"
                                    )
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Text("Start TestActivity1")
                        }
                        // 启动HostApp中但在Manifest未声明的TargetActivity
                        Button(
                            onClick = {
                                // 点击按钮时的操作
                                val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
                                Log.d(TAG, clickMessage) // 打印到日志

                                val intent = Intent()
//                                val activityName = "com.knox.babypluginframework.hookactivity.ProxyActivity"
                                val activityName = "com.knox.babypluginframework.hookactivity.TargetActivity"
                                intent.setComponent(ComponentName("com.knox.babypluginframework", activityName))
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
                                hostContext.startActivity(intent)

                                Log.d(TAG,"startActivity($activityName)")
                            },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Text("Start TargetActivity")
                        }
                        // 启动PluginApp中的PluginTargetActivity
                        Button(
                            onClick = {
                                // 点击按钮时的操作
                                val clickMessage = "Button clicked at ${System.currentTimeMillis()}"
                                Log.d(TAG, clickMessage) // 打印到日志

                                val intent = Intent()
                                val activityName = "com.knox.pluginapk.hookactivity.PluginTargetActivity"
                                intent.setComponent(ComponentName("com.knox.pluginapk", activityName))
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
                                hostContext.startActivity(intent)

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

                                Log.d(TAG,"startActivity($activityName)")
                            },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Text("Start PluginTargetActivity")
                        }
                    }
                }
            }
        }
        // pluginApkFile=/data/user/0/com.knox.babypluginframework/files/plugins/pluginapk-debug.apk
        println("pluginApkFile=$pluginApkFile")
        pluginApkFile?.let {
            loadPluginBridge(this, it)
            // 加载插件APK (也可以放到Application中)
            PluginManager.loadPlugin(this, it)
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BabyPluginFrameworkTheme {
        Greeting("Android")
    }
}