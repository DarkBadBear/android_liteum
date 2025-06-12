package com.peachspot.smartkofarm.util


import android.util.Log

class TimestampListConverter {
    private val TAG = "TimestampListConverter"

    @androidx.room.TypeConverter
    fun fromString(value: String?): List<Long>? {
        if (value.isNullOrEmpty()) {
            return emptyList() // 또는 null을 반환하도록 선택
        }
        return value.split(",").mapNotNull { timestampString ->
            try {
                timestampString.trim().toLong()
            } catch (e: NumberFormatException) {
                // 데이터베이스에서 읽어온 문자열이 Long으로 변환 불가능한 경우
                // 로그를 남기거나, 오류 처리를 하거나, null을 반환하여 해당 항목을 제외할 수 있습니다.
                // 현재는 mapNotNull에 의해 null이 반환되어 제외됩니다.
                // 엄격한 처리가 필요하다면 여기서 예외를 다시 던지거나 기본값을 사용할 수 있습니다.
                Log.e(TAG, "Failed to convert '$timestampString' to Long. It will be excluded.", e)
                null
            }
        }
    }

    @androidx.room.TypeConverter
    fun fromList(list: List<Long>?): String? {
        // 리스트가 null이거나 비어있으면 빈 문자열을 반환할 수도 있습니다.
        // Room은 null 컬럼을 허용하므로, list가 null일 때 null을 반환하는 현재 방식도 유효합니다.
        // 만약 DB에 항상 문자열 (비어 있더라도)을 저장하고 싶다면 아래와 같이 수정:
        // return list?.joinToString(",") ?: "" 캐시 삭제 방법은 다 해봤는데 안된다.
        return list?.joinToString(",")
    }
}