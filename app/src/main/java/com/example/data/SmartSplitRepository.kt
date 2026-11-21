package com.example.data

import com.example.data.local.ExpenseGroupEntity
import com.example.data.local.ExpenseSplitEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.PersonalExpenseEntity
import com.example.data.local.SharedExpenseEntity
import com.example.data.local.SmartSplitDao
import com.example.data.preferences.UserPreferences
import com.example.domain.DebtSimplifier
import com.example.model.ActivityFeedItem
import com.example.model.CategoryBreakdownItem
import com.example.model.CurrencyOption
import com.example.model.DashboardSummary
import com.example.model.ExpenseCategory
import com.example.model.ExpenseGroupDetail
import com.example.model.ExpenseSplitItem
import com.example.model.GroupMember
import com.example.model.PaymentMode
import com.example.model.PersonalExpense
import com.example.model.SharedExpense
import com.example.model.SplitMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

class SmartSplitRepository(
    private val dao: SmartSplitDao,
    private val preferences: UserPreferences,
) {
    val currencyFlow: Flow<CurrencyOption> = preferences.currencyFlow

    val personalExpensesFlow: Flow<List<PersonalExpense>> =
        dao.observePersonalExpenses().map { entities ->
            entities.map { entity ->
                PersonalExpense(
                    id = entity.id,
                    title = entity.title,
                    amount = entity.amount,
                    category = ExpenseCategory.fromName(entity.categoryName),
                    paymentMode = PaymentMode.fromName(entity.paymentModeName),
                    dateMillis = entity.dateMillis,
                    notes = entity.notes,
                )
            }
        }

    val groupDetailsFlow: Flow<List<ExpenseGroupDetail>> = combine(
        dao.observeGroups(),
        dao.observeAllMembers(),
        dao.observeAllSharedExpenses(),
        dao.observeAllSplits(),
    ) { groups, members, expenses, splits ->
        val membersByGroup = members.groupBy { it.groupId }
        val splitsByExpense = splits.groupBy { it.expenseId }
        val expensesByGroup = expenses.groupBy { it.groupId }

        groups.map { group ->
            val groupMembers = (membersByGroup[group.id] ?: emptyList()).map { m ->
                GroupMember(
                    id = m.id,
                    groupId = m.groupId,
                    name = m.name,
                    isCurrentUser = m.isCurrentUser,
                )
            }
            val memberNameMap = groupMembers.associateBy { it.id }

            val groupExpenses = (expensesByGroup[group.id] ?: emptyList()).map { exp ->
                val expSplits = (splitsByExpense[exp.id] ?: emptyList()).map { sp ->
                    ExpenseSplitItem(
                        memberId = sp.memberId,
                        memberName = memberNameMap[sp.memberId]?.name ?: "Member",
                        amount = sp.amount,
                        percentage = sp.percentage,
                    )
                }
                SharedExpense(
                    id = exp.id,
                    groupId = exp.groupId,
                    title = exp.title,
                    totalAmount = exp.totalAmount,
                    category = ExpenseCategory.fromName(exp.categoryName),
                    paidByMemberId = exp.paidByMemberId,
                    paidByName = memberNameMap[exp.paidByMemberId]?.name ?: "Member",
                    splitMode = SplitMode.fromName(exp.splitModeName),
                    dateMillis = exp.dateMillis,
                    notes = exp.notes,
                    isSettlement = exp.isSettlement,
                    splits = expSplits,
                )
            }

            val balances = DebtSimplifier.computeMemberBalances(groupMembers, groupExpenses)
            val transfers = DebtSimplifier.simplifyDebts(balances)

            ExpenseGroupDetail(
                id = group.id,
                name = group.name,
                description = group.description,
                emoji = group.emoji,
                createdAt = group.createdAt,
                members = groupMembers,
                expenses = groupExpenses,
                memberBalances = balances,
                simplifiedTransfers = transfers,
            )
        }
    }

    val dashboardSummaryFlow: Flow<DashboardSummary> = combine(
        groupDetailsFlow,
        personalExpensesFlow,
    ) { groups, personal ->
        val calNow = Calendar.getInstance()
        val currentYear = calNow.get(Calendar.YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)

        val personalThisMonth = personal.filter { item ->
            val c = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
            c.get(Calendar.YEAR) == currentYear && c.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.amount }

        var youAreOwed = 0.0
        var youOwe = 0.0

        for (group in groups) {
            val myBalance = group.memberBalances.firstOrNull { it.member.isCurrentUser }?.netBalance ?: 0.0
            if (myBalance > 0.01) {
                youAreOwed += myBalance
            } else if (myBalance < -0.01) {
                youOwe += -myBalance
            }
        }

        // Category breakdown combines personal spend + user's share of non-settlement group expenses
        val categoryTotals = ExpenseCategory.entries.associateWith { 0.0 }.toMutableMap()
        for (p in personal) {
            categoryTotals[p.category] = (categoryTotals[p.category] ?: 0.0) + p.amount
        }
        for (group in groups) {
            val currentUserIds = group.members.filter { it.isCurrentUser }.map { it.id }.toSet()
            for (exp in group.expenses) {
                if (exp.isSettlement) continue
                val userShare = exp.splits.filter { it.memberId in currentUserIds }.sumOf { it.amount }
                if (userShare > 0.0) {
                    categoryTotals[exp.category] = (categoryTotals[exp.category] ?: 0.0) + userShare
                }
            }
        }

        val totalCatSpend = categoryTotals.values.sum()
        val breakdown = ExpenseCategory.entries.map { cat ->
            val amt = categoryTotals[cat] ?: 0.0
            val pct = if (totalCatSpend > 0.0) (amt / totalCatSpend).toFloat() else 0f
            CategoryBreakdownItem(category = cat, amount = amt, percentage = pct)
        }.sortedByDescending { it.amount }

        val personalFeed = personal.map { p ->
            ActivityFeedItem(
                id = "p_${p.id}",
                title = p.title,
                subtitle = "Personal • ${p.paymentMode.label}",
                amount = p.amount,
                category = p.category,
                dateMillis = p.dateMillis,
                isPositive = false,
                isShared = false,
                isSettlement = false,
            )
        }

        val groupFeed = groups.flatMap { g ->
            val currentUserIds = g.members.filter { it.isCurrentUser }.map { it.id }.toSet()
            g.expenses.map { exp ->
                val youPaid = exp.paidByMemberId in currentUserIds
                val mySplit = exp.splits.filter { it.memberId in currentUserIds }.sumOf { it.amount }
                val netEffect = if (youPaid) exp.totalAmount - mySplit else -mySplit
                ActivityFeedItem(
                    id = "g_${exp.id}",
                    title = exp.title,
                    subtitle = if (exp.isSettlement) {
                        "${g.name} • Settlement"
                    } else {
                        "${g.name} • Paid by ${exp.paidByName}"
                    },
                    amount = if (exp.isSettlement) exp.totalAmount else max(0.0, kotlin.math.abs(netEffect)).let {
                        if (it > 0.01) it else exp.totalAmount
                    },
                    category = exp.category,
                    dateMillis = exp.dateMillis,
                    isPositive = netEffect > 0.01,
                    isShared = true,
                    isSettlement = exp.isSettlement,
                )
            }
        }

        val recent = (personalFeed + groupFeed)
            .sortedByDescending { it.dateMillis }
            .take(12)

        DashboardSummary(
            personalSpendThisMonth = (personalThisMonth * 100.0).roundToInt() / 100.0,
            youAreOwed = (youAreOwed * 100.0).roundToInt() / 100.0,
            youOwe = (youOwe * 100.0).roundToInt() / 100.0,
            categoryBreakdown = breakdown,
            totalCategorySpend = totalCatSpend,
            recentActivity = recent,
        )
    }

    suspend fun ensureSeededOnFirstLaunch() {
        if (!preferences.isInitialSeeded() && dao.getGroupCount() == 0) {
            SampleDataSeeder.seed(dao)
            preferences.markInitialSeeded()
        }
    }

    suspend fun resetSampleData() {
        SampleDataSeeder.seed(dao)
        preferences.markInitialSeeded()
    }

    suspend fun setCurrency(currency: CurrencyOption) {
        preferences.setCurrency(currency)
    }

    suspend fun createGroup(name: String, description: String, emoji: String, memberNames: List<String>): Long {
        val groupId = dao.insertGroup(
            ExpenseGroupEntity(
                name = name.trim(),
                description = description.trim(),
                emoji = emoji.ifBlank { "✨" },
                createdAt = System.currentTimeMillis(),
            )
        )
        val cleanNames = (listOf("You") + memberNames.map { it.trim() }.filter { it.isNotBlank() && !it.equals("You", ignoreCase = true) }).distinct()
        dao.insertMembers(
            cleanNames.mapIndexed { idx, memberName ->
                GroupMemberEntity(
                    groupId = groupId,
                    name = memberName,
                    isCurrentUser = idx == 0,
                )
            }
        )
        return groupId
    }

    suspend fun addMemberToGroup(groupId: Long, memberName: String) {
        val trimmed = memberName.trim()
        if (trimmed.isBlank()) return
        dao.insertMember(
            GroupMemberEntity(
                groupId = groupId,
                name = trimmed,
                isCurrentUser = false,
            )
        )
    }

    suspend fun deleteGroup(groupId: Long) {
        dao.deleteGroup(groupId)
    }

    suspend fun addSharedExpense(
        groupId: Long,
        title: String,
        totalAmount: Double,
        category: ExpenseCategory,
        paidByMemberId: Long,
        splitMode: SplitMode,
        splits: List<Pair<Long, Pair<Double, Double>>>, // memberId -> (amount, percentage)
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis(),
    ) {
        val expense = SharedExpenseEntity(
            groupId = groupId,
            title = title.trim(),
            totalAmount = totalAmount,
            categoryName = category.name,
            paidByMemberId = paidByMemberId,
            splitModeName = splitMode.name,
            dateMillis = dateMillis,
            notes = notes.trim(),
            isSettlement = false,
        )
        val splitEntities = splits.map { (memberId, pair) ->
            ExpenseSplitEntity(
                expenseId = 0L,
                memberId = memberId,
                amount = (pair.first * 100.0).roundToInt() / 100.0,
                percentage = pair.second,
            )
        }
        dao.insertSharedExpenseWithSplits(expense, splitEntities)
    }

    suspend fun recordSettlement(
        groupId: Long,
        fromMember: GroupMember,
        toMember: GroupMember,
        amount: Double,
    ) {
        val expense = SharedExpenseEntity(
            groupId = groupId,
            title = "Settled: ${fromMember.name} → ${toMember.name}",
            totalAmount = amount,
            categoryName = ExpenseCategory.UTILITIES.name,
            paidByMemberId = fromMember.id,
            splitModeName = SplitMode.EXACT.name,
            dateMillis = System.currentTimeMillis(),
            notes = "Debt settlement",
            isSettlement = true,
        )
        val splits = listOf(
            ExpenseSplitEntity(
                expenseId = 0L,
                memberId = toMember.id,
                amount = amount,
                percentage = 100.0,
            )
        )
        dao.insertSharedExpenseWithSplits(expense, splits)
    }

    suspend fun deleteSharedExpense(expenseId: Long) {
        dao.deleteSharedExpense(expenseId)
    }

    suspend fun addPersonalExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        paymentMode: PaymentMode,
        dateMillis: Long,
        notes: String,
    ) {
        dao.insertPersonalExpense(
            PersonalExpenseEntity(
                title = title.trim(),
                amount = amount,
                categoryName = category.name,
                paymentModeName = paymentMode.name,
                dateMillis = dateMillis,
                notes = notes.trim(),
            )
        )
    }

    suspend fun deletePersonalExpense(expenseId: Long) {
        dao.deletePersonalExpense(expenseId)
    }
}
