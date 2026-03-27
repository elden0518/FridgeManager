package com.example.fridgemanager.ui.screens.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgemanager.ai.AiRecognitionService
import com.example.fridgemanager.ai.AiResult
import com.example.fridgemanager.ai.RecognitionResult
import com.example.fridgemanager.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val recognitionResults: List<RecognitionResult> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val aiService: AiRecognitionService,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun recognizeImage(uri: Uri) = viewModelScope.launch {
        _uiState.value = CameraUiState(
            selectedImageUri = uri,
            isLoading = true,
            errorMessage = null
        )

        val apiKey = prefs.aiApiKey.first()
        when (val result = aiService.recognizeImage(uri, apiKey)) {
            is AiResult.Success -> _uiState.value = _uiState.value.copy(
                isLoading = false,
                recognitionResults = result.results
            )
            is AiResult.Error -> _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.message
            )
        }
    }

    fun clearResult() {
        _uiState.value = CameraUiState()
    }
}
