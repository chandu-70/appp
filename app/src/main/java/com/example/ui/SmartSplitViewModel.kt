package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ReceiptChatParserService
import com.example.data.SmartSplitRepository
import com.example.data.local.SmartSplitDatabase
import com.example.data.preferences.UserPreferences
import com.example.model.CurrencyOption
import com.example.model.DashboardSummary
import com.example.model.ExpenseCategory
import com.example.model.ExpenseGroupDetail
import com.example.model.GroupMember
import com.example.model.ParsedExpenseDraft
import com.example.model.ParsedLineItem
import com.example.model.PaymentMode
import com.example.model.PersonalExpense
import com.example.model.SmartTab
import com.example.model.SplitMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

private const val TAG = "SmartSplitViewModel"

sealed interface AiParserUiState {
    data object Idle : AiParserUiState
    data class Loading(val modeLabel: String) : AiParserUiState
    data class Success(val draft: ParsedExpenseDraft) : AiParserUiState
    data class Error(val message: String) : AiParserUiState
}

enum class AiInputMode {
    RECEIPT_IMAGE,
    MESSY_CHAT
}

class SmartSplitViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SmartSplitDatabase.getInstance(application)
    private val preferences = UserPreferences(application)
    private val repository = SmartSplitRepository(database.smartSplitDao(), preferences)
    private val parserService = ReceiptChatParserService()

    private val _currentTab = MutableStateFlow(SmartTab.DASHBOARD)
    val currentTab: StateFlow<SmartTab> = _currentTab.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    private val _aiInputMode = MutableStateFlow(AiInputMode.RECEIPT_IMAGE)
    val aiInputMode: StateFlow<AiInputMode> = _aiInputMode.asStateFlow()

    private val _selectedReceiptUri = MutableStateFlow<Uri?>(null)
    val selectedReceiptUri: StateFlow<Uri?> = _selectedReceiptUri.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    private val _aiTargetGroupId = MutableStateFlow<Long?>(null)
    private val _aiSelectedPayerId = MutableStateFlow<Long?>(null)

    private val _aiParserState = MutableStateFlow<AiParserUiState>(AiParserUiState.Idle)
    val aiParserState: StateFlow<AiParserUiState> = _aiParserState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val currency: StateFlow<CurrencyOption> = repository.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.INR)

    val groups: StateFlow<List<ExpenseGroupDetail>> = repository.groupDetailsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroup: StateFlow<ExpenseGroupDetail?> = combine(
        groups,
        _selectedGroupId,
    ) { allGroups, selectedId ->
        allGroups.firstOrNull { it.id == selectedId } ?: allGroups.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiTargetGroup: StateFlow<ExpenseGroupDetail?> = combine(
        groups,
        _aiTargetGroupId,
    ) { allGroups, targetId ->
        allGroups.firstOrNull { it.id == targetId } ?: allGroups.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiSelectedPayerId: StateFlow<Long?> = combine(
        aiTargetGroup,
        _aiSelectedPayerId,
    ) { targetGroup, selectedId ->
        val members = targetGroup?.members.orEmpty()
        members.firstOrNull { it.id == selectedId }?.id
            ?: members.firstOrNull { it.isCurrentUser }?.id
            ?: members.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val personalExpenses: StateFlow<List<PersonalExpense>> = repository.personalExpensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummaryFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardSummary(0.0, 0.0, 0.0, emptyList(), 0.0, emptyList()),
        )

    init {
        viewModelScope.launch {
            repository.ensureSeededOnFirstLaunch()
        }
    }

    fun selectTab(tab: SmartTab) {
        _currentTab.value = tab
    }

    fun selectGroup(groupId: Long) {
        _selectedGroupId.value = groupId
    }

    fun selectCurrency(option: CurrencyOption) {
        viewModelScope.launch {
            repository.setCurrency(option)
            _snackbarMessage.value = "Currency switched to ${option.code} (${option.symbol})"
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.resetSampleData()
            _selectedGroupId.value = null
            _aiTargetGroupId.value = null
            _aiParserState.value = AiParserUiState.Idle
            _snackbarMessage.value = "Sample data restored"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Groups & Splits ---

    fun createGroup(name: String, description: String, emoji: String, membersCsv: String) {
        viewModelScope.launch {
            val extraMembers = membersCsv.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
            val id = repository.createGroup(name, description, emoji, extraMembers)
            _selectedGroupId.value = id
            _snackbarMessage.value = "Created group \"$name\""
        }
    }

    fun addMember(groupId: Long, memberName: String) {
        viewModelScope.launch {
            repository.addMemberToGroup(groupId, memberName)
            _snackbarMessage.value = "Added $memberName to group"
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
            _selectedGroupId.value = null
            _snackbarMessage.value = "Group deleted"
        }
    }

    fun addSharedExpense(
        groupId: Long,
        title: String,
        totalAmount: Double,
        category: ExpenseCategory,
        paidByMemberId: Long,
        splitMode: SplitMode,
        splits: List<Pair<Long, Pair<Double, Double>>>,
        notes: String,
    ) {
        viewModelScope.launch {
            repository.addSharedExpense(
                groupId = groupId,
                title = title,
                totalAmount = totalAmount,
                category = category,
                paidByMemberId = paidByMemberId,
                splitMode = splitMode,
                splits = splits,
                notes = notes,
            )
            _snackbarMessage.value = "Added \"$title\" to group"
        }
    }

    fun markSettled(groupId: Long, from: GroupMember, to: GroupMember, amount: Double) {
        viewModelScope.launch {
            repository.recordSettlement(groupId, from, to, amount)
            _snackbarMessage.value = "Settled ${from.name} → ${to.name}"
        }
    }

    fun deleteSharedExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteSharedExpense(expenseId)
            _snackbarMessage.value = "Expense removed"
        }
    }

    // --- Personal Tracker ---

    fun addPersonalExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        paymentMode: PaymentMode,
        dateMillis: Long,
        notes: String,
    ) {
        viewModelScope.launch {
            repository.addPersonalExpense(title, amount, category, paymentMode, dateMillis, notes)
            _snackbarMessage.value = "Logged personal expense"
        }
    }

    fun deletePersonalExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deletePersonalExpense(expenseId)
            _snackbarMessage.value = "Deleted personal expense"
        }
    }

    // --- AI Receipt & Chat Parser ---

    fun setAiInputMode(mode: AiInputMode) {
        _aiInputMode.value = mode
    }

    fun setReceiptUri(uri: Uri?) {
        _selectedReceiptUri.value = uri
        if (_aiParserState.value is AiParserUiState.Error) {
            _aiParserState.value = AiParserUiState.Idle
        }
    }

    fun setChatInputText(text: String) {
        _chatInputText.value = text
    }

    fun fillSampleChat() {
        val members = aiTargetGroup.value?.members?.map { it.name }.orEmpty()
        val m1 = members.getOrNull(1) ?: "Aarav"
        val m2 = members.getOrNull(2) ?: "Priya"
        val m3 = members.getOrNull(3) ?: "Rohan"
        _chatInputText.value = """
            $m1: Paid 1850 for Third Wave Coffee & breakfast platters, split bw all 4 of us
            $m2: Added 320 tip & delivery fee on that bill too
            You: I booked the cab back (640) just for me and $m3
            $m1: Oh and snacks from Nature's Basket were 480 for everyone
        """.trimIndent()
    }

    fun selectAiTargetGroup(groupId: Long) {
        _aiTargetGroupId.value = groupId
        val target = groups.value.firstOrNull { it.id == groupId } ?: return
        val currentDraft = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        val validNames = target.members.map { it.name }
        val updatedItems = currentDraft.items.map { item ->
            val kept = item.assignedMemberNames.filter { name -> validNames.any { it.equals(name, ignoreCase = true) } }.toSet()
            item.copy(assignedMemberNames = kept.ifEmpty { validNames.toSet() })
        }
        _aiParserState.value = AiParserUiState.Success(currentDraft.copy(items = updatedItems))
        _aiSelectedPayerId.value = target.members.firstOrNull {
            it.name.equals(currentDraft.payerName, ignoreCase = true)
        }?.id ?: target.members.firstOrNull { it.isCurrentUser }?.id
    }

    fun selectAiPayer(memberId: Long) {
        _aiSelectedPayerId.value = memberId
    }

    fun analyzeReceiptImage() {
        val uri = _selectedReceiptUri.value ?: return
        val targetGroup = aiTargetGroup.value
        val memberNames = targetGroup?.members?.map { it.name } ?: listOf("You")

        viewModelScope.launch {
            _aiParserState.value = AiParserUiState.Loading("Scanning receipt line items with Gemini...")
            try {
                val draft = parserService.parseReceiptImage(
                    context = getApplication(),
                    imageUri = uri,
                    availableMemberNames = memberNames,
                )
                applyParsedDraft(draft, targetGroup)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt analysis timed out", e)
                _aiParserState.value = AiParserUiState.Error(
                    "Receipt analysis timed out after 60 seconds. Check your connection and retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt analysis failed", e)
                _aiParserState.value = AiParserUiState.Error(
                    e.localizedMessage ?: "Failed to parse receipt image."
                )
            }
        }
    }

    fun analyzeChatText() {
        val text = _chatInputText.value.trim()
        if (text.isBlank()) return
        val targetGroup = aiTargetGroup.value
        val memberNames = targetGroup?.members?.map { it.name } ?: listOf("You")

        viewModelScope.launch {
            _aiParserState.value = AiParserUiState.Loading("Extracting expenses & participants with Gemini...")
            try {
                val draft = parserService.parseChatText(
                    chatText = text,
                    availableMemberNames = memberNames,
                )
                applyParsedDraft(draft, targetGroup)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat parsing timed out", e)
                _aiParserState.value = AiParserUiState.Error(
                    "Chat parsing timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat parsing failed", e)
                _aiParserState.value = AiParserUiState.Error(
                    e.localizedMessage ?: "Failed to parse chat log."
                )
            }
        }
    }

    private fun applyParsedDraft(draft: ParsedExpenseDraft, targetGroup: ExpenseGroupDetail?) {
        val members = targetGroup?.members.orEmpty()
        val matchedPayer = members.firstOrNull {
            it.name.equals(draft.payerName, ignoreCase = true)
        } ?: members.firstOrNull { it.isCurrentUser } ?: members.firstOrNull()

        _aiSelectedPayerId.value = matchedPayer?.id
        _aiParserState.value = AiParserUiState.Success(draft)
    }

    fun updateDraftMeta(title: String? = null, category: ExpenseCategory? = null, taxOrTip: Double? = null) {
        val current = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        _aiParserState.value = AiParserUiState.Success(
            current.copy(
                title = title ?: current.title,
                category = category ?: current.category,
                taxOrTip = taxOrTip ?: current.taxOrTip,
            )
        )
    }

    fun updateDraftItem(itemId: String, name: String, quantity: Int, totalPrice: Double) {
        val current = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        val safeQty = quantity.coerceAtLeast(1)
        val safeTotal = totalPrice.coerceAtLeast(0.0)
        val updated = current.items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    name = name,
                    quantity = safeQty,
                    totalPrice = safeTotal,
                    unitPrice = (safeTotal / safeQty * 100.0).roundToInt() / 100.0,
                )
            } else {
                item
            }
        }
        _aiParserState.value = AiParserUiState.Success(current.copy(items = updated))
    }

    fun toggleItemMember(itemId: String, memberName: String) {
        val current = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        val updated = current.items.map { item ->
            if (item.id == itemId) {
                val nextSet = item.assignedMemberNames.toMutableSet()
                if (memberName in nextSet) {
                    if (nextSet.size > 1) nextSet.remove(memberName)
                } else {
                    nextSet.add(memberName)
                }
                item.copy(assignedMemberNames = nextSet)
            } else {
                item
            }
        }
        _aiParserState.value = AiParserUiState.Success(current.copy(items = updated))
    }

    fun addEmptyDraftItem() {
        val current = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        val defaultMembers = aiTargetGroup.value?.members?.map { it.name }?.toSet() ?: setOf("You")
        val next = ParsedLineItem(
            id = UUID.randomUUID().toString(),
            name = "Extra Item",
            quantity = 1,
            unitPrice = 0.0,
            totalPrice = 0.0,
            assignedMemberNames = defaultMembers,
        )
        _aiParserState.value = AiParserUiState.Success(current.copy(items = current.items + next))
    }

    fun removeDraftItem(itemId: String) {
        val current = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        if (current.items.size <= 1) return
        _aiParserState.value = AiParserUiState.Success(
            current.copy(items = current.items.filterNot { it.id == itemId })
        )
    }

    fun clearAiDraft() {
        _aiParserState.value = AiParserUiState.Idle
    }

    fun commitParsedDraftToGroup() {
        val current = (_aiParserState.value as? AiParserUiState.Success)?.draft ?: return
        val group = aiTargetGroup.value ?: return
        if (group.members.isEmpty()) return

        val payerId = aiSelectedPayerId.value ?: group.members.first().id
        val itemsSubtotal = current.items.sumOf { it.totalPrice }
        val grandTotal = ((itemsSubtotal + current.taxOrTip) * 100.0).roundToInt() / 100.0
        if (grandTotal <= 0.0) {
            _snackbarMessage.value = "Total amount must be greater than zero"
            return
        }

        // Accumulate item shares per member
        val shareByMemberId = group.members.associate { it.id to 0.0 }.toMutableMap()
        for (item in current.items) {
            val matchedMembers = group.members.filter { m ->
                item.assignedMemberNames.any { it.equals(m.name, ignoreCase = true) }
            }.ifEmpty { group.members }
            val perPerson = item.totalPrice / matchedMembers.size
            for (member in matchedMembers) {
                shareByMemberId[member.id] = (shareByMemberId[member.id] ?: 0.0) + perPerson
            }
        }

        // Distribute tax/tip proportionally to item subtotals (or equally if subtotal is 0)
        if (current.taxOrTip > 0.0) {
            if (itemsSubtotal > 0.0) {
                for (member in group.members) {
                    val base = shareByMemberId[member.id] ?: 0.0
                    val extra = current.taxOrTip * (base / itemsSubtotal)
                    shareByMemberId[member.id] = base + extra
                }
            } else {
                val equalExtra = current.taxOrTip / group.members.size
                for (member in group.members) {
                    shareByMemberId[member.id] = (shareByMemberId[member.id] ?: 0.0) + equalExtra
                }
            }
        }

        // Round to 2 decimals and fix any 1-cent rounding drift on the first active participant
        val activeSplits = shareByMemberId.entries
            .filter { it.value > 0.005 }
            .map { it.key to ((it.value * 100.0).roundToInt() / 100.0) }
            .toMutableList()

        if (activeSplits.isNotEmpty()) {
            val currentSum = activeSplits.sumOf { it.second }
            val drift = ((grandTotal - currentSum) * 100.0).roundToInt() / 100.0
            if (drift != 0.0) {
                val first = activeSplits[0]
                activeSplits[0] = first.first to ((first.second + drift) * 100.0).roundToInt() / 100.0
            }
        }

        val splitPairs = activeSplits.map { (memberId, amt) ->
            val pct = if (grandTotal > 0.0) ((amt / grandTotal) * 10000.0).roundToInt() / 100.0 else 0.0
            memberId to (amt to pct)
        }

        viewModelScope.launch {
            repository.addSharedExpense(
                groupId = group.id,
                title = current.title.ifBlank { "AI Parsed Bill" },
                totalAmount = grandTotal,
                category = current.category,
                paidByMemberId = payerId,
                splitMode = SplitMode.EXACT,
                splits = splitPairs,
                notes = current.notes.ifBlank { "Added via SmartSplit-X AI Parser (${current.items.size} items)" },
            )
            _selectedGroupId.value = group.id
            _aiParserState.value = AiParserUiState.Idle
            _selectedReceiptUri.value = null
            _chatInputText.value = ""
            _currentTab.value = SmartTab.GROUPS
            _snackbarMessage.value = "Added \"${current.title}\" to ${group.name}"
        }
    }
}
