package com.example.xiaodongwang.lambdamaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class LambaResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        var data = intent.extras
        val name = data.getString("name")
        val payload = data.getString("payload")
        Log.i(
                LOG_TAG,
                "receive lamba result $name=$payload"
        )
        Toast.makeText(
                context,
                "receive lambda result $name=$payload", Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        private val LOG_TAG = "LambaResultReceiver"
    }
}
