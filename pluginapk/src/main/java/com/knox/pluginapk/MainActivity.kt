package com.knox.pluginapk

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
import com.knox.pluginapk.ui.theme.BabyPluginFrameworkTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
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
                        Greeting(
                            name = "PluginMainActivity",
                            modifier = Modifier.padding(padding)
                        )
                        StartPluginActivityIntermediate(hostCtx, padding)
                    }
                }
            }
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