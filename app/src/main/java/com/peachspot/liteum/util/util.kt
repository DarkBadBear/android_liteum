package com.peachspot.liteum.util


// androidx.core.i18n.DateTimeFormatter를 사용하므로, 해당 의존성이 필요합니다.
// import androidx.core.i18n.DateTimeFormatter as AndroidXDateTimeFormatter // 별칭 사용 (선택 사항)

// java.time.format.DateTimeFormatter를 사용하지 않는다면 아래 임포트는 필요 없습니다.
// import java.time.format.DateTimeFormatter
import android.util.Log
import com.peachspot.liteum.BuildConfig

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
        // androidx.core.i18n.DateTimeFormatter 사용
        // 만약 java.time.DateTimeFormatter와 혼동을 피하고 싶다면 별칭(alias) 사용 가능
        // localTime.format(AndroidXDateTimeFormatter.ofPattern("HH:mm:ss"))
        localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) { // 보다 구체적인 예외를 잡는 것이 좋지만, 여기서는 포괄적으로 처리
        // 실제 앱에서는 Logcat에만 출력하는 것보다 로깅 라이브러리 사용을 권장합니다.
        Log.e("TimeUtil", "Error formatting time: ${e.message}", e)
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
        // androidx.core.i18n.DateTimeFormatter 사용
        localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) {
        Log.e("TimeUtil", "Error formatting time: ${e.message}", e)
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