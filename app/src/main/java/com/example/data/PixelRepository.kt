package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PixelRepository(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "pixel_donation.db"
    )
    .fallbackToDestructiveMigration()
    .build()

    private val dao = database.pixelDao()

    val allPurchases: Flow<List<PixelPurchase>> = dao.getAllPurchases()

    fun getPurchasesByUser(email: String): Flow<List<PixelPurchase>> = dao.getPurchasesByUser(email)

    val totalMoneyRaised: Flow<Double> = dao.getTotalMoneyRaisedFlow().map { it ?: 0.0 }
    val totalPixelsSold: Flow<Int> = dao.getTotalPixelsSoldFlow().map { it ?: 0 }

    suspend fun getUserByEmail(email: String): UserAccount? = dao.getUserByEmail(email)

    suspend fun registerUser(user: UserAccount) = dao.insertUser(user)

    suspend fun recordPurchase(purchase: PixelPurchase) = dao.insertPurchase(purchase)

    suspend fun getAllPurchasesList(): List<PixelPurchase> = dao.getAllPurchasesList()

    suspend fun recordPurchases(purchases: List<PixelPurchase>) = dao.insertPurchases(purchases)

    // Prepopulate some realistic community purchases to make the global leaderboard look populated initially!
    suspend fun prepopulateIfEmpty() {
        // Simple check can be made on first run
    }
}
