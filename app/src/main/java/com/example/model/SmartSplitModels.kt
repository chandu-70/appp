package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.VioletAccent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class SmartTab(val label: String) {
    DASHBOARD("Dashboard"),
    GROUPS("Groups"),
    AI_PARSER("AI Parser"),
    PERSONAL("Personal")
}

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val locale: Locale,
) {
    INR("INR", "₹", "Indian Rupee (₹)", Locale("en", "IN")),
    USD("USD", "$", "US Dollar ($)", Locale.US),
    EUR("EUR", "€", "Euro (€)", Locale.GERMANY),
    GBP("GBP", "£", "British Pound (£)", Locale.UK);

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: INR
    }
}

enum class ExpenseCategory(
    val label: String,
    val chartColor: Color,
) {
    FOOD("Food", EmeraldPrimary),
    RENT("Rent", IndigoAccent),
    TRAVEL("Travel", CyanAccent),
    UTILITIES("Utilities", AmberAccent),
    SHOPPING("Shopping", VioletAccent),
    ENTERTAINMENT("Entertainment", RoseAccent);

    companion object {
        fun fromName(name: String?): ExpenseCategory =
            entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                    it.label.equals(name, ignoreCase = true)
            } ?: FOOD
    }
}

enum class SplitMode(val label: String) {
    EQUAL("Equal Split"),
    EXACT("Exact Amounts"),
    PERCENT("Percentages");

    companion object {
        fun fromName(name: String?): SplitMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EQUAL
    }
}

enum class PaymentMode(val label: String) {
    UPI("UPI"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    CASH("Cash"),
    NET_BANKING("NetBanking");

    companion object {
        fun fromName(name: String?): PaymentMode =
            entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                    it.label.equals(name, ignoreCase = true)
            } ?: UPI
    }
}

data class GroupMember(
    val id: Long,
    val groupId: Long,
    val name: String,
    val isCurrentUser: Boolean,
)

data class ExpenseSplitItem(
    val memberId: Long,
    val memberName: String,
    val amount: Double,
    val percentage: Double,
)

data class SharedExpense(
    val id: Long,
    val groupId: Long,
    val title: String,
    val totalAmount: Double,
    val category: ExpenseCategory,
    val paidByMemberId: Long,
    val paidByName: String,
    val splitMode: SplitMode,
    val dateMillis: Long,
    val notes: String,
    val isSettlement: Boolean,
    val splits: List<ExpenseSplitItem>,
)

data class MemberBalance(
    val member: GroupMember,
    val netBalance: Double, // positive = owed money, negative = owes money
)

data class SimplifiedTransfer(
    val fromMember: GroupMember,
    val toMember: GroupMember,
    val amount: Double,
)

data class ExpenseGroupDetail(
    val id: Long,
    val name: String,
    val description: String,
    val emoji: String,
    val createdAt: Long,
    val members: List<GroupMember>,
    val expenses: List<SharedExpense>,
    val memberBalances: List<MemberBalance>,
    val simplifiedTransfers: List<SimplifiedTransfer>,
)

data class PersonalExpense(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val paymentMode: PaymentMode,
    val dateMillis: Long,
    val notes: String,
)

data class ActivityFeedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val category: ExpenseCategory,
    val dateMillis: Long,
    val isPositive: Boolean,
    val isShared: Boolean,
    val isSettlement: Boolean,
)

data class CategoryBreakdownItem(
    val category: ExpenseCategory,
    val amount: Double,
    val percentage: Float,
)

data class DashboardSummary(
    val personalSpendThisMonth: Double,
    val youAreOwed: Double,
    val youOwe: Double,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val totalCategorySpend: Double,
    val recentActivity: List<ActivityFeedItem>,
)

data class ParsedLineItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val assignedMemberNames: Set<String>,
)

data class ParsedExpenseDraft(
    val title: String,
    val category: ExpenseCategory,
    val payerName: String,
    val taxOrTip: Double,
    val items: List<ParsedLineItem>,
    val notes: String,
)

fun formatCurrency(amount: Double, currency: CurrencyOption, showSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(currency.locale).apply {
        val hasFraction = abs(amount % 1.0) > 0.005
        minimumFractionDigits = if (hasFraction) 2 else 0
        maximumFractionDigits = 2
    }
    val formatted = formatter.format(abs(amount))
    val signPrefix = when {
        showSign && amount > 0.005 -> "+"
        amount < -0.005 -> "-"
        else -> ""
    }
    return "$signPrefix${currency.symbol}$formatted"
}

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatFullDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
