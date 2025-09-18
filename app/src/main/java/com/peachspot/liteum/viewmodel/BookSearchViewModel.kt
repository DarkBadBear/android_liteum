package com.peachspot.liteum.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peachspot.liteum.data.remote.client.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.peachspot.liteum.data.remote.model.BookItem // ⭐ 이 줄을 확인하고, 실제 경로로 수정하세요.


import androidx.lifecycle.ViewModelProvider

// HomeViewModel 클래스가 있는 실제 패키지로 import 경로를 맞춰주세요.
// 예: import com.peachspot.liteum.viewmodel.HomeViewModel 또는 다른 경로일 수 있습니다.
// 만약 HomeViewModel이 같은 패키지에 있다면 별도 import 필요 없을 수 있습니다.

class BookSearchViewModelFactory(
    private val homeViewModel: HomeViewModel
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookSearchViewModel(homeViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


class BookSearchViewModel(
    private val homeViewModel: HomeViewModel // Firebase/Kakao UID 가져오기 위해 주입
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<BookItem>>(emptyList())
    val searchResults: StateFlow<List<BookItem>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage


    init {
        viewModelScope.launch {

        }
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
                val firebaseUid = homeViewModel.uiState.value.firebaseUid
                val kakaoUid = homeViewModel.uiState.value.kakaoUid

                // "intitle:" 접두사 사용
                val response = RetrofitClient.instance.searchBooks(
                    query = "intitle:$title",
                    firebaseUid = firebaseUid,
                    kakaoUid = kakaoUid
                )

                _searchResults.value = response.items ?: emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "도서 검색 중 오류가 발생했습니다: ${e.localizedMessage}"
                _searchResults.value = emptyList()
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
