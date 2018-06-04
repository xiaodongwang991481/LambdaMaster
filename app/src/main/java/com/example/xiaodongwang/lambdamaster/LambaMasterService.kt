package com.example.xiaodongwang.lambdamaster

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log

class LambaMasterService : Service() {

    inner class EventBinder() : IEvent.Stub(), IBinder {
        override fun sendMessage(event: Event?) {
            Log.i(LOG_TAG, "receive event $event")
            if (eventHandler != null) {
                Log.i(LOG_TAG, "send event $event to event handler")
                var msg = Message.obtain()
                msg.obj = event
                eventHandler!!.sendMessage(msg)
            } else {
                Log.e(LOG_TAG, "event handler is not ready yet")
            }
        }
    }

    inner class EventHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            var event = msg!!.obj as Event
            Log.i(LOG_TAG, "event handler receive event $event")
            val intent = Intent(event.lambdaHook.name)
            intent.setPackage(event.lambdaHook.package_name)
            intent.putExtra("name", event.name)
            intent.putExtra("payload", event.payload)
            if (startService(intent) == null) {
                Log.e(LOG_TAG, "failed to start the service")
            }
        }
    }

    inner class EventHandlerThread(name: String): HandlerThread(name) {
        override fun onLooperPrepared() {
            super.onLooperPrepared()
            eventHandler = EventHandler(looper)
        }
    }

    private var eventBinder: IBinder = EventBinder()
    private var handlerThread: HandlerThread? = null
    private var eventHandler: EventHandler? = null

    override fun onBind(intent: Intent): IBinder? {
        Log.i(LOG_TAG, "bind service")
        return eventBinder
    }

    override fun onCreate() {
        Log.i(LOG_TAG, "create service")
        super.onCreate()
        if (handlerThread == null) {
            handlerThread = EventHandlerThread("EventHandler")
            handlerThread!!.start()
        } else {
            Log.i(LOG_TAG, "handler thread is already started")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "destroy service")
        if (handlerThread != null) {
            handlerThread!!.quit()
            handlerThread = null
            eventHandler = null
        }
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
