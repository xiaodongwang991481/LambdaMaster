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
import com.google.gson.Gson
import com.rabbitmq.client.*
import java.util.*
import kotlin.concurrent.thread


class LambaMasterService : Service() {

    private var dbHelper: DBOpenHelper? = null
    private var lambdaList: ArrayList<Lambda>? = null
    private var sharedPrefs: SharedPreferences? = null
    @Volatile var amqpThreadRunning = false
    private var amqpLock = Object()

    inner class LambdaExecuteConsumer(
            val chan: Channel,
            val routingKeyExecuteHandlers: MutableMap<String, RoutingKeyHandler>,
            val defaultRoutingKeyExecuteHandler: RoutingKeyHandler) : DefaultConsumer(chan) {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            val routingKey = envelope.routingKey
            if (routingKeyExecuteHandlers.containsKey(routingKey)) {
                routingKeyExecuteHandlers[routingKey]!!.handleDelivery(
                        consumerTag, envelope, properties, body
                )
            } else {
                defaultRoutingKeyExecuteHandler.handleDelivery(
                        consumerTag, envelope, properties, body
                )
            }
        }
    }

    inner class LambdaBinaryConsumer(
            val chan: Channel,
            val routingKeyBinaryHandlers: MutableMap<String, RoutingKeyHandler>,
            val defaultRoutingKeyBinaryHandler: RoutingKeyHandler
    ) : DefaultConsumer(chan) {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            val routingKey = envelope.routingKey
            if (routingKeyBinaryHandlers.containsKey(routingKey)) {
                routingKeyBinaryHandlers[routingKey]!!.handleDelivery(
                        consumerTag, envelope, properties, body
                )
            } else {
                defaultRoutingKeyBinaryHandler.handleDelivery(
                        consumerTag, envelope, properties, body
                )
            }
        }
    }

    inner class DefaultRoutingKeyExecuteHandler(
            val routingKeyExecuteHandlers: MutableMap<String, RoutingKeyHandler>
    ) : RoutingKeyHandler {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            var message = String(body)
            var deliverTag = envelope.deliveryTag.toString()
            Log.i(LOG_TAG, "receive message $message with deliverTag=$deliverTag")
            val gson = Gson()
            val envelopedMessage = gson.fromJson(message, RabbitMQMessage::class.java)
            val lambdaFound = getLambda(envelopedMessage.actionName)
            if (lambdaFound != null) {
                routingKeyExecuteHandlers[lambdaFound.name]!!.handleDelivery(
                        consumerTag, envelope, properties, envelopedMessage.properties.toByteArray()
                )
            } else {
                Log.e(LOG_TAG, "failed to get lambda from ${envelopedMessage.actionName}")
            }
        }
    }

    inner class DefaultRoutingKeyBinaryHandler(
            val routingKeyBinaryHandlers: MutableMap<String, RoutingKeyHandler>
    ) : RoutingKeyHandler {
        override fun handleDelivery(
                consumerTag: String?, envelope: Envelope,
                properties: AMQP.BasicProperties?, body: ByteArray
        ) {
            var message = String(body)
            var deliverTag = envelope.deliveryTag.toString()
            Log.i(LOG_TAG, "receive message $message with deliverTag=$deliverTag")
            val gson = Gson()
            val envelopedMessage = gson.fromJson(message, RabbitMQMessage::class.java)
            val lambdaFound = getLambda(envelopedMessage.actionName)
            if (lambdaFound != null) {
                routingKeyBinaryHandlers[lambdaFound.name]!!.handleDelivery(
                        consumerTag, envelope, properties, envelopedMessage.properties.toByteArray()
                )
            } else {
                Log.e(LOG_TAG, "failed to get lambda from ${envelopedMessage.actionName}")
            }
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
            var event = Event(UUID.randomUUID().toString(), lambdaHook, message)
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

    fun getRoutingKeyBinaryHandler(lambdaHook: Lambda): RoutingKeyHandler {
        return LambdaRoutingKeyBinaryHandler(lambdaHook)
    }

    fun getRoutingKeyExecuteHandler(lambdaHook: Lambda): RoutingKeyHandler {
        return LambdaRoutingKeyExecuteHandler(lambdaHook)
    }

    @Synchronized private fun startAMQP() {
        var host = sharedPrefs!!.getString("rabbitmq_host", "localhost")
        Log.i(LOG_TAG, "rabbitmq host=$host")
        var port = sharedPrefs!!.getString("rabbitmq_port", "5672")
        Log.i(LOG_TAG, "rabbitmq port=port")
        var virtualHost = sharedPrefs!!.getString("rabbitmq_virtualhost", "/")
        Log.i(LOG_TAG, "rabbitmq virtualhost=$virtualHost")
        var username = sharedPrefs!!.getString("rabbitmq_username", "guest")
        Log.i(LOG_TAG, "rabbitmq username=$username")
        var password = sharedPrefs!!.getString("rabbitmq_password", "guest")
        Log.i(LOG_TAG, "rabbitmq password=$password")
        var exchangeName = sharedPrefs!!.getString("exchange_name", "")
        var queueName = sharedPrefs!!.getString("queue_name", "")
        var broadcastExchangeName = sharedPrefs!!.getString("broadcast_exchange_name", "")
        var anycastExchangeName = sharedPrefs!!.getString("anycast_exchange_name", "")
        var anycastQueueName = sharedPrefs!!.getString("anycast_queue_name", "")
        Log.i(LOG_TAG, "exchange name=$exchangeName")
        Log.i(LOG_TAG, "queue name=$queueName")
        Log.i(LOG_TAG, "broadcast exchange name=$broadcastExchangeName")
        Log.i(LOG_TAG, "anycast exchange name=$anycastExchangeName")
        Log.i(LOG_TAG, "anycast queue name=$anycastQueueName")
        amqpThreadRunning = true
        thread(start=true) {
            var factory = ConnectionFactory()
            factory.host = host
            if (!port.isNullOrBlank()) {
                factory.port = port.toInt()
            }
            if (!virtualHost.isNullOrBlank()) {
                factory.virtualHost = virtualHost
            }
            if (!username.isNullOrBlank()) {
                factory.username = username
            }
            if (!password.isNullOrBlank()) {
                factory.password = password
            }
            factory.setAutomaticRecoveryEnabled(true)
            Log.i(LOG_TAG, "start ampq thread")
            while (amqpThreadRunning) {
                try {
                    var connection = factory.newConnection()
                    var channel = connection!!.createChannel()
                    Log.i(LOG_TAG, "ampq connection is created")
                    channel.queueDeclare(queueName, false, false, false, null)
                    if (!exchangeName.isNullOrBlank()) {
                        channel.exchangeDeclare(exchangeName, "topic")
                    }
                    channel.queueDeclare(anycastQueueName, false, false, false, null)
                    if (!anycastExchangeName.isNullOrBlank()) {
                        channel.exchangeDeclare(anycastExchangeName, "topic")
                    }
                    if (!broadcastExchangeName.isNullOrBlank()) {
                        channel.exchangeDeclare(broadcastExchangeName, "fanout")
                        channel.queueBind(queueName, broadcastExchangeName, "")
                        Log.i(LOG_TAG, "bind queue $queueName to exchange $broadcastExchangeName")
                    }
                    var routingKeyExecuteHandlers = mutableMapOf<String, RoutingKeyHandler>()
                    var routingKeyBinaryHandlers = mutableMapOf<String, RoutingKeyHandler>()
                    lambdaList?.let {
                        for (item in it) {
                            if (!exchangeName.isNullOrBlank()) {
                                channel.queueBind(queueName, exchangeName, item.name)
                                Log.i(
                                        LOG_TAG,
                                        "bind queue $queueName with " +
                                        "exchange $exchangeName by routing key ${item.name}"
                                )
                            }
                            routingKeyBinaryHandlers[item.name] = getRoutingKeyBinaryHandler(
                                    item
                            )
                            if (!anycastExchangeName.isNullOrBlank()) {
                                channel.queueBind(anycastQueueName, anycastExchangeName, item.name)
                                Log.i(
                                        LOG_TAG,
                                        "bind queue $anycastQueueName with exchange $anycastExchangeName " +
                                        "by routing key ${item.name}"
                                )
                            }
                            routingKeyExecuteHandlers[item.name] = getRoutingKeyExecuteHandler(
                                    item
                            )
                        }
                    }
                    var defaultRoutingKeyBinaryHandler = DefaultRoutingKeyBinaryHandler(routingKeyBinaryHandlers)
                    var defaultRoutingKeyExecuteHandler = DefaultRoutingKeyExecuteHandler(routingKeyExecuteHandlers)
                    channel.basicConsume(
                            queueName, true,
                            LambdaBinaryConsumer(channel, routingKeyBinaryHandlers, defaultRoutingKeyBinaryHandler)
                    )
                    channel.basicConsume(
                            anycastQueueName, true,
                            LambdaExecuteConsumer(channel, routingKeyExecuteHandlers, defaultRoutingKeyExecuteHandler)
                    )
                } catch (e: Exception) {
                    Log.e(LOG_TAG, e.message)
                    Thread.sleep(1000)
                }
            }
            Log.i(LOG_TAG, "amqp thread is finished")
            synchronized(amqpLock) {
                amqpLock.notifyAll()
            }
        }
    }

    @Synchronized private fun stopAMQP() {
        synchronized(amqpLock) {
            if (amqpThreadRunning) {
                amqpThreadRunning = false
                Log.i(LOG_TAG, "stop amqp thread")
                amqpLock.wait()
            } else {
                Log.i(LOG_TAG, "amqp thread is already stopped")
            }
        }
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

    @Synchronized fun startEventHandler() {
        if (handlerThread == null) {
            handlerThread = EventHandlerThread("EventHandler")
            handlerThread!!.start()
        } else {
            Log.i(LOG_TAG, "handler thread is already started")
        }
    }

    @Synchronized fun stopEventHandler() {
        if (handlerThread != null) {
            handlerThread!!.quit()
        }
        handlerThread = null
        eventHandler = null
    }

    override fun onCreate() {
        Log.i(LOG_TAG, "create service")
        super.onCreate()
        createNotificationChannel()
        startInForeground()
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        lambdaList = getInitialLambdaList()
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        startEventHandler()
        startAMQP()
    }

    private fun getInitialLambdaList() : ArrayList<Lambda> {
        return dbHelper!!.getLambdas()
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "destroy service")
        stopAMQP()
        stopEventHandler()
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
    }
}
