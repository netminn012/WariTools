package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY id ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
}

@Dao
interface BillItemDao {
    @Query("SELECT * FROM bill_items ORDER BY id ASC")
    fun getAllBillItems(): Flow<List<BillItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItem(item: BillItem): Long

    @Update
    suspend fun updateBillItem(item: BillItem)

    @Delete
    suspend fun deleteBillItem(item: BillItem)

    @Query("DELETE FROM bill_items")
    suspend fun deleteAllBillItems()
}

@Dao
interface BillSettingsDao {
    @Query("SELECT * FROM bill_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<BillSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: BillSettings)
}
