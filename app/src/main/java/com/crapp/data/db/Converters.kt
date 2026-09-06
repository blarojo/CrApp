package com.crapp.data.db

import androidx.room.TypeConverter
import com.crapp.data.model.Amount
import com.crapp.data.model.EnergyLevel
import com.crapp.data.model.Location
import com.crapp.data.model.MealType
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromMealType(value: MealType?): String? = value?.name

    @TypeConverter
    fun toMealType(value: String?): MealType? = value?.let { MealType.valueOf(it) }

    @TypeConverter
    fun fromAmount(value: Amount?): String? = value?.name

    @TypeConverter
    fun toAmount(value: String?): Amount? = value?.let { Amount.valueOf(it) }

    @TypeConverter
    fun fromLocation(value: Location?): String? = value?.name

    @TypeConverter
    fun toLocation(value: String?): Location? = value?.let { Location.valueOf(it) }

    @TypeConverter
    fun fromEnergyLevel(value: EnergyLevel?): String? = value?.name

    @TypeConverter
    fun toEnergyLevel(value: String?): EnergyLevel? = value?.let { EnergyLevel.valueOf(it) }
}
