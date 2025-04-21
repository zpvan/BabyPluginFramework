package com.knox.babypluginframework

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.knox.babypluginframework.ui.theme.BabyPluginFrameworkTheme
import java.io.File

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
        setContent {
            BabyPluginFrameworkTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
                    Greeting(
                        name = pluginApkFile?.let { loadPluginAndGetRes(this, it) } ?: "Android",
                    )
                }) { innerPadding ->
                    Greeting(
                        name = pluginApkFile?.let { loadPluginAndGetName(this, it) } ?: "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        // pluginApkFile=/data/user/0/com.knox.babypluginframework/files/plugins/pluginapk-debug.apk
        println("pluginApkFile=$pluginApkFile")
        pluginApkFile?.let {
            loadPluginBridge(this, it)
            Log.d(TAG, "startPluginTestService1 Begin")
            startPluginTestService1(this, it)
            Log.d(TAG, "startPluginTestService1 End")
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