package com.example.myphone


import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsRepository(private val context: Context) {

    private val api: SmsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://9092-152-59-199-148.ngrok-free.app/") // Live ngrok link
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(SmsApi::class.java)
    }

    private val keywords = listOf("UPI", "credited", "debited", "paid", "received", "txn", "bank", "INR", "Rs", "₹", "A/c")

    fun getFilteredSms(): List<Sms> {
        val smsList = mutableListOf<Sms>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        val sortOrder = "date DESC LIMIT 100"

        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            val addressIndex = it.getColumnIndexOrThrow("address")
            val bodyIndex = it.getColumnIndexOrThrow("body")
            val dateIndex = it.getColumnIndexOrThrow("date")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex) ?: ""
                val address = it.getString(addressIndex) ?: "Unknown"
                val dateLong = it.getLong(dateIndex)
                
                // --- ABSOLUTE FIREWALL ---
                // Only process messages from the specific verified contact
                if (!address.contains("BOIIND", ignoreCase = true)) {
                    android.util.Log.d("SmsRepo", "Firewall blocked junk from: $address")
                    continue
                }

                // Filter by keywords (extra safety)
                if (keywords.any { keyword -> body.contains(keyword, ignoreCase = true) }) {
                    smsList.add(
                        Sms(
                            body = body,
                            sender = address,
                            date = dateFormat.format(Date(dateLong))
                        )
                    )
                }
            }
        }
        return smsList
    }

    /**
     * Syncs local SMS to backend to get the latest Credit Profile
     */
    suspend fun syncSms(messages: List<Sms>): Result<CreditProfileResponse> {
        return try {
            val response = api.syncSms(SmsSyncRequest(messages))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Evaluation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches transaction history from Supabase via backend
     */
    suspend fun getHistory(): Result<HistoryResponse> {
        return try {
            val response = api.getHistory()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("History fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}