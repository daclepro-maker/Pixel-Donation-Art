package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object NetworkSyncManager {
    private val client = OkHttpClient()
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val listType = Types.newParameterizedType(List::class.java, PixelPurchase::class.java)
    private val jsonAdapter = moshi.adapter<List<PixelPurchase>>(listType)
    
    // Unique shared cloud storage URL for the applet
    private const val SYNC_URL = "https://kvdb.io/6125989027204765b32df4fd6e26f9d6/purchases"

    suspend fun fetchRemotePurchases(): List<PixelPurchase> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(SYNC_URL)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        return@withContext jsonAdapter.fromJson(bodyString) ?: emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun pushPurchases(purchases: List<PixelPurchase>): Boolean = withContext(Dispatchers.IO) {
        try {
            val sanitized = purchases.map { it.copy(id = 0) }
            val json = jsonAdapter.toJson(sanitized)
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(SYNC_URL)
                .put(body)
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
