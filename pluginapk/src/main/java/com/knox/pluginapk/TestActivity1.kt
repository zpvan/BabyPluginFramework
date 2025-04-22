package com.knox.pluginapk

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.knox.pluginlibrary.IPlugin

private const val TAG = "TestActivity1"

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