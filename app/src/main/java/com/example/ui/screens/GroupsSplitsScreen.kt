package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyOption
import com.example.model.ExpenseCategory
import com.example.model.ExpenseGroupDetail
import com.example.model.GroupMember
import com.example.model.SharedExpense
import com.example.model.SplitMode
import com.example.model.formatCurrency
import com.example.model.formatShortDate
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechCard
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralDebt
import com.example.ui.theme.CoralSoftBg
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.TabularAmountStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupsSplitsScreen(
    groups: List<ExpenseGroupDetail>,
    selectedGroup: ExpenseGroupDetail?,
    currency: CurrencyOption,
    isWideLayout: Boolean,
    onSelectGroup: (Long) -> Unit,
    onCreateGroup: (name: String, description: String, emoji: String, membersCsv: String) -> Unit,
    onAddMember: (groupId: Long, memberName: String) -> Unit,
    onAddSharedExpense: (
        groupId: Long,
        title: String,
        totalAmount: Double,
        category: ExpenseCategory,
        paidByMemberId: Long,
        splitMode: SplitMode,
        splits: List<Pair<Long, Pair<Double, Double>>>,
        notes: String,
    ) -> Unit,
    onMarkSettled: (groupId: Long, from: GroupMember, to: GroupMember, amount: Double) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onCreate = onCreateGroup,
            onDismiss = { showCreateGroupDialog = false },
        )
    }

    if (showAddMemberDialog && selectedGroup != null) {
        AddMemberDialog(
            groupName = selectedGroup.name,
            onAddMember = { memberName -> onAddMember(selectedGroup.id, memberName) },
            onDismiss = { showAddMemberDialog = false },
        )
    }

    if (showAddExpenseSheet && selectedGroup != null) {
        AddSharedExpenseSheet(
            group = selectedGroup,
            currency = currency,
            onSave = { title, total, cat, paidBy, mode, splits, notes ->
                onAddSharedExpense(selectedGroup.id, title, total, cat, paidBy, mode, splits, notes)
            },
            onDismiss = { showAddExpenseSheet = false },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Group Selector Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Your Groups",
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledTonalButton(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier.testTag("create_group_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (groups.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups, key = { it.id }) { group ->
                        val isSelected = selectedGroup?.id == group.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectGroup(group.id) },
                            label = {
                                Text(
                                    text = "${group.emoji} ${group.name}",
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSoftBg,
                                selectedLabelColor = EmeraldDark,
                            ),
                            modifier = Modifier.testTag("group_chip_${group.id}"),
                        )
                    }
                }
            }
        }

        if (selectedGroup == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Groups,
                    title = "No Expense Groups Yet",
                    subtitle = "Create a group for roommates, trips, or dinners to split expenses and simplify debts.",
                    actionLabel = "Create First Group",
                    onAction = { showCreateGroupDialog = true },
                )
            }
        } else {
            // Selected Group Header & Members
            item {
                FintechCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedGroup.emoji} ${selectedGroup.name}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (selectedGroup.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedGroup.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showAddExpenseSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_shared_expense_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Split Bill")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Member Balances (${selectedGroup.members.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { showAddMemberDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_member_button"),
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Member", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedGroup.memberBalances.forEach { bal ->
                            val isPositive = bal.netBalance > 0.01
                            val isNegative = bal.netBalance < -0.01
                            val bg = when {
                                isPositive -> EmeraldSoftBg
                                isNegative -> CoralSoftBg
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val border = when {
                                isPositive -> EmeraldBorder
                                isNegative -> CoralBorder
                                else -> MaterialTheme.colorScheme.outline
                            }
                            val textColor = when {
                                isPositive -> EmeraldDark
                                isNegative -> CoralDebt
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = bg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, border),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = bal.member.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = when {
                                            isPositive -> "+${formatCurrency(bal.netBalance, currency)}"
                                            isNegative -> "-${formatCurrency(-bal.netBalance, currency)}"
                                            else -> "Settled"
                                        },
                                        style = TabularAmountStyle.copy(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = textColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Simplify Debts / Settle Up Card
            item {
                FintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (selectedGroup.simplifiedTransfers.isEmpty()) {
                        EmeraldSoftBg
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    borderColor = if (selectedGroup.simplifiedTransfers.isEmpty()) {
                        EmeraldBorder
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Simplify Debts & Settle Up",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (selectedGroup.simplifiedTransfers.isEmpty()) {
                                    "All balances in ${selectedGroup.name} are settled!"
                                } else {
                                    "Minimum ${selectedGroup.simplifiedTransfers.size} transfer(s) needed to settle all balances"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selectedGroup.simplifiedTransfers.isEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "All settled",
                                tint = EmeraldDark,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    if (selectedGroup.simplifiedTransfers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            selectedGroup.simplifiedTransfers.forEachIndexed { idx, transfer ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = transfer.fromMember.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = CoralDebt,
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                    contentDescription = "owes",
                                                    modifier = Modifier
                                                        .padding(horizontal = 6.dp)
                                                        .size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    text = transfer.toMember.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = EmeraldDark,
                                                )
                                            }
                                            Text(
                                                text = formatCurrency(transfer.amount, currency),
                                                style = TabularAmountStyle.copy(
                                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onMarkSettled(
                                                    selectedGroup.id,
                                                    transfer.fromMember,
                                                    transfer.toMember,
                                                    transfer.amount,
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("mark_settled_button_$idx"),
                                        ) {
                                            Text("Mark as Settled", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shared Expenses History
            item {
                Text(
                    text = "Shared Expenses (${selectedGroup.expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (selectedGroup.expenses.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Add,
                        title = "No Shared Bills Yet",
                        subtitle = "Tap \"Split Bill\" above or parse a receipt with AI to log shared expenses.",
                        actionLabel = "Split a Bill",
                        onAction = { showAddExpenseSheet = true },
                    )
                }
            } else {
                items(selectedGroup.expenses, key = { it.id }) { expense ->
                    SharedExpenseRowCard(
                        expense = expense,
                        currency = currency,
                        onDelete = { onDeleteExpense(expense.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedExpenseRowCard(
    expense: SharedExpense,
    currency: CurrencyOption,
    onDelete: () -> Unit,
) {
    FintechCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expense.isSettlement) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldSoftBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Settlement",
                        tint = EmeraldDark,
                    )
                }
            } else {
                CategoryIconBadge(category = expense.category, size = 40.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (expense.isSettlement) {
                        "Settlement • ${formatShortDate(expense.dateMillis)}"
                    } else {
                        "Paid by ${expense.paidByName} • ${expense.splitMode.label} • ${formatShortDate(expense.dateMillis)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!expense.isSettlement && expense.splits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = expense.splits.joinToString(" • ") {
                            "${it.memberName}: ${formatCurrency(it.amount, currency)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.totalAmount, currency),
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (expense.isSettlement) EmeraldDark else MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
