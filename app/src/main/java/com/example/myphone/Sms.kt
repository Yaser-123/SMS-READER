package com.example.myphone

import com.google.gson.annotations.SerializedName

/**
 * Data model for a single SMS message
 */
data class Sms(
    val body: String,
    val sender: String,
    val date: String
)

/**
 * Data model for Business Scoring Breakdown
 */
data class ScoreBreakdown(
    @SerializedName("base") val base: Int = 300,
    @SerializedName("income") val income: Int = 0,
    @SerializedName("activity") val activity: Int = 0,
    @SerializedName("stability") val stability: Int = 0
)

/**
 * Data model for Loan Recommendation (Tiered)
 */
data class LoanProduct(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("provider") val provider: String,
    @SerializedName("minScore") val minScore: Int,
    @SerializedName("maxAmount") val maxAmount: String = "",
    @SerializedName("interestRate") val interestRate: String = "",
    @SerializedName("tenure") val tenure: String = "",
    @SerializedName("link") val link: String,
    @SerializedName("tag") val tag: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("eligible") val eligible: Boolean = false,
    @SerializedName("pointsToUnlock") val pointsToUnlock: Int = 0
)

/**
 * Main Credit Profile response from backend
 */
data class CreditProfileResponse(
    @SerializedName("status") val status: String = "idle",
    @SerializedName("score") val score: Int = 300,
    @SerializedName("risk") val risk: String = "UNKNOWN",
    @SerializedName("scoreChange") val scoreChange: Int = 0,
    @SerializedName("breakdown") val breakdown: ScoreBreakdown = ScoreBreakdown(),
    @SerializedName("summary") val summary: String = "",
    @SerializedName("features") val features: BusinessFeatures? = BusinessFeatures(),
    @SerializedName("insights") val insights: CreditInsights? = CreditInsights(),
    @SerializedName("eligibleLoans") val eligibleLoans: List<LoanProduct>? = emptyList()
)

data class BusinessFeatures(
    @SerializedName("totalCredit") val totalCredit: Double = 0.0,
    @SerializedName("totalDebit") val totalDebit: Double = 0.0,
    @SerializedName("netBalance") val netBalance: Double = 0.0,
    @SerializedName("transactionCount") val transactionCount: Int = 0
)

data class CreditInsights(
    @SerializedName("income_strength") val incomeStrength: String = "N/A",
    @SerializedName("spending_behavior") val spendingBehavior: String = "N/A",
    @SerializedName("activity_level") val activityLevel: String = "N/A"
)

data class HistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("merchant") val merchant: String,
    @SerializedName("date") val date: String
)

data class HistoryResponse(
    @SerializedName("transactions") val transactions: List<HistoryItem> = emptyList(),
    @SerializedName("latestScore") val latestScore: ScoreEntry? = null,
    @SerializedName("scoreChange") val scoreChange: Int = 0
)

data class ScoreEntry(
    @SerializedName("score") val score: Int,
    @SerializedName("risk") val risk: String,
    @SerializedName("summary") val summary: String = "",
    @SerializedName("breakdown") val breakdown: ScoreBreakdown = ScoreBreakdown(),
    @SerializedName("features") val features: BusinessFeatures = BusinessFeatures(),
    @SerializedName("insights") val insights: CreditInsights = CreditInsights(),
    @SerializedName("eligibleLoans") val eligibleLoans: List<LoanProduct>? = emptyList()
)