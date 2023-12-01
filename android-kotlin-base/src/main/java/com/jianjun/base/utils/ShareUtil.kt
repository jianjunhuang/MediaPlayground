package com.jianjun.base.utils

import android.content.Context
import android.content.Intent

object ShareUtil {

    fun shareText(context: Context, text: String) {
        context.startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"

        }, null))
    }
}