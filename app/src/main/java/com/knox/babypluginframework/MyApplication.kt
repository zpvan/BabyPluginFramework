package com.knox.babypluginframework

import android.app.Application
import com.knox.babypluginframework.hookactivity.ActivityHook
import com.knox.babypluginframework.hookactivity.ProxyActivity
import com.knox.babypluginframework.hookactivity.hookPackageManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 安装Activity Hook
        ActivityHook.install(this, ProxyActivity::class.java)

        // 添加需要代理的目标Activity
        ActivityHook.addTargetActivity("com.knox.babypluginframework.hookactivity.TargetActivity")

        hookPackageManager(this)
    }
}