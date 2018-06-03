package com.example.xiaodongwang.lambdamaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LambaResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.i(LOG_TAG, "receive lamba result")
    }

    companion object {
        private val LOG_TAG = "LambaResultReceiver"
    }
}
