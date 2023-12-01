package com.jianjun.base.compontent

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.use
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

abstract class BaseActivity : AppCompatActivity() {

    private inline fun <reified T : ViewModel> AppCompatActivity.viewModels() = lazy {
        ViewModelProvider(this).get(T::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun isTransparent(): Boolean {
        theme.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.windowIsTranslucent,
                android.R.attr.windowIsFloating
            )
        ).use {
            return it.getBoolean(0, false) || it.getBoolean(1, false)
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            if (it is BaseFragment && it.onBackPressed()) {

                return
            }
        }
        super.onBackPressed()
    }

    //Fix Build.VERSION_CODES.O: java.lang.IllegalStateException: Only fullscreen opaque activities can request orientation
    private fun isNeedToFixOrientation(): Boolean {
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.O || Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) && isTransparent()) {
            return true
        }
        return false
    }

    override fun setRequestedOrientation(requestedOrientation: Int) {
        if (isNeedToFixOrientation()) {
            return
        }
        super.setRequestedOrientation(requestedOrientation)
    }
}