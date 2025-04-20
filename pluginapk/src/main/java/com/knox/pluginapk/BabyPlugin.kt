package com.knox.pluginapk

import android.content.res.Resources
import com.knox.pluginlibrary.IPlugin

class BabyPlugin : IPlugin {
    override val name: String
        get() = "BabyPlugin"

    @OptIn(ExperimentalStdlibApi::class)
    override fun getStringForResId(resources: Resources): String {
        return resources.getString(R.string.hello_resources).also {
            println("In BabyPlugin, getStringForResId(${R.string.hello_resources.toHexString()})=$it")
        }
    }
}