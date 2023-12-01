package com.jianjun.base.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDate(date: Date): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH)
        return dateFormatter.format(date)
    }
}