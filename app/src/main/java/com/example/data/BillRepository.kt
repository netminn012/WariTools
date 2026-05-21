package com.example.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

class BillRepository(
    private val memberDao: MemberDao,
    private val billItemDao: BillItemDao,
    private val billSettingsDao: BillSettingsDao,
    private val geminiRepository: GeminiRepository
) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val allBillItems: Flow<List<BillItem>> = billItemDao.getAllBillItems()
    val settings: Flow<BillSettings?> = billSettingsDao.getSettings()

    suspend fun insertMember(member: Member): Long {
        return memberDao.insertMember(member)
    }

    suspend fun updateMember(member: Member) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
    }

    suspend fun deleteAllMembers() {
        memberDao.deleteAllMembers()
    }

    suspend fun insertBillItem(item: BillItem): Long {
        return billItemDao.insertBillItem(item)
    }

    suspend fun updateBillItem(item: BillItem) {
        billItemDao.updateBillItem(item)
    }

    suspend fun deleteBillItem(item: BillItem) {
        billItemDao.deleteBillItem(item)
    }

    suspend fun deleteAllBillItems() {
        billItemDao.deleteAllBillItems()
    }

    suspend fun saveSettings(settings: BillSettings) {
        billSettingsDao.insertSettings(settings)
    }

    suspend fun scanReceipt(bitmap: Bitmap, apiKey: String): List<ScanResultItem> {
        return geminiRepository.scanReceipt(bitmap, apiKey)
    }
}
