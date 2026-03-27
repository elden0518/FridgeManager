package com.example.fridgemanager.data.repository

import com.example.fridgemanager.data.database.CategoryCount
import com.example.fridgemanager.data.database.FoodItemDao
import com.example.fridgemanager.data.database.CategoryDao
import com.example.fridgemanager.data.database.MonthlyCount
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.model.FoodItem
import com.example.fridgemanager.data.model.FoodStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val categoryDao: CategoryDao
) {
    // ── 食材 ──────────────────────────────────────────────────────────

    fun getActiveItems(query: String = "", categoryId: Long? = null): Flow<List<FoodItem>> =
        foodItemDao.getActiveItems(query, categoryId)

    fun getExpiringItems(daysBefore: Int): Flow<List<FoodItem>> {
        require(daysBefore >= 0) { "daysBefore must be non-negative, got $daysBefore" }
        val threshold = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysBefore) }.time
        return foodItemDao.getExpiringItems(threshold)
    }

    suspend fun addItem(item: FoodItem): Long = foodItemDao.insert(item)

    suspend fun updateItem(item: FoodItem) = foodItemDao.update(item)

    suspend fun markConsumed(id: Long) = foodItemDao.updateStatus(id, FoodStatus.CONSUMED)

    suspend fun markDiscarded(id: Long) = foodItemDao.updateStatus(id, FoodStatus.DISCARDED)

    suspend fun deleteItem(item: FoodItem) = foodItemDao.delete(item)

    suspend fun getItemById(id: Long): FoodItem? = foodItemDao.getById(id)

    // ── 统计 ──────────────────────────────────────────────────────────

    fun getMonthlyStats(): Flow<List<MonthlyCount>> {
        val since = Calendar.getInstance().apply { add(Calendar.MONTH, -12) }.timeInMillis
        return foodItemDao.getMonthlyStats(since)
    }

    fun getCategoryDistribution(): Flow<List<CategoryCount>> =
        foodItemDao.getCategoryDistribution()

    // ── 分类 ──────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAll()

    suspend fun addCategory(category: Category): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)
}
