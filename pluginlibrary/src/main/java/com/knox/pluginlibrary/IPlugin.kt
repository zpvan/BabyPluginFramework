package com.knox.pluginlibrary

import android.content.res.Resources

interface IPlugin {

    val name: String

    fun getStringForResId(resources: Resources): String
}