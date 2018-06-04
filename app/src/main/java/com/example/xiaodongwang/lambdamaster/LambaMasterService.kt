package com.example.xiaodongwang.lambdamaster

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class LambaMasterService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        Log.i(LOG_TAG, "bind service")
        return null
    }

    override fun onCreate() {
        Log.i(LOG_TAG, "create service")
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "destroy service")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(LOG_TAG, "unbind service")
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "start commond is called")
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        private val LOG_TAG = "LambaMasterService"
    }
}
