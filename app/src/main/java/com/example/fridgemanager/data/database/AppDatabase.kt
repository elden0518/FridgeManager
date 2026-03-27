package com.example.fridgemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.model.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FoodItem::class, Category::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "fridge_manager.db"

        // 预填充默认分类的回调
        fun buildCallback(scope: CoroutineScope, db: () -> AppDatabase) =
            object : Callback() {
                override fun onCreate(sqLiteDatabase: SupportSQLiteDatabase) {
                    scope.launch(Dispatchers.IO) {
                        db().categoryDao().insertAll(Category.DEFAULT_CATEGORIES)
                    }
                }
            }
    }
}
