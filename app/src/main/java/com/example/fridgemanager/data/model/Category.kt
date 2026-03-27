package com.example.fridgemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🍽️",
    val isDefault: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(name = "蔬果",     emoji = "🥦", isDefault = true),
            Category(name = "水果",     emoji = "🍎", isDefault = true),
            Category(name = "乳制品",   emoji = "🥛", isDefault = true),
            Category(name = "肉类",     emoji = "🥩", isDefault = true),
            Category(name = "零食",     emoji = "🍪", isDefault = true),
            Category(name = "饮料",     emoji = "🧃", isDefault = true),
            Category(name = "调料",     emoji = "🧂", isDefault = true),
            Category(name = "冷冻食品", emoji = "🧊", isDefault = true),
            Category(name = "其他",     emoji = "📦", isDefault = true),
        )
    }
}
