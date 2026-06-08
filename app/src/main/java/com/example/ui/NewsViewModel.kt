package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NewsItem
import com.example.data.NewsRepository
import com.example.data.SportsNewsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewsUiState {
    object Loading : NewsUiState
    data class Success(val data: SportsNewsData) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

class NewsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedArticle = MutableStateFlow<NewsItem?>(null)
    val selectedArticle: StateFlow<NewsItem?> = _selectedArticle.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = NewsUiState.Loading
            // Artificial delay to make sure the user can appreciate the gorgeous sport skeleton loader!
            delay(1200)
            try {
                val data = NewsRepository.fetchNewsFromGemini()
                _uiState.value = NewsUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Error desconocido al cargar noticias.")
            }
        }
    }

    fun refreshNews() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            // Showing skeleton for refresh
            _uiState.value = NewsUiState.Loading
            delay(1500)
            try {
                val data = NewsRepository.fetchNewsFromGemini()
                _uiState.value = NewsUiState.Success(data)
                _isRefreshing.value = false
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Error al actualizar.")
                _isRefreshing.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectArticle(article: NewsItem?) {
        _selectedArticle.value = article
    }
}
