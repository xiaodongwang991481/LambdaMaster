package com.example.xiaodongwang.lambdamaster

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View

import kotlinx.android.synthetic.main.activity_lambda_edit.*

class LambdaEditActivity : AppCompatActivity() {

    inner class SaveLambda : View.OnClickListener {
        override fun onClick(v: View?) {
            this@LambdaEditActivity.onButtonClickSaveLambda()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lambda_edit)
        if (intent.hasExtra("lambda")) {
            val lambdaHook: Lambda = intent.getParcelableExtra("lambda") as Lambda
            Log.i(LOG_TAG, "get lambda $Lambda")
            var actionName = lambdaHook.name
            var packageName = lambdaHook.package_name
            edit_action_name.setText(actionName)
            edit_action_name.focusable = View.NOT_FOCUSABLE
            edit_action_name.setEnabled(false)
            edit_action_name.setTextColor(Color.GRAY)
            edit_package_name.setText(packageName)
        }
        edit_lambda_save.setOnClickListener(SaveLambda())
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }

        var actionName = edit_action_name.text.toString()
        var packageName = edit_package_name.text.toString()
        Log.i(LOG_TAG, "save state $actionName=$packageName")
        outState?.let {
            outState.putString("name", actionName)
            outState.putString("package_name", packageName)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        var actionName = savedInstanceState?.getString("name") ?: ""
        var packageName = savedInstanceState?.getString("package_name") ?: ""
        Log.i(LOG_TAG, "restore state $actionName=$packageName")
        edit_action_name.setText(actionName)
        edit_package_name.setText(packageName)
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

    fun onButtonClickSaveLambda() {
        Log.i(LOG_TAG, "save lambda")
        if (edit_action_name == null || edit_action_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "action name is empty")
            return
        }
        if (edit_package_name == null || edit_package_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "package name is empty")
            return
        }
        var actionName = edit_action_name.text.toString()
        var packageName = edit_package_name.text.toString()
        val lambdaHook = Lambda(
                name=actionName,
                package_name=packageName
        )
        Log.i(LOG_TAG, "set lambda to $lambdaHook")
        val intent = Intent()
        intent.putExtra("lambda", lambdaHook)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        private val LOG_TAG = "LambdaEditActivity"
    }
}
