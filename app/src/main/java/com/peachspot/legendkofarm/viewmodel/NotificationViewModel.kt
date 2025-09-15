package com.peachspot.legendkofarm.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.peachspot.legendkofarm.data.db.AppDatabase
import com.peachspot.legendkofarm.data.db.NotificationEntity

import com.peachspot.legendkofarm.data.repositiory.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationRepository: NotificationRepository

    init {
        val notificationDao = AppDatabase.getInstance(application).notificationDao()
        notificationRepository = NotificationRepository(notificationDao)
    }

    val notifications: StateFlow<List<NotificationEntity>> =
        notificationRepository.getAllNotifications()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // 5초 동안 구독자가 없으면 Flow 중지
                initialValue = emptyList()
            )

    fun clearNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAllNotifications()
        }
    }
}

// Hilt를 사용하지 않으므로 ViewModelProvider.Factory를 직접 구현합니다.
class NotificationViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}