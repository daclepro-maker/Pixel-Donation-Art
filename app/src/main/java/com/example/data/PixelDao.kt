package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PixelDao {
    @Query("SELECT * FROM pixel_purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PixelPurchase>>

    @Query("SELECT * FROM pixel_purchases WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getPurchasesByUser(email: String): Flow<List<PixelPurchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PixelPurchase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PixelPurchase>)

    @Query("SELECT * FROM pixel_purchases")
    suspend fun getAllPurchasesList(): List<PixelPurchase>

    @Query("SELECT SUM(amountUsd) FROM pixel_purchases")
    fun getTotalMoneyRaisedFlow(): Flow<Double?>

    @Query("SELECT SUM(pixelCount) FROM pixel_purchases")
    fun getTotalPixelsSoldFlow(): Flow<Int?>

    // Users
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)
}
