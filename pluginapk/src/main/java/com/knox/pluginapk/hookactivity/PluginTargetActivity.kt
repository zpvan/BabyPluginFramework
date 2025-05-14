package com.knox.pluginapk.hookactivity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.knox.pluginapk.R

private const val TAG = "PluginTargetActivity"

class PluginTargetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate In.")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "setContentView Begin.")
        setContentView(R.layout.activity_plugin_target)
        Log.d(TAG, "setContentView End.")
        Log.d(TAG, "onCreate Out.")
    }
}