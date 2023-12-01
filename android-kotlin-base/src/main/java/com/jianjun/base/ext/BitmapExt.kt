package com.jianjun.base.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

fun Uri.toBitmap(context: Context): Bitmap {
    val ins = context.contentResolver.openInputStream(this)
    return BitmapFactory.decodeStream(ins)
}