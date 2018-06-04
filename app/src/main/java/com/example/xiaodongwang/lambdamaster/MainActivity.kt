package com.example.xiaodongwang.lambdamaster

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.example.xiaodong.lambdamaster.DBOpenHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

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
            Log.i(LOG_TAG, "item $position long click on $lambdaHook")
            this@MainActivity.onButtonClickEditLambda(lambdaHook!!)
        }
    }

    inner class SendMessage : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickSendMessage()
        }
    }

    private var dbHelper: DBOpenHelper? = null
    private var lambdaList: ArrayList<Lambda>? = null
    private var lambdasAdapter: LambdaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestPermissions()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_service.setOnClickListener(StartService())
        stop_service.setOnClickListener(StopService())
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        lambdaList = getInitialLambdaList()
        lambdasAdapter = LambdaAdapter(
                this, lambdaList!!
        )
        var header = layoutInflater.inflate(R.layout.lambda_header, lambdas, false)
        lambdas.addHeaderView(header)
        var footer = layoutInflater.inflate(R.layout.listview_footer, lambdas, false)
        lambdas.addFooterView(footer)
        lambdas.setAdapter(lambdasAdapter!!)
        lambdas.smoothScrollToPosition(0, 0)
        add_lambda.setOnClickListener(AddLambda())
        lambdas.setOnItemClickListener(EditLambda())
        save_lambdas.setOnClickListener(SaveLambdas())
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

    fun onButtonClickSendMessage() {
        if (action_name == null || action_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "action name is empty")
            return
        }
        var actionName = action_name.text.toString()
        if (lambdaList == null) {
            Log.e(LOG_TAG, "empty lambda list")
            return
        }
        var lambdaFound: Lambda? = null
        for (lambdaHook in lambdaList!!) {
            if (lambdaHook.name == actionName) {
                lambdaFound = lambdaHook
                break
            }
        }
        if (lambdaFound == null) {
            Log.e(LOG_TAG, "no lambda found in $lambdaList")
            return
        }
        var payLoad = ""
        if (payload != null && !payload.text.isNullOrBlank()) {
            payLoad = payload.text.toString()
        }
        var uuid = UUID.randomUUID().toString()
        var event = Event(uuid, lambdaFound!!, payLoad)
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
