package com.peachspot.smartkofarm.util



import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {

    // DateTimeFormatter는 LocalDate <-> String 변환에만 사용됩니다.
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    // --- LocalDate <-> String 변환 ---
    // 이 메서드 쌍을 사용하여 LocalDate를 데이터베이스에 String으로 저장하고 읽어옵니다.

    @TypeConverter
    fun fromLocalDateToString(date: LocalDate?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun fromStringToLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            // 제공된 문자열을 ISO_LOCAL_DATE 형식으로 파싱하여 LocalDate 객체를 생성합니다.
            LocalDate.parse(it, formatter)
        }
    }

    // --- LocalDate <-> Long (Epoch Day) 변환 (주석 처리 또는 삭제) ---
    // 아래 메서드 쌍은 LocalDate를 Long 타입으로 저장할 때 사용되지만,
    // 현재 String 저장 방식을 선택했으므로 주석 처리하거나 삭제합니다.
    // 만약 애플리케이션 전체적으로 Long 저장을 선택한다면 아래를 활성화하고 위의 String 변환을 주석 처리합니다.
    /*
    @TypeConverter
    fun toDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun fromDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
    */

    // 여기에 List<Long> <-> String (JSON) 등 다른 필요한 TypeConverter들을 추가할 수 있습니다.
    // 예를 들어, TimestampListConverter의 내용을 여기에 통합하거나 별도 클래스로 유지할 수 있습니다.
    // 예시:
    // @TypeConverter
    // fun fromTimestampList(timestamps: List<Long>?): String? {
    //     return timestamps?.joinToString(",") // 간단한 예시, 실제로는 JSON 라이브러리 사용 권장
    // }
    //
    // @TypeConverter
    // fun toTimestampList(data: String?): List<Long>? {
    //     return data?.split(',')?.mapNotNull { it.toLongOrNull() }
    // }
}