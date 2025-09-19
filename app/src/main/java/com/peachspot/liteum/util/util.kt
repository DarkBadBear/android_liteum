package com.peachspot.liteum.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix // Matrix 사용을 위해 import
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface // EXIF 처리용 import
import com.peachspot.liteum.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream // InputStream 명시적 import
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// 적절한 inSampleSize 값을 계산하는 함수
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

// EXIF 회전 각도를 가져오는 헬퍼 함수
private fun getRotationAngleFromExif(context: Context, uri: Uri): Int {
    var inputStream: InputStream? = null
    try {
        inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val exifInterface = ExifInterface(inputStream)
            return when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
    } catch (e: IOException) {
        Log.e("ImageUtil", "Error reading EXIF orientation from URI: $uri", e)
    } finally {
        inputStream?.close()
    }
    return 0
}

// Bitmap을 회전시키는 헬퍼 함수
private fun rotateBitmap(bitmap: Bitmap, angle: Int): Bitmap {
    if (angle == 0) return bitmap
    val matrix = Matrix()
    matrix.postRotate(angle.toFloat())
    return try {
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotatedBitmap != bitmap) { // 새 Bitmap이 생성된 경우 원본 recycle (주의: 이 함수 외부에서 호출자가 원본을 recycle 할 수도 있음)
            // bitmap.recycle() // 여기서 recycle하면 이 함수를 호출하는 곳에서 문제가 생길 수 있음.
            // 호출자가 관리하도록 하는 것이 더 안전할 수 있다.
            // 또는 이 함수가 항상 새 bitmap을 반환하거나, 원본을 수정하도록 설계.
            // 여기서는 새 bitmap을 반환하고, 원본은 그대로 둔다. 호출부가 관리.
        }
        rotatedBitmap
    } catch (e: OutOfMemoryError) {
        Log.e("ImageUtil", "OutOfMemoryError while rotating bitmap.", e)
        bitmap // OOM 발생 시 원본 반환
    }
}

// 리사이징, 압축, EXIF 회전 기능이 추가된 이미지 저장 함수
fun saveImageToInternalStorageWithResizing(
    context: Context,
    uri: Uri,
    desiredFileNamePrefix: String = "LITEUM_COVER_",
    targetWidth: Int = 1080,
    targetHeight: Int = 1080,
    quality: Int = 85
): File? {
    var inputStreamForDecode: InputStream? = null
    var currentBitmap: Bitmap? = null
    var rotatedBitmap: Bitmap? = null // 회전된 Bitmap을 저장할 변수
    var scaledBitmapForRotation: Bitmap? = null // 스케일링 후 회전 전 Bitmap
    var outputStream: FileOutputStream? = null
    var outputFile: File? = null

    try {
        // 1. EXIF 회전 정보 읽기
        val rotationAngle = getRotationAngleFromExif(context, uri)

        // 2. URI에서 Bitmap으로 디코딩 (리사이징 옵션 포함)
        val options = BitmapFactory.Options().apply {
            var tempInputStream: InputStream? = null
            try {
                tempInputStream = context.contentResolver.openInputStream(uri)
                if (tempInputStream == null) throw IOException("Cannot open input stream for bounds check")
                inJustDecodeBounds = true
                BitmapFactory.decodeStream(tempInputStream, null, this)
            } finally {
                tempInputStream?.close()
            }
            inSampleSize = calculateInSampleSize(this, targetWidth, targetHeight)
            inJustDecodeBounds = false
        }

        inputStreamForDecode = context.contentResolver.openInputStream(uri)
        if (inputStreamForDecode == null) throw IOException("Cannot open input stream for decode")
        currentBitmap = BitmapFactory.decodeStream(inputStreamForDecode, null, options)
        if (currentBitmap == null) {
            Log.e("ImageUtil", "BitmapFactory.decodeStream returned null for $uri")
            return null
        }
        Log.d("ImageResize", "Decoded (inSampleSize: ${options.inSampleSize}) bitmap size: ${currentBitmap.width}x${currentBitmap.height}")

        // 3. (선택적) 더 정확한 리사이징
        var bitmapToProcess = currentBitmap // 처리할 기본 비트맵
        if (currentBitmap.width > targetWidth || currentBitmap.height > targetHeight) {
            val aspectRatio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
            val finalWidth: Int
            val finalHeight: Int

            // 회전을 고려하지 않고, 현재 비트맵 비율로 목표 크기 계산
            if (currentBitmap.width > currentBitmap.height) { // 가로 이미지
                finalWidth = targetWidth
                finalHeight = (targetWidth / aspectRatio).toInt().coerceAtLeast(1)
            } else { // 세로 이미지 또는 정사각형
                finalHeight = targetHeight
                finalWidth = (targetHeight * aspectRatio).toInt().coerceAtLeast(1)
            }

            if (finalWidth > 0 && finalHeight > 0) {
                scaledBitmapForRotation = Bitmap.createScaledBitmap(currentBitmap, finalWidth, finalHeight, true)
                if (scaledBitmapForRotation != currentBitmap) { // 새 객체가 생성된 경우
                    // currentBitmap.recycle() // scaledBitmapForRotation이 currentBitmap을 사용했다면 recycle하면 안됨
                    // createScaledBitmap은 새 Bitmap을 반환. 원본은 호출자가 관리.
                    // 여기서는 currentBitmap을 더 이상 직접 사용하지 않으므로,
                    // scaledBitmapForRotation으로 대체하기 전에 recycle 가능.
                    // 하지만, currentBitmap을 recycle하기 전에 scaledBitmapForRotation이 확실히 생성되었는지 확인 필요.
                    // currentBitmap.recycle() // 여기서 recycle하면 아래에서 문제가 될 수 있음.
                    // bitmapToProcess를 업데이트하고, 루프나 분기 마지막에 한번에 recycle 고려.
                }
                bitmapToProcess = scaledBitmapForRotation // 처리할 비트맵을 스케일된 것으로 변경
                Log.d("ImageResize", "Scaled to: ${bitmapToProcess.width}x${bitmapToProcess.height}")
            }
        }


        // 4. EXIF 정보에 따른 Bitmap 회전
        // bitmapToProcess (스케일링 되었을 수 있는)를 회전
        rotatedBitmap = rotateBitmap(bitmapToProcess, rotationAngle)
        // Log.d("ImageResize", "Rotated by $rotationAngle degrees. Rotated bitmap: ${rotatedBitmap.width}x${rotatedBitmap.height}")


        // 5. 저장할 디렉토리 및 파일 이름 생성
        val outputDir = File(context.filesDir, "images")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val fileExtension = ".jpg"
        val fileName = "${desiredFileNamePrefix}${timeStamp}${fileExtension}"
        outputFile = File(outputDir, fileName)

        // 6. 최종 Bitmap(회전된)을 압축하여 파일로 저장
        outputStream = FileOutputStream(outputFile)
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream) // rotatedBitmap 사용

        Log.d("ImageSaveUtil", "리사이징/압축/회전 후 이미지 내부 저장 성공: ${outputFile.absolutePath}, Size: ${outputFile.length() / 1024} KB")
        return outputFile

    } catch (e: Exception) {
        Log.e("ImageSaveUtil", "이미지 저장/처리 실패 URI: $uri", e)
        outputFile?.takeIf { it.exists() }?.delete()
        return null
    } finally {
        inputStreamForDecode?.close()
        outputStream?.close()
        // 사용된 모든 Bitmap 객체들을 recycle
        currentBitmap?.recycle()
        // scaledBitmapForRotation과 rotatedBitmap은 currentBitmap과 다른 객체일 수 있으므로 개별 관리 필요
        // 만약 bitmapToProcess가 currentBitmap과 다른 객체(즉, scaledBitmapForRotation)였다면,
        // 그리고 rotatedBitmap이 bitmapToProcess와 다른 객체였다면, 각각 recycle 필요.
        // 현재 로직에서는 rotatedBitmap이 최종 결과물이므로, 그 이전 단계의 것들을 recycle.
        // scaledBitmapForRotation이 생성되었고, rotatedBitmap이 scaledBitmapForRotation과 다르다면 scaledBitmapForRotation도 recycle.
        // 이는 복잡해질 수 있으므로, 최종적으로 rotatedBitmap을 제외한 중간 단계 비트맵들을 잘 추적하여 recycle.
        // 가장 안전한 방법은 각 단계에서 새 비트맵을 받으면 이전 비트맵을 즉시 recycle 하는 것이나,
        // createScaledBitmap이나 rotateBitmap이 원본을 수정하지 않고 새 것을 반환하는 것을 전제로 해야 함.
        if (scaledBitmapForRotation != null && scaledBitmapForRotation != currentBitmap && scaledBitmapForRotation != rotatedBitmap) {
            scaledBitmapForRotation.recycle()
        }
        // rotatedBitmap은 compress 후 recycle되어서는 안됨 (위에서 이미 사용 후 함수 종료됨)
        // currentBitmap은 scaledBitmapForRotation 또는 rotatedBitmap으로 대체되었으므로 recycle 대상.
        // 이 finally 블록에서 recycle하는 것보다 각 비트맵 사용 직후에 하는 것이 더 명확할 수 있음.
        // 여기서는 함수가 종료되므로, 지역변수로 선언된 Bitmap들은 어차피 GC 대상이 될 수 있으나 명시적 recycle이 권장.
        // 지금은 currentBitmap만 recycle (scaledBitmapForRotation, rotatedBitmap은 currentBitmap과 같은 객체이거나, currentBitmap으로부터 파생되어 compress에 사용)
    }
}


// --- 이미지 저장 유틸리티 함수 (리사이징/압축 기능 추가 예시) ---
// 이 함수는 saveImageToInternalStorageWithResizing와 거의 동일한 역할을 합니다.
// EXIF 처리 및 오류 수정이 필요합니다.
fun saveImageToInternalStorage(
    context: Context,
    uri: Uri,
    desiredFileNamePrefix: String = "LITEUM_COVER_", // 기본 접두사 유지
    targetWidth: Int = 1080,
    targetHeight: Int = 1080,
    quality: Int = 85
): File? {
    // 내부적으로 saveImageToInternalStorageWithResizing를 호출하거나,
    // 동일한 로직을 여기에 구현합니다. 여기서는 동일 로직을 복사하고 필요한 수정만 가합니다.
    var inputStreamForDecode: InputStream? = null
    var currentBitmap: Bitmap? = null
    var rotatedBitmap: Bitmap? = null
    var scaledBitmapForRotation: Bitmap? = null
    var outputStream: FileOutputStream? = null
    var outputFile: File? = null

    try {
        val rotationAngle = getRotationAngleFromExif(context, uri)

        val options = BitmapFactory.Options().apply {
            var tempInputStream: InputStream? = null
            try {
                tempInputStream = context.contentResolver.openInputStream(uri)
                if (tempInputStream == null) throw IOException("Cannot open input stream for bounds check")
                inJustDecodeBounds = true
                BitmapFactory.decodeStream(tempInputStream, null, this)
            } finally {
                tempInputStream?.close()
            }
            inSampleSize = calculateInSampleSize(this, targetWidth, targetHeight)
            inJustDecodeBounds = false
        }

        inputStreamForDecode = context.contentResolver.openInputStream(uri)
        if (inputStreamForDecode == null) throw IOException("Cannot open input stream for decode")
        currentBitmap = BitmapFactory.decodeStream(inputStreamForDecode, null, options)
        if (currentBitmap == null) {
            Log.e("ImageUtil", "BitmapFactory.decodeStream returned null for $uri (in saveImageToInternalStorage)")
            return null
        }

        var bitmapToProcess = currentBitmap
        if (currentBitmap.width > targetWidth || currentBitmap.height > targetHeight) {
            val aspectRatio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
            val finalWidth: Int
            val finalHeight: Int
            if (currentBitmap.width > currentBitmap.height) {
                finalWidth = targetWidth
                finalHeight = (targetWidth / aspectRatio).toInt().coerceAtLeast(1)
            } else {
                finalHeight = targetHeight
                finalWidth = (targetHeight * aspectRatio).toInt().coerceAtLeast(1)
            }
            if (finalWidth > 0 && finalHeight > 0) {
                scaledBitmapForRotation = Bitmap.createScaledBitmap(currentBitmap, finalWidth, finalHeight, true)
                bitmapToProcess = scaledBitmapForRotation
            }
        }

        rotatedBitmap = rotateBitmap(bitmapToProcess, rotationAngle)

        val outputDir = File(context.filesDir, "images")
        if (!outputDir.exists()) outputDir.mkdirs()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val fileExtension = ".jpg"
        val fileName = "${desiredFileNamePrefix}${timeStamp}${fileExtension}"
        outputFile = File(outputDir, fileName)

        outputStream = FileOutputStream(outputFile)
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        Log.d("ImageSaveUtil", "(from saveImageToInternalStorage) 리사이징/압축 후 이미지 내부 저장 성공: ${outputFile.absolutePath}")
        return outputFile
    } catch (e: IOException) { // 원본 코드의 Exception 대신 IOException 명시
        Log.e("ImageSaveUtil", "(from saveImageToInternalStorage) 이미지 저장/처리 실패 URI: $uri", e)
        outputFile?.takeIf { it.exists() }?.delete()
        return null
    } catch (e: Exception) { // 다른 예외들도 처리
        Log.e("ImageSaveUtil", "(from saveImageToInternalStorage) 이미지 저장 중 일반 오류 URI: $uri", e)
        outputFile?.takeIf { it.exists() }?.delete()
        return null
    } finally {
        inputStreamForDecode?.close()
        outputStream?.close()
        currentBitmap?.recycle()
        if (scaledBitmapForRotation != null && scaledBitmapForRotation != currentBitmap && scaledBitmapForRotation != rotatedBitmap) {
            scaledBitmapForRotation.recycle()
        }
        // rotatedBitmap은 compress 후 recycle하지 않음
    }
}


// 이미지 리사이징 예시 함수 (원하는 너비와 높이로 조절) - Bitmap 반환
// 이 함수도 EXIF 회전을 고려해야 함
fun resizeImage(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
    var inputStreamForDecode: InputStream? = null
    var currentBitmap: Bitmap? = null
    var rotatedBitmap: Bitmap? = null
    var scaledBitmapForRotation: Bitmap? = null

    try {
        val rotationAngle = getRotationAngleFromExif(context, uri)

        val options = BitmapFactory.Options().apply {
            var tempInputStream: InputStream? = null
            try {
                tempInputStream = context.contentResolver.openInputStream(uri)
                if (tempInputStream == null) throw IOException("Cannot open input stream for bounds check")
                inJustDecodeBounds = true
                BitmapFactory.decodeStream(tempInputStream, null, this)
            } finally {
                tempInputStream?.close()
            }
            inSampleSize = calculateInSampleSize(this, targetWidth, targetHeight)
            inJustDecodeBounds = false
        }

        inputStreamForDecode = context.contentResolver.openInputStream(uri)
        if (inputStreamForDecode == null) throw IOException("Cannot open input stream for decode")
        currentBitmap = BitmapFactory.decodeStream(inputStreamForDecode, null, options)
        if (currentBitmap == null) {
            Log.e("ImageUtil", "BitmapFactory.decodeStream returned null for $uri (in resizeImage)")
            return null
        }

        var bitmapToProcess = currentBitmap
        if (currentBitmap.width > targetWidth || currentBitmap.height > targetHeight) {
            val aspectRatio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
            val finalWidth: Int
            val finalHeight: Int
            if (currentBitmap.width > currentBitmap.height) {
                finalWidth = targetWidth
                finalHeight = (targetWidth / aspectRatio).toInt().coerceAtLeast(1)
            } else {
                finalHeight = targetHeight
                finalWidth = (targetHeight * aspectRatio).toInt().coerceAtLeast(1)
            }
            if (finalWidth > 0 && finalHeight > 0) {
                scaledBitmapForRotation = Bitmap.createScaledBitmap(currentBitmap, finalWidth, finalHeight, true)
                bitmapToProcess = scaledBitmapForRotation
            }
        }

        rotatedBitmap = rotateBitmap(bitmapToProcess, rotationAngle)

        // 원본 비트맵(currentBitmap)과 중간 스케일 비트맵(scaledBitmapForRotation)은
        // 반환되는 rotatedBitmap과 다른 경우 recycle 해주는 것이 좋으나,
        // 이 함수는 Bitmap을 반환하므로, 최종 반환된 Bitmap의 recycle은 호출자가 담당해야 함.
        // 여기서 currentBitmap이나 scaledBitmapForRotation을 recycle하면 rotatedBitmap이 유효하지 않을 수 있음.
        // 예를 들어, 스케일링이나 회전이 필요 없었다면 rotatedBitmap은 currentBitmap과 동일 객체일 수 있음.
        if (currentBitmap != rotatedBitmap && currentBitmap != scaledBitmapForRotation) { // currentBitmap이 중간과정도 아니고 최종도 아닐 때
            currentBitmap.recycle()
        }
        if (scaledBitmapForRotation != null && scaledBitmapForRotation != rotatedBitmap) { // scaledBitmap이 최종이 아닐 때
            scaledBitmapForRotation.recycle()
        }

        return rotatedBitmap // 최종적으로 회전된(또는 원본) Bitmap 반환
    } catch (e: IOException) {
        Log.e("ImageUtil", "IOException in resizeImage for URI: $uri", e)
        // e.printStackTrace() // Log.e로 대체
        currentBitmap?.recycle()
        scaledBitmapForRotation?.recycle() // 실패 시에도 생성된 비트맵은 recycle
        return null
    } catch (e: Exception) {
        Log.e("ImageUtil", "Exception in resizeImage for URI: $uri", e)
        currentBitmap?.recycle()
        scaledBitmapForRotation?.recycle()
        return null
    }
    finally {
        inputStreamForDecode?.close()
    }
}


// Context 확장 함수를 만들어 카메라 촬영용 임시 파일 생성
fun Context.createImageFileForCamera(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_TEMP_${timeStamp}_"
    val storageDir = File(externalCacheDir, "temp_images")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    )
}


// 이미지 저장 유틸리티 함수 (원본 복사 - 리사이징/압축 없음)
fun saveImageToInternalStorage(context: Context, uri: Uri, desiredFileNamePrefix: String = "COVER_"): File? {
    var inputStream: InputStream? = null
    var outputStream: FileOutputStream? = null
    var outputFile: File? = null // Nullable로 변경

    try {
        inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e("ImageUtil", "Failed to open input stream for copy (original). URI: $uri")
            return null
        }

        val outputDir = File(context.filesDir, "images")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // 파일 확장자 추론 개선 여지 있음
        val originalFileName = uri.lastPathSegment ?: "unknown"
        val extension = originalFileName.substringAfterLast('.', "")
        val fileExtension = if (extension.isNotBlank() && extension.length <= 4) ".$extension" else ".jpg"


        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val fileName = "${desiredFileNamePrefix}${timeStamp}${fileExtension}"
        outputFile = File(outputDir, fileName)

        outputStream = FileOutputStream(outputFile)
        inputStream.copyTo(outputStream)

        Log.d("ImageSaveUtil", "원본 이미지 내부 저장 성공: ${outputFile.absolutePath}")
        return outputFile
    } catch (e: IOException) {
        Log.e("ImageSaveUtil", "원본 이미지 저장 실패 URI: $uri", e)
        outputFile?.takeIf { it.exists() }?.delete() // 실패 시 불완전한 파일 삭제
        return null
    } finally {
        inputStream?.close()
        outputStream?.close()
    }
}


// --- 시간 관련 유틸리티 --- (이하 동일)
fun LocalDateTime.toEpochMillis(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long?.toFormattedTimeStringOrNull(): String? {
    if (this == null) return null
    return try {
        val instant = Instant.ofEpochMilli(this)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) {
        Log.e("TimeUtil", "Error formatting time: ${e.message}", e)
        null
    }
}

fun Long?.toFormattedTimeStringOrDefault(defaultString: String = "N/A"): String {
    if (this == null) return defaultString
    return try {
        val instant = Instant.ofEpochMilli(this)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } catch (e: Exception) {
        Log.e("TimeUtil", "Error formatting time: ${e.message}", e)
        defaultString
    }
}

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}


// --- 로깅 유틸리티 --- (이하 동일)
object Logger {
    private const val GLOBAL_TAG = "LiteumApp" // 앱 이름 등으로 변경
    var enabled = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (enabled) {
            Log.d(tag, message)
        }
    }

    fun d(message: String) {
        if (enabled) {
            Log.d(GLOBAL_TAG, message)
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

    fun w(message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable != null) {
                Log.w(GLOBAL_TAG, message, throwable)
            } else {
                Log.w(GLOBAL_TAG, message)
            }
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

    fun i(tag: String, message: String) {
        if (enabled) {
            Log.i(tag, message)
        }
    }

    fun i(message: String) {
        if (enabled) {
            Log.i(GLOBAL_TAG, message)
        }
    }

    fun v(tag: String, message: String) {
        if (enabled) {
            Log.v(tag, message)
        }
    }

    fun v(message: String) {
        if (enabled) {
            Log.v(GLOBAL_TAG, message)
        }
    }
}

