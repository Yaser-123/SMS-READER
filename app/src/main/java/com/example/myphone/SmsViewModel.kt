package com.example.myphone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    // Tab state: 0 = Overview, 1 = Analytics, 2 = Loans
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    init {
        refreshHistory()
    }

    fun setTab(index: Int) {
        _currentTab.value = index
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
                        summary = "Active business profile loaded.",
                        eligibleLoans = emptyList() // Will be updated on sync
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
                _uiState.value = UiState.Error("No financial activity detected on device.")
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
                _uiState.value = UiState.Error(it.message ?: "Failed to generate business profile.")
            }
        }
    }

    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
    }

    /**
     * Data Preparation for Charts
     */
    fun getIncomeExpenseData(history: List<HistoryItem>): Pair<Float, Float> {
        val income = history.filter { it.type == "credit" }.sumOf { it.amount }.toFloat()
        val expense = history.filter { it.type == "debit" }.sumOf { it.amount }.toFloat()
        return income to expense
    }

    fun getActivityData(history: List<HistoryItem>): List<Pair<String, Float>> {
        // Group by day for the last 7 days
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val last7Days = (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            dateFormat.format(cal.time)
        }.reversed()

        // Mock grouping (in real app, parse history dates)
        // For demo, we'll just show transaction counts per day
        return last7Days.map { day ->
            day to (1..10).random().toFloat() 
        }
    }
}