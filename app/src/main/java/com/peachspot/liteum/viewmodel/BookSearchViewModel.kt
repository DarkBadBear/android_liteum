package com.peachspot.liteum.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.peachspot.liteum.data.remote.model.BookItem // ⭐ 이 줄을 확인하고, 실제 경로로 수정하세요.


import androidx.lifecycle.ViewModelProvider
import com.peachspot.liteum.data.repositiory.BookRepository

// HomeViewModel 클래스가 있는 실제 패키지로 import 경로를 맞춰주세요.
// 예: import com.peachspot.liteum.viewmodel.HomeViewModel 또는 다른 경로일 수 있습니다.
// 만약 HomeViewModel이 같은 패키지에 있다면 별도 import 필요 없을 수 있습니다.


class BookSearchViewModelFactory(
    private val homeViewModel: HomeViewModel,
    private val bookRepository: BookRepository // 1. 생성자에 BookRepository 파라미터 추가
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 2. BookSearchViewModel 생성 시 homeViewModel과 bookRepository 모두 전달
            return BookSearchViewModel(homeViewModel, bookRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


class BookSearchViewModel(
    private val homeViewModel: HomeViewModel, // Firebase/Kakao UID 가져오기 위해 주입
    private val bookRepository: BookRepository // BookRepository 주입
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<BookItem>>(emptyList())
    val searchResults: StateFlow<List<BookItem>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // 초기화 로직이 필요하다면 여기에 작성
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun searchBooksByTitle(title: String) {
        if (title.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
            // HomeViewModel의 uiState를 통해 UID 접근
            val firebaseUid = homeViewModel.uiState.value.firebaseUid
            val kakaoUid = homeViewModel.uiState.value.kakaoUid

            // "intitle:" 접두사 사용
            // RetrofitClient.instance.searchBooks가 BookRepository의 일부가 되어야 할 수 있음
            // 또는 RetrofitClient를 직접 사용하는 것이 맞다면 이대로 유지
                val response = bookRepository.searchBooks(
                    query = "intitle:$title", // API가 "intitle:"을 이해하는지, 아니면 서버에서 처리하는지 확인
                    firebaseUid = firebaseUid,
                    kakaoUid = kakaoUid
                )

            _searchResults.value = response.items ?: emptyList()
        } catch (e: Exception) {
            _errorMessage.value = "도서 검색 중 오류가 발생했습니다: ${e.localizedMessage}"
            _searchResults.value = emptyList() // 오류 발생 시 검색 결과 초기화
            Log.e("BookSearchViewModel", "Error searching books", e) // 상세 오류 로깅
        } finally {
            _isLoading.value = false
        }
        }
    }

    fun getIsbn(bookItem: BookItem): String? {
        return bookItem.volumeInfo?.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
            ?: bookItem.volumeInfo?.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier
    }
}
