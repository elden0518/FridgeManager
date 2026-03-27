package com.example.fridgemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Calendar
import java.util.Date

enum class FoodStatus { ACTIVE, CONSUMED, DISCARDED }

@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String,           // e.g. "250g", "6个"
    val categoryId: Long? = null,
    val expiryDate: Date? = null,
    val remark: String = "",
    val imageUri: String? = null,
    val status: FoodStatus = FoodStatus.ACTIVE,
    val createdAt: Date = Date()
) {
    /** 距今剩余天数（按自然日计算），null 表示未设置到期日 */
    val daysUntilExpiry: Long?
        get() = expiryDate?.let { expiry ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }
            val expiryDay = Calendar.getInstance().apply {
                time = expiry
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }
            val diff = expiryDay.timeInMillis - today.timeInMillis
            diff / (1000L * 60 * 60 * 24)
        }

    val isExpired: Boolean
        get() = daysUntilExpiry?.let { it < 0 } ?: false
}
