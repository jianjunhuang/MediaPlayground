package com.jianjun.base.utils.crash

import java.lang.RuntimeException


/**
 * if [context.getExternalDir] is null, throw this Exception
 */
class ExternalCacheException : RuntimeException("ExternalCache is null") {
}