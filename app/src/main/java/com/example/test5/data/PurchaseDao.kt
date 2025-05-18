package com.example.test5.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.test5.models.Purchase

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insertPurchase(purchase: Purchase)

    @Query("SELECT * FROM purchases")
    suspend fun getAllPurchases(): List<Purchase>

    @Delete
    suspend fun deletePurchase(purchase: Purchase)

    @Query("SELECT category, SUM(price) as totalAmount FROM purchases GROUP BY category")
    suspend fun getCategoryExpenses(): List<CategoryExpense>

    @Query("SELECT * FROM purchases")
    fun getAll(): List<com.example.test5.models.Purchase>
}


data class CategoryExpense(
    val category: String,
    val totalAmount: Double
)