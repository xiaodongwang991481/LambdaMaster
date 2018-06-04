package com.example.xiaodong.lambdamaster

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.xiaodongwang.lambdamaster.Lambda
import kotlinx.android.synthetic.main.activity_main.view.*

class DBOpenHelper(
        context: Context?, name: String, factory: SQLiteDatabase.CursorFactory?, version: Int
) : SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        Log.i(LOG_TAG,"create database")
        db?.let {
            db.execSQL(
                    "CREATE TABLE if not exists lambda(name text primary key, package_name text not null)"
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.i(LOG_TAG, "upgrade database")
        db?.let {
            db.execSQL("drop table if exists lambda")
        }
        onCreate(db)
    }

    fun getLambda(name: String) : Lambda? {
        var db = readableDatabase
        db.beginTransaction()
        var lambdaHook: Lambda? = null
        try {
            var cursor = db.query(
                    "lambda", null, "name=?",
                    arrayOf(name), null, null, null
            )
            if (cursor.moveToFirst()) {
                val existingName = cursor.getString(cursor.getColumnIndex("name"))
                val packageName = cursor.getString(cursor.getColumnIndex("package_name"))
                lambdaHook = Lambda(existingName, packageName)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message)
            throw e
        } finally {
            db.endTransaction()
            db.close()
        }
        Log.i(LOG_TAG, "got lambda $lambdaHook")
        return lambdaHook
    }

    fun getLambdas() : ArrayList<Lambda> {
        var lambdas = ArrayList<Lambda>()
        var db = readableDatabase
        db.beginTransaction()
        try {
            var cursor = db.query(
                    "lambda", null, null,
                    null, null, null, null
            )
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val packageName = cursor.getString(cursor.getColumnIndex("package_name"))
                lambdas.add(Lambda(name, packageName))
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message)
            throw e
        } finally {
            db.endTransaction()
            db.close()
        }
        Log.i(LOG_TAG, "get lambdas: $lambdas")
        return lambdas
    }

    fun updateLambda(lambdaHook: Lambda) {
        Log.i(LOG_TAG, "update lambda $lambdaHook")
        var db = writableDatabase
        db.beginTransaction()
        val name = lambdaHook.name
        val packageName = lambdaHook.package_name
        try {
            var cursor = db.query(
                    "lambda", null, "name=?",
                    arrayOf(name), null, null, null
            )
            if (cursor.moveToFirst()) {
                var existingPackageName = cursor.getString(cursor.getColumnIndex("package_name"))
                if (packageName != existingPackageName) {
                    var lambdaContent = ContentValues()
                    lambdaContent.put("package_name", packageName)
                    db.update("lambda", lambdaContent, "name=?", arrayOf(name))
                    Log.d(LOG_TAG, "update lambda $name package from $existingPackageName to $packageName")
                } else {
                    Log.d(LOG_TAG, "lambda $name package $packageName is unchanged")
                }
            } else {
                var lambdaContent = ContentValues()
                lambdaContent.put("name", name)
                lambdaContent.put("package_name", packageName)
                db.insert("lambda", null, lambdaContent)
                Log.d(LOG_TAG, "insert lambda $name package $packageName")
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.i(LOG_TAG, e.message)
            throw e
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun updateLambdas(lambdas: ArrayList<Lambda>) {
        Log.i(LOG_TAG, "update lambdas: $lambdas")
        var db = writableDatabase
        db.beginTransaction()
        try {

            var cursor = db.query(
                    "lambda", null, null,
                    null, null, null, null
            )
            var lambdaMap = HashMap<String, Lambda>()
            var lambdaFound = HashMap<String, Boolean>()
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val packageName = cursor.getString(cursor.getColumnIndex("package_name"))
                lambdaMap.put(name, Lambda(name, packageName))
                lambdaFound.put(name, false)
            }
            for (item in lambdas) {
                val updateName = item.name
                val updatePackageName = item.package_name
                if (updateName in lambdaMap) {
                    val existingLambda = lambdaMap[updateName]
                    val existingPackageName = existingLambda!!.package_name
                    if (updatePackageName != existingPackageName) {
                        var lambdaContent = ContentValues()
                        lambdaContent.put("package_name", updatePackageName)
                        db.update("lambda", lambdaContent, "name=?", arrayOf(updateName))
                        Log.d(
                                LOG_TAG,
                                "update lambda $updateName package from $existingPackageName to $updatePackageName"
                        )
                    } else {
                        Log.d(LOG_TAG, "lambda $updateName package $updatePackageName is unchanged")
                    }
                    lambdaFound[updateName] = true
                } else {
                    var lambdaContent = ContentValues()
                    lambdaContent.put("name", updateName)
                    lambdaContent.put("package_name", updatePackageName)
                    db.insert("lambda", null, lambdaContent)
                    Log.d(LOG_TAG, "insert lambda $updateName package $updatePackageName")
                }
            }
            for ((name, found) in lambdaFound) {
                if (!found) {
                    db.delete("lambda", "name=?", arrayOf(name))
                    Log.d(LOG_TAG, "delete lambda $name")
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.i(LOG_TAG, e.message)
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    companion object {
        private val LOG_TAG = "DBOpenHelper"
    }
}