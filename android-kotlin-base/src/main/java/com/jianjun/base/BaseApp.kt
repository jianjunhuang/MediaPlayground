package com.jianjun.base

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.jianjun.base.utils.crash.CrashManager

open class BaseApp : MultiDexApplication() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        globalApp = this
    }

    override fun onCreate() {
        super.onCreate()
        CrashManager.register(this)
    }

    companion object {
        private lateinit var globalApp: BaseApp
        fun getApp() = globalApp
    }
}