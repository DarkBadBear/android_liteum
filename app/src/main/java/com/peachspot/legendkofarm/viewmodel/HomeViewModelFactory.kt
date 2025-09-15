package com.peachspot.legendkofarm.viewmodel

import android.app.Application
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.peachspot.legendkofarm.data.remote.api.MyApiService
import com.peachspot.legendkofarm.data.repositiory.HomeRepository
import com.peachspot.legendkofarm.data.repositiory.UserPreferencesRepository
import com.peachspot.legendkofarm.util.Logger


class HomeViewModelFactory(
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val myApiService: MyApiService,
    private val homeRepository: HomeRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val credentialManager = CredentialManager.create(application)
            val viewModel = HomeViewModel(
                application,
                userPreferencesRepository,
                firebaseAuth,
                credentialManager,
                myApiService,
                homeRepository,

            ) as T
            Logger.d("ProfileVMFactory", "ProfileViewModel instance created: $viewModel")
            return viewModel
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

