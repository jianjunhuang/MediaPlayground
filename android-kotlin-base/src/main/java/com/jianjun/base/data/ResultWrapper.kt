package com.jianjun.base.data

sealed class ResultWrapper<out T> {
    data class Success<out T>(val data: T) : ResultWrapper<T>()
    data class Error(val code: Int = 0, val msg: String?) : ResultWrapper<Nothing>()
}