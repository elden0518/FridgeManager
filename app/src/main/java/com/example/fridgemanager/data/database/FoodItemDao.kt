package com.example.fridgemanager.data.database

import androidx.room.*
import com.example.fridgemanager.data.model.FoodItem
import com.example.fridgemanager.data.model.FoodStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface FoodItemDao {

    @Query("""
        SELECT * FROM food_items
        WHERE status = 'ACTIVE'
        AND (:query = '' OR name LIKE '%' || :query || '%')
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY
            CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END,
            expiryDate ASC
    """)
    fun getActiveItems(query: String = "", categoryId: Long? = null): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: Long): FoodItem?

    @Query("""
        SELECT * FROM food_items
        WHERE status = 'ACTIVE'
        AND expiryDate IS NOT NULL
        AND expiryDate <= :threshold
        ORDER BY expiryDate ASC
    """)
    fun getExpiringItems(threshold: Date): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItem>): List<Long>

    @Update
    suspend fun update(item: FoodItem)

    @Query("UPDATE food_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: FoodStatus)

    @Delete
    suspend fun delete(item: FoodItem)

    // 统计：按月统计过期/丢弃数量（近12个月）
    @Query("""
        SELECT strftime('%Y-%m', datetime(createdAt / 1000, 'unixepoch')) as month,
               COUNT(*) as count
        FROM food_items
        WHERE status IN ('CONSUMED', 'DISCARDED')
        AND createdAt >= :since
        GROUP BY month
        ORDER BY month ASC
    """)
    fun getMonthlyStats(since: Long): Flow<List<MonthlyCount>>

    // 统计：当前库存按分类占比
    @Query("""
        SELECT categoryId, COUNT(*) as count
        FROM food_items
        WHERE status = 'ACTIVE'
        GROUP BY categoryId
    """)
    fun getCategoryDistribution(): Flow<List<CategoryCount>>
}

data class MonthlyCount(val month: String, val count: Int)
data class CategoryCount(val categoryId: Long?, val count: Int)
