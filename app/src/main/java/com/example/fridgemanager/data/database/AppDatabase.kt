package com.example.fridgemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fridgemanager.data.model.Category
import com.example.fridgemanager.data.model.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FoodItem::class, Category::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "fridge_manager.db"

        // 迁移：v1→v2 补充"水果"默认分类
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "INSERT OR IGNORE INTO categories (name, emoji, isDefault) " +
                    "VALUES ('水果', '🍎', 1)"
                )
            }
        }

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
