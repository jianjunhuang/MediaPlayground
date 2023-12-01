package com.jianjun.base.utils.crash

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteFullException
import androidx.core.app.ActivityManagerCompat

/**
 * catch the Uncaught exception, exit instead of crash
 */
object CrashManager : Thread.UncaughtExceptionHandler {

    private var app: Application? = null
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun register(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!handle(t, e)) {
            defaultHandler?.uncaughtException(t, e)
        }
    }

    private fun handle(t: Thread, e: Throwable): Boolean {
        return iteratorException(e)
    }

    private fun iteratorException(e: Throwable?): Boolean {
        if (e == null) return false
        return if (check(e)) {
            return true
        } else {
            iteratorException(e.cause)
        }
    }

    private fun check(e: Throwable): Boolean {
        return when (e) {
            is ExternalCacheException, is SQLiteFullException -> {
//                Toast.makeText(app, "缓存空间不足, 无法正常使用.", Toast.LENGTH_LONG).show()
                finishAll()
                true
            }
            else -> false
        }
    }

    private fun finishAll() {
        val activityManager =
            app?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appTaskList = activityManager.appTasks
        for (appTask in appTaskList) {
            appTask.finishAndRemoveTask()
        }
        System.exit(0)
    }
}