package com.example.myphone

import com.google.gson.annotations.SerializedName

/**
 * Data model for a single SMS message
 */
data class Sms(
    @SerializedName("body") val body: String,
    @SerializedName("date") val date: String,
    @SerializedName("sender") val sender: String
)

/**
 * Data model for Business Metrics
 */
data class BusinessFeatures(
    @SerializedName("totalCredit") val totalCredit: Double,
    @SerializedName("totalDebit") val totalDebit: Double,
    @SerializedName("netBalance") val netBalance: Double,
    @SerializedName("transactionCount") val transactionCount: Int,
    @SerializedName("spendingRatio") val spendingRatio: Double,
    @SerializedName("avgTransactionValue") val avgTransactionValue: Double
)

/**
 * Data model for Explainable Insights
 */
data class CreditInsights(
    @SerializedName("income_strength") val incomeStrength: String,
    @SerializedName("spending_behavior") val spendingBehavior: String,
    @SerializedName("activity_level") val activityLevel: String
)

/**
 * Data model for Loan Recommendation
 */
data class LoanProduct(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("provider") val provider: String,
    @SerializedName("minScore") val minScore: Int,
    @SerializedName("amount") val amount: String,
    @SerializedName("link") val link: String
)

/**
 * Main Credit Profile response from backend
 */
data class CreditProfileResponse(
    @SerializedName("status") val status: String,
    @SerializedName("score") val score: Int,
    @SerializedName("risk") val risk: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("features") val features: BusinessFeatures? = null,
    @SerializedName("insights") val insights: CreditInsights? = null,
    @SerializedName("topMerchants") val topMerchants: List<String> = emptyList(),
    @SerializedName("eligibleLoans") val eligibleLoans: List<LoanProduct> = emptyList()
)

/**
 * History response item
 */
data class HistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("merchant") val merchant: String,
    @SerializedName("date") val date: String,
    @SerializedName("raw_message") val rawMessage: String
)

/**
 * Full History Response
 */
data class HistoryResponse(
    @SerializedName("transactions") val transactions: List<HistoryItem>,
    @SerializedName("latestScore") val latestScore: ScoreEntry? = null
)

data class ScoreEntry(
    @SerializedName("score") val score: Int,
    @SerializedName("risk") val risk: String
)