package com.knox.pluginapk

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

private const val TAG = "TestService1"

class TestService1 : Service() {

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    class LocalBinder : Binder()

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind In PluginApk.")
        return LocalBinder()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate In PluginApk.")
    }
}