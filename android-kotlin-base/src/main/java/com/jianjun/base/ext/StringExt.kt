package com.jianjun.base.ext

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

val String.md5: String?
    get() {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(this.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

fun String.firstOrEmpty(): String {
    return this.firstOrNull()?.toString() ?: ""
}