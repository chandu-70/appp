package com.example.domain

import com.example.model.GroupMember
import com.example.model.MemberBalance
import com.example.model.SharedExpense
import com.example.model.SimplifiedTransfer
import kotlin.math.min
import kotlin.math.roundToInt

object DebtSimplifier {

    fun computeMemberBalances(
        members: List<GroupMember>,
        expenses: List<SharedExpense>,
    ): List<MemberBalance> {
        if (members.isEmpty()) return emptyList()
        val netByMemberId = members.associate { it.id to 0.0 }.toMutableMap()

        for (expense in expenses) {
            // Payer gets credited the full amount they paid
            netByMemberId[expense.paidByMemberId] =
                (netByMemberId[expense.paidByMemberId] ?: 0.0) + expense.totalAmount

            // Each split participant owes their share
            for (split in expense.splits) {
                netByMemberId[split.memberId] =
                    (netByMemberId[split.memberId] ?: 0.0) - split.amount
            }
        }

        return members.map { member ->
            val raw = netByMemberId[member.id] ?: 0.0
            val rounded = (raw * 100.0).roundToInt() / 100.0
            MemberBalance(member = member, netBalance = rounded)
        }
    }

    fun simplifyDebts(balances: List<MemberBalance>): List<SimplifiedTransfer> {
        data class Entry(val member: GroupMember, var amount: Double)

        val creditors = balances
            .filter { it.netBalance > 0.01 }
            .map { Entry(it.member, it.netBalance) }
            .sortedByDescending { it.amount }
            .toMutableList()

        val debtors = balances
            .filter { it.netBalance < -0.01 }
            .map { Entry(it.member, -it.netBalance) }
            .sortedByDescending { it.amount }
            .toMutableList()

        val transfers = mutableListOf<SimplifiedTransfer>()
        var cIdx = 0
        var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val creditor = creditors[cIdx]
            val debtor = debtors[dIdx]

            val transferAmt = min(creditor.amount, debtor.amount)
            val roundedAmt = (transferAmt * 100.0).roundToInt() / 100.0

            if (roundedAmt > 0.01) {
                transfers.add(
                    SimplifiedTransfer(
                        fromMember = debtor.member,
                        toMember = creditor.member,
                        amount = roundedAmt,
                    )
                )
            }

            creditor.amount -= transferAmt
            debtor.amount -= transferAmt

            if (creditor.amount <= 0.01) cIdx++
            if (debtor.amount <= 0.01) dIdx++
        }

        return transfers
    }
}
