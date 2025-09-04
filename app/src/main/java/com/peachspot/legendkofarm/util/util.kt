package com.peachspot.legendkofarm.util

import android.util.Log
import com.peachspot.legendkofarm.BuildConfig

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun LocalDateTime.toEpochMillis(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * Long 타입의 Epoch Milliseconds를 "HH:mm:ss" 형식의 시간 문자열로 변환합니다.
 * 변환에 실패하거나 Long 값이 null이면 null을 반환합니다.
 *
 * @return 포맷팅된 시간 문자열 또는 null.
 */
fun Long?.toFormattedTimeStringOrNull(): String? {
    if (this == null) return null

    return try {
        val instant = Instant.ofEpochMilli(this)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) {
        Logger.e("TimeUtil", "Error formatting time: ${e.message}", e)
        null
    }
}

/**
 * Long 타입의 Epoch Milliseconds를 "HH:mm:ss" 형식의 시간 문자열로 변환합니다.
 * 변환에 실패하거나 Long 값이 null이면 지정된 기본 문자열(기본값: "N/A")을 반환합니다.
 *
 * @param defaultString 변환 실패 또는 null 입력 시 반환할 기본 문자열.
 * @return 포맷팅된 시간 문자열 또는 기본 문자열.
 */
fun Long?.toFormattedTimeStringOrDefault(defaultString: String = "N/A"): String {
    if (this == null) return defaultString

    return try {
        val instant = Instant.ofEpochMilli(this)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) {
        Logger.e("TimeUtil", "Error formatting time: ${e.message}", e)
        defaultString
    }
}

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}



object Logger {
    private const val GLOBAL_TAG = "MyAppTag"
    var enabled = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (enabled) {
            Log.d(tag, message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }

    fun d(message: String) {
        if (enabled) {
            Log.d(GLOBAL_TAG, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable != null) {
                Log.e(GLOBAL_TAG, message, throwable)
            } else {
                Log.e(GLOBAL_TAG, message)
            }
        }
    }

    // 필요하면 i(), v() 등도 추가 가능
}