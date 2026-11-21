package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyOption
import com.example.model.ExpenseCategory
import com.example.model.ExpenseGroupDetail
import com.example.model.SplitMode
import com.example.model.formatCurrency
import com.example.ui.theme.CoralDebt
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.TabularAmountStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CreateGroupDialog(
    onCreate: (name: String, description: String, emoji: String, membersCsv: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🏠") }
    var membersCsv by remember { mutableStateOf("Aarav, Priya") }
    val emojis = listOf("🏠", "🌴", "✈️", "🍕", "🎉", "💼")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { emoji ->
                        FilterChip(
                            selected = selectedEmoji == emoji,
                            onClick = { selectedEmoji = emoji },
                            label = { Text(emoji) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g., Weekend Roadtrip") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_group_name_input"),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = membersCsv,
                    onValueChange = { membersCsv = it },
                    label = { Text("Other Members (comma separated)") },
                    supportingText = { Text("\"You\" is included automatically") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_group_members_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, selectedEmoji, membersCsv)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_group_button"),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun AddMemberDialog(
    groupName: String,
    onAddMember: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var memberName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member to $groupName") },
        text = {
            OutlinedTextField(
                value = memberName,
                onValueChange = { memberName = it },
                label = { Text("Member Name") },
                placeholder = { Text("e.g., Neha") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_member_name_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (memberName.isNotBlank()) {
                        onAddMember(memberName)
                        onDismiss()
                    }
                },
                enabled = memberName.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_member_button"),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSharedExpenseSheet(
    group: ExpenseGroupDetail,
    currency: CurrencyOption,
    onSave: (
        title: String,
        totalAmount: Double,
        category: ExpenseCategory,
        paidByMemberId: Long,
        splitMode: SplitMode,
        splits: List<Pair<Long, Pair<Double, Double>>>,
        notes: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var paidByMemberId by remember {
        mutableStateOf(
            group.members.firstOrNull { it.isCurrentUser }?.id
                ?: group.members.firstOrNull()?.id
                ?: 0L
        )
    }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    val selectedParticipantIds = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            group.members.forEach { put(it.id, true) }
        }
    }
    val exactInputs = remember {
        mutableStateMapOf<Long, String>().apply {
            group.members.forEach { put(it.id, "") }
        }
    }
    val percentInputs = remember {
        mutableStateMapOf<Long, String>().apply {
            val defaultPct = if (group.members.isNotEmpty()) {
                ((100.0 / group.members.size) * 100.0).roundToInt() / 100.0
            } else 0.0
            group.members.forEach { put(it.id, defaultPct.toString()) }
        }
    }

    val parsedTotal = amountText.toDoubleOrNull() ?: 0.0
    val activeParticipants = group.members.filter { selectedParticipantIds[it.id] == true }

    val exactSum = activeParticipants.sumOf { exactInputs[it.id]?.toDoubleOrNull() ?: 0.0 }
    val percentSum = activeParticipants.sumOf { percentInputs[it.id]?.toDoubleOrNull() ?: 0.0 }

    val splitValidationError: String? = when {
        title.isBlank() -> "Enter a title"
        parsedTotal <= 0.0 -> "Enter a valid amount > 0"
        activeParticipants.isEmpty() -> "Select at least one participant"
        splitMode == SplitMode.EXACT && abs(exactSum - parsedTotal) > 0.05 ->
            "Exact amounts sum to ${formatCurrency(exactSum, currency)} (must equal ${formatCurrency(parsedTotal, currency)})"
        splitMode == SplitMode.PERCENT && abs(percentSum - 100.0) > 0.2 ->
            "Percentages sum to ${"%.1f".format(percentSum)}% (must equal 100%)"
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Add Shared Expense • ${group.name}",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title") },
                placeholder = { Text("e.g., Dinner at Bademiya") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shared_expense_title_input"),
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text("Total Amount (${currency.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shared_expense_amount_input"),
            )

            // Category
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                    )
                }
            }

            // Paid By
            Text("Paid By", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                group.members.forEach { member ->
                    FilterChip(
                        selected = paidByMemberId == member.id,
                        onClick = { paidByMemberId = member.id },
                        label = { Text(member.name) },
                    )
                }
            }

            // Split Mode Segmented Toggle
            Text("Split Mode", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SplitMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = splitMode == mode,
                        onClick = { splitMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SplitMode.entries.size,
                        ),
                        modifier = Modifier.testTag("split_mode_${mode.name.lowercase()}"),
                    ) {
                        Text(
                            text = when (mode) {
                                SplitMode.EQUAL -> "Equal"
                                SplitMode.EXACT -> "Exact"
                                SplitMode.PERCENT -> "Percent"
                            },
                            maxLines = 1,
                        )
                    }
                }
            }

            // Participants & Per-Member Split Inputs
            Text("Participants", style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.members.forEach { member ->
                    val included = selectedParticipantIds[member.id] == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FilterChip(
                            selected = included,
                            onClick = { selectedParticipantIds[member.id] = !included },
                            label = { Text(member.name) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        when {
                            !included -> {
                                Text(
                                    text = "Excluded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            splitMode == SplitMode.EQUAL -> {
                                val equalAmt = if (activeParticipants.isNotEmpty() && parsedTotal > 0.0) {
                                    parsedTotal / activeParticipants.size
                                } else 0.0
                                Text(
                                    text = formatCurrency(equalAmt, currency),
                                    style = TabularAmountStyle.copy(fontWeight = FontWeight.SemiBold),
                                    color = EmeraldDark,
                                )
                            }
                            splitMode == SplitMode.EXACT -> {
                                OutlinedTextField(
                                    value = exactInputs[member.id].orEmpty(),
                                    onValueChange = { v ->
                                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                            exactInputs[member.id] = v
                                        }
                                    },
                                    prefix = { Text(currency.symbol) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(136.dp),
                                )
                            }
                            splitMode == SplitMode.PERCENT -> {
                                OutlinedTextField(
                                    value = percentInputs[member.id].orEmpty(),
                                    onValueChange = { v ->
                                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                            percentInputs[member.id] = v
                                        }
                                    },
                                    suffix = { Text("%") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(120.dp),
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (splitValidationError != null && (title.isNotBlank() || amountText.isNotBlank())) {
                Text(
                    text = splitValidationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralDebt,
                )
            }

            Button(
                onClick = {
                    val computedSplits: List<Pair<Long, Pair<Double, Double>>> = when (splitMode) {
                        SplitMode.EQUAL -> {
                            val count = activeParticipants.size
                            val base = ((parsedTotal / count) * 100.0).roundToInt() / 100.0
                            val pct = ((100.0 / count) * 100.0).roundToInt() / 100.0
                            val list = activeParticipants.mapIndexed { idx, m ->
                                val amt = if (idx == 0) {
                                    ((parsedTotal - base * (count - 1)) * 100.0).roundToInt() / 100.0
                                } else base
                                m.id to (amt to pct)
                            }
                            list
                        }
                        SplitMode.EXACT -> {
                            activeParticipants.map { m ->
                                val amt = exactInputs[m.id]?.toDoubleOrNull() ?: 0.0
                                val pct = if (parsedTotal > 0.0) ((amt / parsedTotal) * 10000.0).roundToInt() / 100.0 else 0.0
                                m.id to (amt to pct)
                            }
                        }
                        SplitMode.PERCENT -> {
                            activeParticipants.mapIndexed { idx, m ->
                                val pct = percentInputs[m.id]?.toDoubleOrNull() ?: 0.0
                                val amt = ((parsedTotal * pct / 100.0) * 100.0).roundToInt() / 100.0
                                m.id to (amt to pct)
                            }
                        }
                    }
                    onSave(title, parsedTotal, selectedCategory, paidByMemberId, splitMode, computedSplits, notes)
                    onDismiss()
                },
                enabled = splitValidationError == null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_shared_expense_button"),
            ) {
                Text("Save Shared Expense")
            }
        }
    }
}
