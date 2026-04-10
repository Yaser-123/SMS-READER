package com.example.myphone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val profile: CreditProfileResponse, val history: List<HistoryItem>) : UiState()
    data class Error(val message: String) : UiState()
}

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    init {
        // Initial fetch of history if any
        refreshHistory()
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getHistory()
            result.onSuccess { response ->
                if (response.latestScore != null) {
                    val mockProfile = CreditProfileResponse(
                        status = "success",
                        score = response.latestScore.score,
                        risk = response.latestScore.risk,
                        summary = "Historical profile loaded. Sync for latest update."
                    )
                    _uiState.value = UiState.Success(mockProfile, response.transactions)
                } else {
                    _uiState.value = UiState.Idle
                }
            }.onFailure {
                _uiState.value = UiState.Idle
            }
        }
    }

    fun syncBusinessData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val messages = repository.getFilteredSms()
            
            if (messages.isEmpty()) {
                _uiState.value = UiState.Error("No business SMS found on device.")
                return@launch
            }

            val syncResult = repository.syncSms(messages)
            syncResult.onSuccess { profile ->
                val historyResult = repository.getHistory()
                historyResult.onSuccess { history ->
                    _uiState.value = UiState.Success(profile, history.transactions)
                }.onFailure {
                    _uiState.value = UiState.Success(profile, emptyList())
                }
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Failed to evaluate credit profile.")
            }
        }
    }

    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
    }
}