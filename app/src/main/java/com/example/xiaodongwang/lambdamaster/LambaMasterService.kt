package com.example.xiaodongwang.lambdamaster

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import com.example.xiaodong.lambdamaster.DBOpenHelper
import com.rabbitmq.client.*
import java.util.*


class LambaMasterService : Service() {

    private var dbHelper: DBOpenHelper? = null
    private var lambdaList: ArrayList<Lambda>? = null
    private var sharedPrefs: SharedPreferences? = null
    private var routingKeyHandlers: MutableMap<String, RoutingKeyHandler>? = null
    private var connection: Connection? = null
    private var channel: Channel? = null

    inner class LambdaConsumer(val chan: Channel) : DefaultConsumer(chan) {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            val routingKey = envelope.routingKey
            if (routingKeyHandlers!!.containsKey(routingKey)) {
                routingKeyHandlers!![routingKey]!!.handleDelivery(
                        consumerTag, envelope, properties, body
                )
            } else {
                Log.e(LOG_TAG, "failed to find routing key handler for $routingKey")
            }
        }
    }

    inner class GeneralRoutingKeyHandler : RoutingKeyHandler {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            var message = String(body)
            Log.i(LOG_TAG, "receive message $message")
        }
    }

    inner class LambdaRoutingKeyBinaryHandler(
            val lambdaHook: Lambda
    ) : RoutingKeyHandler {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            var message = String(body)
            Log.i(LOG_TAG, "receive message $message")
        }
    }

    inner class LambdaRoutingKeyConfigurationHandler(
            val lambdaHook: Lambda
    ) : RoutingKeyHandler {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            var message = String(body)
            Log.i(LOG_TAG, "receive message $message")
        }
    }

    inner class LambdaRoutingKeyExecuteHandler(
            val lambdaHook: Lambda
    ) : RoutingKeyHandler {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            var message = String(body)
            var deliverTag = envelope.deliveryTag.toString()
            Log.i(LOG_TAG, "receive message $message with deliverTag=$deliverTag")
            var event = Event(deliverTag, lambdaHook, message)
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

    fun getRoutingKeyHandlerByType(routingKeyType: String, lambdaHook: Lambda): RoutingKeyHandler {
        when(routingKeyType) {
            ROUTING_KEY_TYPE_BINARY -> return LambdaRoutingKeyBinaryHandler(lambdaHook)
            ROUTING_KEY_TYPE_CONFIGURATION -> return LambdaRoutingKeyConfigurationHandler(lambdaHook)
            ROUTING_KEY_TYPE_EXECUTE -> return LambdaRoutingKeyExecuteHandler(lambdaHook)
            else -> return GeneralRoutingKeyHandler()
        }
    }

    @Synchronized private fun startAMQP() {
        var factory = ConnectionFactory()
        factory.host = sharedPrefs!!.getString("rabbitmq_host", "localhost")
        factory.port = sharedPrefs!!.getString("rabbitmq_port", "5672").toInt()
        factory.virtualHost = sharedPrefs!!.getString("rabbitmq_virtualhost", "/")
        factory.username = sharedPrefs!!.getString("rabbitmq_username", "guest")
        factory.password = sharedPrefs!!.getString("rabbitmq_password", "guest")
        var exchangeName = sharedPrefs!!.getString("exchange_name", "")
        var queueName = sharedPrefs!!.getString("queue_name", "")
        var routingKey = sharedPrefs!!.getString("routing_key", "")
        factory.setAutomaticRecoveryEnabled(true)
        connection = factory.newConnection()
        channel = connection!!.createChannel()
        channel!!.exchangeDeclare(exchangeName, "topic")
        channel!!.queueDeclare(queueName, false, false, false, null)
        channel!!.queueBind(queueName, exchangeName, routingKey)
        routingKeyHandlers = mutableMapOf()
        routingKeyHandlers!![routingKey] = GeneralRoutingKeyHandler()
        lambdaList?.let {
            for (item in it) {
                for (routingKeyType in arrayOf(
                        ROUTING_KEY_TYPE_BINARY, ROUTING_KEY_TYPE_CONFIGURATION,
                        ROUTING_KEY_TYPE_EXECUTE
                )) {
                    val routingKeyForLambda = "$routingKeyType.${item.name}"
                    channel!!.queueBind(queueName, exchangeName, routingKeyForLambda)
                    routingKeyHandlers!![routingKeyForLambda] = getRoutingKeyHandlerByType(
                            routingKeyType, item
                    )
                }
            }
        }
        channel!!.basicConsume(queueName, true, LambdaConsumer(channel!!))
    }

    @Synchronized private fun stopAMQP() {
        channel?.let {
            it.close()
        }
        channel = null
        connection?.let {
            it.close()
        }
        connection = null
    }

    private fun startInForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this)
                .setContentTitle("LambdaMasterService")
                .setContentText("lambda master service")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setTicker("TICKER")
                .setAutoCancel(false)
                .setWhen(System.currentTimeMillis())
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .build()
        startForeground(101, notification)
    }

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

    fun getLambda(actionName: String) : Lambda? {
        var lambdaFound: Lambda? = null
        for (lambdaHook in lambdaList!!) {
            if (lambdaHook.name == actionName) {
                lambdaFound = lambdaHook
                break
            }
        }
        return lambdaFound
    }

    inner class EventHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            var event = msg!!.obj as Event
            Log.i(LOG_TAG, "event handler receive event $event")
            val lambdaHook = event.lambdaHook
            val intent = Intent(lambdaHook.name)
            intent.setPackage(lambdaHook.package_name)
            intent.putExtra("name", event.name)
            intent.putExtra("payload", event.payload)
            Log.i(LOG_TAG, "send message to lambda handler")
            if (startForegroundService(intent) == null) {
                Log.e(LOG_TAG, "failed to start the lambda service")
            }
        }
    }

    inner class EventHandlerThread(name: String): HandlerThread(name) {
        override fun onLooperPrepared() {
            super.onLooperPrepared()
            eventHandler = EventHandler(looper)
        }
    }

    inner class EventConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(LOG_TAG, "service is connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(LOG_TAG, "service is disconnected")
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
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        lambdaList = getInitialLambdaList()
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        createNotificationChannel()
        startInForeground()
        startAMQP()
    }

    private fun getInitialLambdaList() : ArrayList<Lambda> {
        return dbHelper!!.getLambdas()
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "destroy service")
        if (handlerThread != null) {
            handlerThread!!.quit()
            handlerThread = null
            eventHandler = null
        }
        stopAMQP()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(LOG_TAG, "unbind service")
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "start commond is called")
        return super.onStartCommand(intent, flags, startId)
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MAX
            )
            mChannel.enableLights(true)
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.lightColor = Color.RED
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    companion object {
        private val LOG_TAG = "LambaMasterService"
        private val NOTIFICATION_CHANNEL_ID = "lambda_master"
        private val NOTIFICATION_CHANNEL_NAME = "lambda_master"
        private val ROUTING_KEY_TYPE_BINARY = "binary"
        private val ROUTING_KEY_TYPE_CONFIGURATION = "configuration"
        private val ROUTING_KEY_TYPE_EXECUTE = "execute"
    }
}
