package com.example

import com.example.domain.DebtSimplifier
import com.example.model.ExpenseCategory
import com.example.model.ExpenseSplitItem
import com.example.model.GroupMember
import com.example.model.SharedExpense
import com.example.model.SplitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtSimplifierTest {

    @Test
    fun simplifyDebts_computesMinimumTransfers_andClearsAfterSettlement() {
        val you = GroupMember(1L, 1L, "You", true)
        val aarav = GroupMember(2L, 1L, "Aarav", false)
        val priya = GroupMember(3L, 1L, "Priya", false)
        val members = listOf(you, aarav, priya)

        val dinner = SharedExpense(
            id = 10L,
            groupId = 1L,
            title = "Dinner",
            totalAmount = 900.0,
            category = ExpenseCategory.FOOD,
            paidByMemberId = you.id,
            paidByName = you.name,
            splitMode = SplitMode.EQUAL,
            dateMillis = 1000L,
            notes = "",
            isSettlement = false,
            splits = members.map { ExpenseSplitItem(it.id, it.name, 300.0, 33.33) },
        )

        val cab = SharedExpense(
            id = 11L,
            groupId = 1L,
            title = "Cab",
            totalAmount = 300.0,
            category = ExpenseCategory.TRAVEL,
            paidByMemberId = aarav.id,
            paidByName = aarav.name,
            splitMode = SplitMode.EQUAL,
            dateMillis = 2000L,
            notes = "",
            isSettlement = false,
            splits = members.map { ExpenseSplitItem(it.id, it.name, 100.0, 33.33) },
        )

        val balances = DebtSimplifier.computeMemberBalances(members, listOf(dinner, cab))
        // You paid 900, share = 400 -> +500
        // Aarav paid 300, share = 400 -> -100
        // Priya paid 0, share = 400 -> -400
        assertEquals(500.0, balances.first { it.member.id == you.id }.netBalance, 0.01)
        assertEquals(-100.0, balances.first { it.member.id == aarav.id }.netBalance, 0.01)
        assertEquals(-400.0, balances.first { it.member.id == priya.id }.netBalance, 0.01)

        val transfers = DebtSimplifier.simplifyDebts(balances)
        assertEquals(2, transfers.size)
        assertEquals(400.0, transfers.first { it.fromMember.id == priya.id }.amount, 0.01)
        assertEquals(100.0, transfers.first { it.fromMember.id == aarav.id }.amount, 0.01)

        // Apply settlement for Priya -> You 400
        val settlement = SharedExpense(
            id = 12L,
            groupId = 1L,
            title = "Settled: Priya -> You",
            totalAmount = 400.0,
            category = ExpenseCategory.UTILITIES,
            paidByMemberId = priya.id,
            paidByName = priya.name,
            splitMode = SplitMode.EXACT,
            dateMillis = 3000L,
            notes = "",
            isSettlement = true,
            splits = listOf(ExpenseSplitItem(you.id, you.name, 400.0, 100.0)),
        )

        val afterBalances = DebtSimplifier.computeMemberBalances(members, listOf(dinner, cab, settlement))
        val afterTransfers = DebtSimplifier.simplifyDebts(afterBalances)
        assertEquals(1, afterTransfers.size)
        assertEquals(aarav.id, afterTransfers[0].fromMember.id)
        assertEquals(you.id, afterTransfers[0].toMember.id)
        assertEquals(100.0, afterTransfers[0].amount, 0.01)

        val zeroBalances = DebtSimplifier.computeMemberBalances(
            members,
            listOf(
                dinner,
                cab,
                settlement,
                settlement.copy(
                    id = 13L,
                    paidByMemberId = aarav.id,
                    paidByName = aarav.name,
                    splits = listOf(ExpenseSplitItem(you.id, you.name, 100.0, 100.0)),
                ),
            ),
        )
        assertTrue(DebtSimplifier.simplifyDebts(zeroBalances).isEmpty())
    }
}
