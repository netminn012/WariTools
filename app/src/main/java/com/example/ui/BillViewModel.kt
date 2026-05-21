package com.example.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class DetailedMemberPayment(
    val member: Member,
    val amount: Int,
    val breakdown: String
)

data class SettlementResult(
    val memberPayments: List<DetailedMemberPayment>,
    val totalAmount: Int,
    val isTaxIncluded: Boolean,
    val taxRate: Int
)

class BillViewModel(private val repository: BillRepository) : ViewModel() {

    val members: StateFlow<List<Member>> = repository.allMembers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val billItems: StateFlow<List<BillItem>> = repository.allBillItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<BillSettings?> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _scanLoading = MutableStateFlow(false)
    val scanLoading: StateFlow<Boolean> = _scanLoading

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError

    init {
        // Initialize default settings if not exists
        viewModelScope.launch {
            repository.settings.firstOrNull()?.let {
                if (it == null) {
                    repository.saveSettings(BillSettings())
                }
            } ?: repository.saveSettings(BillSettings())
        }
    }

    // --- Member Actions ---

    fun addMember(name: String?) = viewModelScope.launch {
        val resolvedName = if (name.isNullOrBlank()) {
            val count = members.value.size + 1
            "${count}番"
        } else {
            name.trim()
        }
        repository.insertMember(Member(name = resolvedName))
    }

    fun addMembersByCount(count: Int) = viewModelScope.launch {
        val currentSize = members.value.size
        for (i in 1..count) {
            val num = currentSize + i
            repository.insertMember(Member(name = "${num}番"))
        }
    }

    fun updateMember(member: Member) = viewModelScope.launch {
        repository.updateMember(member)
    }

    fun deleteMember(member: Member) = viewModelScope.launch {
        repository.deleteMember(member)
        // Clean up assignment lists inside bill items as well
        val currentItems = billItems.value
        for (item in currentItems) {
            if (item.assignedMemberIds.contains(member.id)) {
                val updatedIds = item.assignedMemberIds.filter { it != member.id }
                repository.updateBillItem(item.copy(assignedMemberIds = updatedIds))
            }
        }
    }

    fun clearAllMembers() = viewModelScope.launch {
        repository.deleteAllMembers()
        repository.deleteAllBillItems()
    }

    // --- Bill Item Actions ---

    fun addBillItem(
        name: String,
        amount: Double,
        splitType: String,
        assignedMemberIds: List<Int>,
        isTaxIncluded: Boolean,
        taxRate: Int
    ) = viewModelScope.launch {
        repository.insertBillItem(
            BillItem(
                name = name.trim().ifEmpty { "品目" },
                amount = amount,
                splitType = splitType,
                assignedMemberIds = assignedMemberIds,
                isTaxIncluded = isTaxIncluded,
                taxRate = taxRate
            )
        )
    }

    fun updateBillItem(
        id: Int,
        name: String,
        amount: Double,
        splitType: String,
        assignedMemberIds: List<Int>,
        isTaxIncluded: Boolean,
        taxRate: Int
    ) = viewModelScope.launch {
        repository.updateBillItem(
            BillItem(
                id = id,
                name = name.trim().ifEmpty { "品目" },
                amount = amount,
                splitType = splitType,
                assignedMemberIds = assignedMemberIds,
                isTaxIncluded = isTaxIncluded,
                taxRate = taxRate
            )
        )
    }

    fun deleteBillItem(item: BillItem) = viewModelScope.launch {
        repository.deleteBillItem(item)
    }

    fun clearAllBillItems() = viewModelScope.launch {
        repository.deleteAllBillItems()
    }

    // --- Settings Actions ---

    fun updateSettings(newSettings: BillSettings) = viewModelScope.launch {
        repository.saveSettings(newSettings)
    }

    // --- Receipt Image Vision Scan ---

    fun scanReceipt(bitmap: Bitmap, apiKey: String) {
        viewModelScope.launch {
            _scanLoading.value = true
            _scanError.value = null
            try {
                val currentSettings = settings.value ?: BillSettings()
                val scannedItems = repository.scanReceipt(bitmap, apiKey)
                if (scannedItems.isNotEmpty()) {
                    for (scanned in scannedItems) {
                        repository.insertBillItem(
                            BillItem(
                                name = scanned.name,
                                amount = scanned.amount,
                                splitType = "ALL_EQUAL",
                                assignedMemberIds = emptyList(),
                                isTaxIncluded = currentSettings.isTaxIncluded,
                                taxRate = currentSettings.taxRate
                            )
                        )
                    }
                } else {
                    _scanError.value = "レシートから品目を検出できませんでした。"
                }
            } catch (e: Exception) {
                _scanError.value = "読み取りに失敗しました: ${e.localizedMessage}"
            } finally {
                _scanLoading.value = false
            }
        }
    }

    fun clearScanError() {
        _scanError.value = null
    }

    // --- Calculation Engine ---

    fun calculateSettlement(): SettlementResult {
        val currentMembers = members.value
        val currentItems = billItems.value
        val currentSettings = settings.value ?: BillSettings()

        if (currentMembers.isEmpty()) {
            return SettlementResult(emptyList(), 0, currentSettings.isTaxIncluded, currentSettings.taxRate)
        }

        // Initialize shares
        val rawShares = mutableMapOf<Int, Double>()
        val breakdowns = mutableMapOf<Int, MutableList<String>>()
        for (m in currentMembers) {
            rawShares[m.id] = 0.0
            breakdowns[m.id] = mutableListOf()
        }

        var totalBaseAmount = 0.0
        var totalTaxAmount = 0.0

        // 1. Check if Pure Equal Split Mode is toggled (一タップ通常等分)
        if (currentSettings.isPureEqualSplitMode) {
            var grandTotal = 0.0
            for (item in currentItems) {
                val itemBase = item.amount
                val itemTax = if (item.isTaxIncluded) {
                    0.0
                } else {
                    itemBase * (item.taxRate / 100.0)
                }
                grandTotal += (itemBase + itemTax)
            }

            val roundedGrandTotal = grandTotal.toInt()
            val numMembers = currentMembers.size
            if (numMembers == 0) return SettlementResult(emptyList(), 0, currentSettings.isTaxIncluded, currentSettings.taxRate)

            val baseShare = roundedGrandTotal / numMembers
            val rem = roundedGrandTotal % numMembers

            val extraPayerIndices = getExtraPayerIndices(numMembers, rem, currentSettings, currentMembers)

            val payments = currentMembers.mapIndexed { index, member ->
                val extraYen = if (extraPayerIndices.contains(index)) 1 else 0
                val amt = baseShare + extraYen
                DetailedMemberPayment(
                    member = member,
                    amount = amt,
                    breakdown = "全員均等割り (${amt}円)"
                )
            }

            return SettlementResult(
                memberPayments = payments,
                totalAmount = roundedGrandTotal,
                isTaxIncluded = currentSettings.isTaxIncluded,
                taxRate = currentSettings.taxRate
            )
        }

        // 2. Standard Itemized Splits
        for (item in currentItems) {
            val itemBase = item.amount
            val itemTax = if (item.isTaxIncluded) {
                0.0
            } else {
                itemBase * (item.taxRate / 100.0)
            }
            val itemTotal = itemBase + itemTax

            totalBaseAmount += itemBase
            totalTaxAmount += itemTax

            val assignedIds = when (item.splitType) {
                "ALL_EQUAL" -> currentMembers.map { it.id }
                "SELECTED_EQUAL", "INDIVIDUAL" -> {
                    if (item.assignedMemberIds.isEmpty()) {
                        currentMembers.map { it.id }
                    } else {
                        item.assignedMemberIds
                    }
                }
                else -> currentMembers.map { it.id }
            }

            if (assignedIds.isEmpty()) continue

            // Determine if tax splitting is prorated or globally equal-split
            val splitUnit = if (currentSettings.taxSplitType == "PRORATED") {
                itemTotal / assignedIds.size
            } else {
                itemBase / assignedIds.size
            }

            for (mId in assignedIds) {
                if (rawShares.containsKey(mId)) {
                    rawShares[mId] = rawShares[mId]!! + splitUnit
                    val shareDesc = if (assignedIds.size > 1) " (1/${assignedIds.size})" else ""
                    val taxLabel = if (currentSettings.taxSplitType == "PRORATED" && !item.isTaxIncluded) " 税込" else ""
                    breakdowns[mId]?.add("${item.name}: ${splitUnit.toInt()}円${shareDesc}${taxLabel}")
                }
            }
        }

        // Add tax if splitting equally per person (人数等分)
        if (currentSettings.taxSplitType == "EQUAL" && totalTaxAmount > 0.0) {
            val splitTax = totalTaxAmount / currentMembers.size
            for (m in currentMembers) {
                rawShares[m.id] = rawShares[m.id]!! + splitTax
                breakdowns[m.id]?.add("消費税人数等分: ${splitTax.toInt()}円")
            }
        }

        // Calculate Target Grand Total
        val grandTotal = totalBaseAmount + totalTaxAmount
        val roundedGrandTotal = grandTotal.toInt()

        // Sum up base floored integers
        val flooredShares = mutableMapOf<Int, Int>()
        var flooredSum = 0
        for (m in currentMembers) {
            val baseFloor = rawShares[m.id]?.toInt() ?: 0
            flooredShares[m.id] = baseFloor
            flooredSum += baseFloor
        }

        var rem = roundedGrandTotal - flooredSum
        if (rem < 0) rem = 0

        val extraPayerIndices = getExtraPayerIndices(currentMembers.size, rem, currentSettings, currentMembers)

        val payments = currentMembers.mapIndexed { index, member ->
            val extraYen = if (extraPayerIndices.contains(index)) 1 else 0
            val amt = flooredShares[member.id]!! + extraYen
            if (extraYen > 0) {
                breakdowns[member.id]?.add("端数調整: +1円")
            }

            val itemsDesc = breakdowns[member.id]?.joinToString("・") ?: ""
            val fullBreakdown = itemsDesc.ifEmpty { "注文なし" }

            DetailedMemberPayment(
                member = member,
                amount = amt,
                breakdown = fullBreakdown
            )
        }

        return SettlementResult(
            memberPayments = payments,
            totalAmount = roundedGrandTotal,
            isTaxIncluded = currentSettings.isTaxIncluded,
            taxRate = currentSettings.taxRate
        )
    }

    private fun getExtraPayerIndices(
        numMembers: Int,
        remainder: Int,
        settings: BillSettings,
        allMembers: List<Member>
    ): Set<Int> {
        if (remainder <= 0) return emptySet()
        val indexSet = mutableSetOf<Int>()

        when (settings.fractionPayerType) {
            "ORGANIZER" -> {
                // First indices pay the remaining +1 Yen fraction
                for (i in 0 until remainder.coerceAtMost(numMembers)) {
                    indexSet.add(i)
                }
            }
            "MANUAL" -> {
                val payerId = settings.manualFractionPayerId
                val manualIndex = allMembers.indexOfFirst { it.id == payerId }
                val startIdx = if (manualIndex >= 0) manualIndex else 0
                for (i in 0 until remainder.coerceAtMost(numMembers)) {
                    indexSet.add((startIdx + i) % numMembers)
                }
            }
            "RANDOM" -> {
                val seed = allMembers.hashCode() + remainder
                val random = Random(seed)
                val shuffledIndices = (0 until numMembers).toList().shuffled(random)
                for (i in 0 until remainder.coerceAtMost(numMembers)) {
                    indexSet.add(shuffledIndices[i])
                }
            }
            else -> {
                for (i in 0 until remainder.coerceAtMost(numMembers)) {
                    indexSet.add(i)
                }
            }
        }
        return indexSet
    }
}

// --- ViewModel Factory ---

class BillViewModelFactory(private val repository: BillRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
