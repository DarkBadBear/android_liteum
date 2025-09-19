package com.peachspot.liteum.data.repositiory


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

object UserPreferencesKeys {
    val USER_GOOGLE_ID = stringPreferencesKey("user_google_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
    val FIREBASE_UID = stringPreferencesKey("firebase_uid")
    val AGREE = booleanPreferencesKey("agree")
    val SHOW_ONLY_MY_REVIEWS = booleanPreferencesKey("show_only_my_reviews")
}

data class UserProfileData(
    val googleId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val firebaseUid: String? = null,// Firebase UID 필드 추가
    val weight: Float? = null
)

// 이 데이터 클래스는 현재 UserPreferencesRepository에서 직접 사용되지 않지만,
// 필요하다면 여러 설정을 묶어서 하나의 Flow로 노출할 때 사용할 수 있습니다.
// data class UserSettings(
// val floorHeight: Float,
// val floorCount: Int
// )

open class UserPreferencesRepository(private val context: Context) {

    private fun Flow<Preferences>.catchIOExceptionAndEmitEmptyPreferences(): Flow<Preferences> =
        catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }


    // "내것만 보기" 설정 값을 Flow로 읽어옴
    val showOnlyMyReviewsFlow: Flow<Boolean> = context.userSettingsDataStore.data
        .catchIOExceptionAndEmitEmptyPreferences()
        .map { preferences ->
            // 키가 존재하지 않으면 기본값 false 반환
            preferences[UserPreferencesKeys.SHOW_ONLY_MY_REVIEWS] ?: false
        }

    // "내것만 보기" 설정 값을 업데이트
    suspend fun updateShowOnlyMyReviews(showOnlyMyReviews: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.SHOW_ONLY_MY_REVIEWS] = showOnlyMyReviews
        }
    }



    val agreeFlow: Flow<Boolean?> = context.userSettingsDataStore.data
        .catchIOExceptionAndEmitEmptyPreferences()
        .map { preferences ->
            preferences[UserPreferencesKeys.AGREE]
        }


    suspend fun saveAgree(flag: Boolean) {
        context.userSettingsDataStore.edit { settings ->
            settings[UserPreferencesKeys.AGREE] = flag
        }
    }




    val userProfileDataFlow: Flow<UserProfileData> = context.userSettingsDataStore.data
        .catchIOExceptionAndEmitEmptyPreferences()
        .map { preferences ->
            UserProfileData(
                googleId = preferences[UserPreferencesKeys.USER_GOOGLE_ID],
                name = preferences[UserPreferencesKeys.USER_NAME],
                email = preferences[UserPreferencesKeys.USER_EMAIL],
                photoUrl = preferences[UserPreferencesKeys.USER_PHOTO_URL],
                firebaseUid = preferences[UserPreferencesKeys.FIREBASE_UID] // Firebase UID 매핑 추가

            )
        }


    suspend fun saveUserProfileData(profile: UserProfileData) {
        context.userSettingsDataStore.edit { settings ->
            profile.googleId?.let {
                settings[UserPreferencesKeys.USER_GOOGLE_ID] = it
            } ?: settings.remove(UserPreferencesKeys.USER_GOOGLE_ID)

            profile.name?.let { settings[UserPreferencesKeys.USER_NAME] = it }
                ?: settings.remove(UserPreferencesKeys.USER_NAME)

            profile.email?.let { settings[UserPreferencesKeys.USER_EMAIL] = it }
                ?: settings.remove(UserPreferencesKeys.USER_EMAIL)

            profile.photoUrl?.let {
                settings[UserPreferencesKeys.USER_PHOTO_URL] = it
            } ?: settings.remove(UserPreferencesKeys.USER_PHOTO_URL)
            profile.firebaseUid?.let {
                settings[UserPreferencesKeys.FIREBASE_UID] = it
            } // Firebase UID 저장 추가
                ?: settings.remove(UserPreferencesKeys.FIREBASE_UID)
        }
    }

    suspend fun clearUserProfileData() {
        context.userSettingsDataStore.edit { settings ->
            settings.remove(UserPreferencesKeys.USER_GOOGLE_ID)
            settings.remove(UserPreferencesKeys.USER_NAME)
            settings.remove(UserPreferencesKeys.USER_EMAIL)
            settings.remove(UserPreferencesKeys.USER_PHOTO_URL)

        }
    }


    suspend fun clearFirebaseUid() {
        context.userSettingsDataStore.edit { settings ->
            settings.remove(UserPreferencesKeys.FIREBASE_UID)
        }
    }

    suspend fun hasStoredFirebaseUid(): Boolean {
        return userProfileDataFlow
            .map { it.firebaseUid != null && it.firebaseUid.isNotBlank() }
            .first() // Flow에서 첫 번째 값(현재 상태)만 가져옴
    }


    // --- ExerciseViewModel 관련 DataStore 함수들 ---



}