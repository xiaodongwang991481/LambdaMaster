package com.example.xiaodongwang.lambdamaster

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    inner class StartService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, LambaMasterService::class.java)
            Log.i(LOG_TAG, "start service")
            startService(intent)
        }
    }

    inner class StopService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, LambaMasterService::class.java)
            Log.i(LOG_TAG, "stop service")
            stopService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_service.setOnClickListener(StartService())
        stop_service.setOnClickListener(StopService())
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
        private val LOG_TAG = "MainActivity"
    }
}
