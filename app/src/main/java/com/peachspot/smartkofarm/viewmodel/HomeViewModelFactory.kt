package com.peachspot.smartkofarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.peachspot.smartkofarm.data.remote.api.MyApiService
import com.peachspot.smartkofarm.data.repositiory.HomeRepository
import com.peachspot.smartkofarm.data.repositiory.UserPreferencesRepository


class HomeViewModelFactory(
    private val homeRepository: HomeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val myApiService: MyApiService, // MyApiService 추가
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                homeRepository,
                userPreferencesRepository,
                myApiService,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

