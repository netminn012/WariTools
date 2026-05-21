package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "bill_items")
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val splitType: String, // "INDIVIDUAL", "ALL_EQUAL", "SELECTED_EQUAL"
    val assignedMemberIds: List<Int>, // Members assigned for split
    val isTaxIncluded: Boolean = true,
    val taxRate: Int = 10 // 8 or 10
)

@Entity(tableName = "bill_settings")
data class BillSettings(
    @PrimaryKey val id: Int = 1,
    val isTaxIncluded: Boolean = true,
    val taxRate: Int = 10, // 8 or 10
    val taxSplitType: String = "PRORATED", // "PRORATED" (比率按分) or "EQUAL" (人数等分)
    val fractionPayerType: String = "ORGANIZER", // "ORGANIZER", "RANDOM", "MANUAL"
    val manualFractionPayerId: Int? = null,
    val isPureEqualSplitMode: Boolean = false
)

class AppTypeConverters {
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.toIntOrNull() }
    }
}
