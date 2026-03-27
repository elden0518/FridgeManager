package com.example.fridgemanager.data.database

import androidx.room.TypeConverter
import com.example.fridgemanager.data.model.FoodStatus
import java.util.Date

class Converters {
    @TypeConverter fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    @TypeConverter fun toTimestamp(date: Date?): Long? = date?.time

    @TypeConverter fun fromFoodStatus(value: String?): FoodStatus? =
        value?.let { name -> FoodStatus.values().firstOrNull { it.name == name } ?: FoodStatus.ACTIVE }
    @TypeConverter fun toFoodStatus(status: FoodStatus?): String? = status?.name
}
