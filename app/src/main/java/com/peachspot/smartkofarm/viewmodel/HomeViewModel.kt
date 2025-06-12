package com.peachspot.smartkofarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.peachspot.smartkofarm.data.remote.api.MyApiService
import com.peachspot.smartkofarm.data.repositiory.HomeRepository
import com.peachspot.smartkofarm.data.repositiory.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val errorMessage: String? = null,
    val userMessage: String? = null, // 사용자에게 표시할 메시지 (Snackbar 등)
    val userMessageType: String? = "info", //
)


class HomeViewModel (
    private val homeRepository: HomeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val myApiService: MyApiService,

    ): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

