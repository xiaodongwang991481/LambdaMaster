package com.example.xiaodongwang.lambdamaster

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.example.xiaodong.lambdamaster.DBOpenHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.os.IBinder
import android.widget.Toast
import android.app.PendingIntent
import android.content.*
import android.preference.PreferenceManager
import android.widget.ViewAnimator
import com.google.gson.Gson
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory


class MainActivity : AppCompatActivity() {

    inner class StartService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, LambaMasterService::class.java)
            Log.i(LOG_TAG, "start service")
            if(startForegroundService(intent) == null) {
                Log.e(LOG_TAG, "failed to start service")
            }
        }
    }

    inner class StopService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, LambaMasterService::class.java)
            Log.i(LOG_TAG, "stop service")
            if (!stopService(intent)) {
                Log.e(LOG_TAG, "failed to stop service")
            }
        }
    }

    inner class BindService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, LambaMasterService::class.java)
            Log.i(LOG_TAG, "bind service")
            if(!bindService(intent, eventConnection, BIND_AUTO_CREATE)) {
                Log.e(LOG_TAG, "failed to bind service")
            }
        }
    }

    inner class UnbindService : View.OnClickListener {
        override fun onClick(v: View?) {
            Log.i(LOG_TAG, "unbind service")
            unbindService(eventConnection)
        }
    }

    inner class AddLambda : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickAddLambda()
        }
    }

    inner class SaveLambdas : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickSaveLambdas()
        }
    }

    inner class EditLambda : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val lambdaHook: Lambda? = lambdas.getItemAtPosition(position) as? Lambda
            Log.i(LOG_TAG, "item $position click on $lambdaHook")
            this@MainActivity.onButtonClickEditLambda(lambdaHook!!)
        }
    }

    inner class CopyLambda : AdapterView.OnItemLongClickListener {
        override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
            val lambdaHook: Lambda? = lambdas.getItemAtPosition(position) as? Lambda
            Log.i(LOG_TAG, "item $position long click on $lambdaHook")
            action_name.setText(lambdaHook!!.name)
            return true
        }
    }

    inner class SelectLambda : View.OnClickListener {
        override fun onClick(v: View?) {
            var lambdaNames = ArrayList<String>()
            if (lambdaList != null) {
                for (item in lambdaList!!) {
                    lambdaNames.add(item.name)
                }
            }
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Select Action Name")
            builder.setItems(lambdaNames.toTypedArray(), DialogInterface.OnClickListener {
                _, which ->
                action_name.setText(lambdaNames[which])
            })
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }

    inner class SendMessage : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickSendMessage()
        }
    }

    inner class SendMessageByRabbitMQ : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickSendMessageByRabbitMQ()
        }
    }

    inner class Settings : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickSettings()
        }
    }

    inner class EventConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(LOG_TAG, "service is connected as $service")
            iEvent = IEvent.Stub.asInterface(service)
            Toast.makeText(
                    this@MainActivity, "service is bond", Toast.LENGTH_SHORT
            ).show()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(LOG_TAG, "service is disconnected")
            iEvent = null
            Toast.makeText(
                    this@MainActivity, "service is unbond", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private var dbHelper: DBOpenHelper? = null
    private var lambdaList: ArrayList<Lambda>? = null
    private var lambdasAdapter: LambdaAdapter? = null
    private var iEvent: IEvent? = null
    private var eventConnection = EventConnection()
    private var sharedPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestPermissions()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_service.setOnClickListener(StartService())
        stop_service.setOnClickListener(StopService())
        bind_service.setOnClickListener(BindService())
        unbind_service.setOnClickListener(UnbindService())
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        lambdaList = getInitialLambdaList()
        lambdasAdapter = LambdaAdapter(
                this, lambdaList!!
        )
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        var header = layoutInflater.inflate(R.layout.lambda_header, lambdas, false)
        lambdas.addHeaderView(header)
        var footer = layoutInflater.inflate(R.layout.listview_footer, lambdas, false)
        lambdas.addFooterView(footer)
        lambdas.setAdapter(lambdasAdapter!!)
        lambdas.smoothScrollToPosition(0, 0)
        add_lambda.setOnClickListener(AddLambda())
        lambdas.setOnItemClickListener(EditLambda())
        lambdas.setOnItemLongClickListener(CopyLambda())
        save_lambdas.setOnClickListener(SaveLambdas())
        action_name.setOnClickListener(SelectLambda())
        send_message.setOnClickListener(SendMessage())
        settings.setOnClickListener(Settings())
        send_message_by_rabbitmq.setOnClickListener(SendMessageByRabbitMQ())
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }
        Log.i(LOG_TAG, "save state")
        lambdaList?.let {
            outState?.putParcelableArrayList("lambdas", lambdaList)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        Log.i(LOG_TAG, "restore state")
        lambdaList = savedInstanceState?.getParcelableArrayList("lambdas") ?: getInitialLambdaList()
        lambdasAdapter = LambdaAdapter(
                this, lambdaList!!
        )
        lambdas.setAdapter(lambdasAdapter!!)
    }


    fun onButtonClickAddLambda() {
        Log.i(LOG_TAG, "add lambda")
        val intent = Intent(this, LambdaEditActivity::class.java)
        this.startActivityForResult(intent, REQUEST_ADD_LAMBDA)
    }

    fun onButtonClickSaveLambdas() {
        Log.i(LOG_TAG, "save lambdas: $lambdaList")
        dbHelper!!.updateLambdas(lambdaList!!)
    }

    override fun onStart() {
        super.onStart()
        Log.i(LOG_TAG, "start activity")
    }

    override fun onStop() {
        super.onStop()
        Log.i(LOG_TAG, "stop activity")
    }

    override fun onResume() {
        super.onResume()
        Log.i(LOG_TAG, "resume activity")
    }

    override fun onPause() {
        super.onPause()
        Log.i(LOG_TAG, "pause activity")
    }

    fun updateLambda(lambdaHook: Lambda) {
        Log.i(LOG_TAG,"update lambda $lambdaHook")
        var found = false
        var lambdaListInternal = lambdaList!!
        for (i in lambdaListInternal.indices) {
            var existingLambda = lambdaListInternal[i]
            if (existingLambda.name == lambdaHook.name) {
                existingLambda= lambdaHook
                lambdaListInternal[i] = existingLambda
                Log.i(LOG_TAG, "update existing lambda $existingLambda")
                found = true
                break
            }
        }
        if (!found) {
            Log.e(LOG_TAG, "lambda ${lambdaHook.name} is not found in existing lambdas")
        }
        lambdasAdapter!!.notifyDataSetChanged()
    }

    fun addLambda(lambdaHook: Lambda) {
        Log.i(LOG_TAG,"add lambda $lambdaHook")
        if (lambdaHook in lambdaList!!) {
            Log.i(LOG_TAG, "lambda $lambdaHook in lambda list $lambdaList")
        } else {
            lambdaList!!.add(lambdaHook)
        }
        lambdasAdapter!!.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            data?.let {
                val lambdaHook: Lambda = it.getParcelableExtra("lambda") as Lambda
                when (requestCode) {
                    REQUEST_EDIT_LAMBDA -> updateLambda(lambdaHook)
                    REQUEST_ADD_LAMBDA -> addLambda(lambdaHook)
                    else -> {
                        Log.e(LOG_TAG, "unknown request code $requestCode")
                    }
                }
            }
        } else {
            Log.e(LOG_TAG, "result code = $resultCode")
        }
    }

    fun onButtonClickEditLambda(lambdaHook: Lambda) {
        Log.i(LOG_TAG, " edit lambda $lambdaHook")
        val intent = Intent(this, LambdaEditActivity::class.java)
        intent.putExtra("lambda", lambdaHook)
        this.startActivityForResult(intent, REQUEST_EDIT_LAMBDA)
    }

    fun onButtonClickDeleteLambda(lambdaHook: Lambda) {
        Log.i(LOG_TAG, "delete lambda $lambdaHook")
        lambdaList!!.remove(lambdaHook)
        lambdasAdapter!!.notifyDataSetChanged()
    }

    fun onButtonClickSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun checkReadyToSendMessage(): Boolean {
        if (action_name == null || action_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "action name is empty")
            Toast.makeText(
                    this, "action name is empty", Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (payload == null || payload.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "payload is empty")
            Toast.makeText(
                    this, "payload is empty", Toast.LENGTH_SHORT
            )
            return false
        }
        if (lambdaList == null) {
            Log.e(LOG_TAG, "empty lambda list")
            Toast.makeText(
                    this, "empty lambda list", Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    fun onButtonClickSendMessageByRabbitMQ() {
        if (!checkReadyToSendMessage()) {
            Log.e(LOG_TAG, "not ready to send message")
            return
        }
        var actionName = action_name.text.toString()
        var payLoad = payload.text.toString()
        var event = RabbitMQMessage(actionName = actionName, properties = payLoad)
        var gson = Gson()
        var message = gson.toJson(RabbitMQMessage::class.java)
        var factory = ConnectionFactory()
        factory.host = sharedPrefs!!.getString("rabbitmq_host", "localhost")
        factory.port = sharedPrefs!!.getString("rabbitmq_port", "5672").toInt()
        factory.virtualHost = sharedPrefs!!.getString("rabbitmq_virtualhost", "/")
        factory.username = sharedPrefs!!.getString("rabbitmq_username", "guest")
        factory.password = sharedPrefs!!.getString("rabbitmq_password", "guest")
        var anycastQueueName = sharedPrefs!!.getString("anycast_queue_name", "")
        factory.setAutomaticRecoveryEnabled(true)
        var connection = factory.newConnection()
        var channel = connection.createChannel()
        channel.basicPublish("", anycastQueueName, null, message.toByteArray())
        channel.close()
        connection.close()
    }

    fun onButtonClickSendMessage() {
        if (!checkReadyToSendMessage()) {
            Log.e(LOG_TAG, "not ready to send message")
            return
        }
        if (iEvent == null) {
            Log.e(LOG_TAG, "bind interface is not ready")
            Toast.makeText(
                    this, "bind interface is not ready", Toast.LENGTH_SHORT
            ).show()
            return
        }
        var uuid = UUID.randomUUID().toString()
        var actionName = action_name.text.toString()
        var payLoad = payload.text.toString()
        var lambdaFound: Lambda? = null
        for (lambdaHook in lambdaList!!) {
            if (lambdaHook.name == actionName) {
                lambdaFound = lambdaHook
                break
            }
        }
        if (lambdaFound == null) {
            Log.e(LOG_TAG, "no lambda found in $lambdaList")
            Toast.makeText(
                    this, "no lambda found", Toast.LENGTH_SHORT
            ).show()
            return
        }
        var event = Event(uuid, lambdaFound, payLoad)
        iEvent!!.sendMessage(event)
    }

    private fun getInitialLambdaList() : ArrayList<Lambda> {
        return dbHelper!!.getLambdas()
    }

    fun requestPermissions() {
        var permissions = ArrayList<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            for (permission in arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INSTALL_PACKAGES,
                    Manifest.permission.INTERNET
                )) {
                Log.d("permission", "permission denied to $permission - requesting it")
                permissions.add(permission)

            }
            requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
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
        private val PERMISSION_REQUEST_CODE = 1
        private val LOG_TAG = "MainActivity"
        private val REQUEST_EDIT_LAMBDA = 1
        private val REQUEST_ADD_LAMBDA = 2
    }
}
