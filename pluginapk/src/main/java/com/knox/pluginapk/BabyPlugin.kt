package com.knox.pluginapk

import android.content.res.Resources
import android.util.Log
import com.knox.pluginlibrary.IPlugin

private const val TAG = "BabyPlugin"

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