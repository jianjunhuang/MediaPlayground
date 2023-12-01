package com.jianjun.base.utils

import android.os.Looper
import android.os.MessageQueue
import java.util.*

object IdleHandlerUtils {

    private val tasker = IdleHandlerTasker()

    init {
        Looper.myQueue().addIdleHandler(tasker)
    }

    fun addTask(task: Runnable) {
        tasker.tasks.add(task)
    }

    class IdleHandlerTasker : MessageQueue.IdleHandler {
        val tasks = ArrayDeque<Runnable>()
        override fun queueIdle(): Boolean {
            if (tasks.isNotEmpty()) {
                tasks.pop().run()
            }
            return true
        }

    }
}