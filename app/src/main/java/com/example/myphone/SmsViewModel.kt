package com.example.myphone

import android.app.Application
import android.util.Log
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

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    init {
        refreshHistory()
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    private fun calculateLocalBreakdown(
        remoteBreakdown: ScoreBreakdown,
        features: BusinessFeatures,
        historyCount: Int
    ): ScoreBreakdown {
        // If the server already sent values, use them.
        if (remoteBreakdown.income > 0 || remoteBreakdown.activity > 0) return remoteBreakdown

        // LOCAL FAILSAFE: Mirror of backend scoring.js v5.0
        val incomePoints = (Math.min(features.totalCredit / 15000.0, 1.0) * 250).toInt()
        val activityPoints = (Math.min(historyCount / 15.0, 1.0) * 150).toInt()
        val stabilityPoints = if (features.totalCredit > 5000) 100 else 50

        Log.d("SMS_DEBUG", "Failsafe Active: Calculated +$incomePoints income points locally.")
        
        return ScoreBreakdown(
            base = 300,
            income = incomePoints,
            activity = activityPoints,
            stability = stabilityPoints
        )
    }

    private fun updateFailsafeState(
        profile: CreditProfileResponse,
        history: List<HistoryItem>,
        scoreChange: Int = 0
    ) {
        val features = profile.features ?: BusinessFeatures()
        val localBreakdown = calculateLocalBreakdown(profile.breakdown, features, history.size)
        
        // Calculate final score based on local or remote breakdown
        val finalScore = 300 + localBreakdown.income + localBreakdown.activity + localBreakdown.stability
        
        // Ensure loans are unlocked/locked correctly based on THIS score
        val finalLoans = profile.eligibleLoans?.map { loan ->
            val isEligible = finalScore >= loan.minScore
            loan.copy(
                eligible = isEligible,
                pointsToUnlock = if (isEligible) 0 else loan.minScore - finalScore
            )
        } ?: emptyList()

        _uiState.value = UiState.Success(
            profile.copy(
                score = finalScore,
                breakdown = localBreakdown,
                scoreChange = scoreChange,
                eligibleLoans = finalLoans,
                summary = if (finalScore >= 550) "Solid business standing detected." else "Increase business inflow to improve score."
            ),
            history
        )
    }

    fun refreshHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = repository.getHistory()
                result.onSuccess { response ->
                    if (response.latestScore != null) {
                        val scoreEntry = response.latestScore
                        val profileFromHistory = CreditProfileResponse(
                            status = "success",
                            score = scoreEntry.score,
                            risk = scoreEntry.risk,
                            breakdown = scoreEntry.breakdown,
                            features = scoreEntry.features,
                            insights = scoreEntry.insights,
                            summary = scoreEntry.summary,
                            eligibleLoans = scoreEntry.eligibleLoans
                        )
                        updateFailsafeState(profileFromHistory, response.transactions, response.scoreChange)
                    } else {
                        _uiState.value = UiState.Idle
                    }
                }.onFailure {
                    Log.e("SmsViewModel", "History fetch failed", it)
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                Log.e("SmsViewModel", "Critical error in refreshHistory", e)
                _uiState.value = UiState.Idle
            }
        }
    }

    fun syncBusinessData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val messages = repository.getFilteredSms()
                Log.d("SmsViewModel", "Syncing ${messages.size} filtered messages")

                if (messages.isEmpty()) {
                    _uiState.value = UiState.Error("No financial activity found from trusted contacts.")
                    return@launch
                }

                val syncResult = repository.syncSms(messages)
                syncResult.onSuccess { profile ->
                    val historyResult = repository.getHistory()
                    historyResult.onSuccess { history ->
                        updateFailsafeState(profile, history.transactions, history.scoreChange)
                    }.onFailure {
                        _uiState.value = UiState.Success(profile, emptyList())
                    }
                }.onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Analysis failed.")
                }
            } catch (e: Exception) {
                Log.e("SmsViewModel", "Sync error", e)
                _uiState.value = UiState.Error("Critical error during analysis.")
            }
        }
    }

    fun getCurrentTimestamp(): String {
        return "Last Update: " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun getIncomeExpenseData(history: List<HistoryItem>): Pair<Float, Float> {
        val income = history.filter { it.type.equals("credit", ignoreCase = true) }.sumOf { it.amount }.toFloat()
        val expense = history.filter { it.type.equals("debit", ignoreCase = true) }.sumOf { it.amount }.toFloat()
        return income to expense
    }
}