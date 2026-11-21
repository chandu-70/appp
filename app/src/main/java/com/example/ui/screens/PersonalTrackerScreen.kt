package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyOption
import com.example.model.ExpenseCategory
import com.example.model.PaymentMode
import com.example.model.PersonalExpense
import com.example.model.formatCurrency
import com.example.model.formatFullDate
import com.example.model.formatShortDate
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechCard
import com.example.ui.theme.CoralDebt
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.TabularAmountStyle
import java.util.Calendar

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    THIS_WEEK("Last 7 Days"),
    THIS_MONTH("This Month"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalTrackerScreen(
    expenses: List<PersonalExpense>,
    currency: CurrencyOption,
    onAddPersonalExpense: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        paymentMode: PaymentMode,
        dateMillis: Long,
        notes: String,
    ) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.UPI) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var filterDateRange by remember { mutableStateOf(DateRangeFilter.ALL) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val filteredExpenses = remember(expenses, searchQuery, filterCategory, filterDateRange) {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24L * 60L * 60L * 1000L
        val calNow = Calendar.getInstance()
        val curYear = calNow.get(Calendar.YEAR)
        val curMonth = calNow.get(Calendar.MONTH)

        expenses.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true)
            val matchesCat = filterCategory == null || item.category == filterCategory
            val matchesDate = when (filterDateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.THIS_WEEK -> item.dateMillis >= sevenDaysAgo
                DateRangeFilter.THIS_MONTH -> {
                    val c = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                    c.get(Calendar.YEAR) == curYear && c.get(Calendar.MONTH) == curMonth
                }
            }
            matchesSearch && matchesCat && matchesDate
        }
    }

    val filteredTotal = filteredExpenses.sumOf { it.amount }
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSubmit = title.isNotBlank() && parsedAmount > 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Quick-Add Personal Expense Form
        item {
            FintechCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick-Add Personal Expense",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Track solo spending separately from shared group bills",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountText = input
                            }
                        },
                        label = { Text("Amount (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("personal_amount_input"),
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g., Coffee") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("personal_title_input"),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExpenseCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Payment Mode", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedPaymentMode == mode,
                            onClick = { selectedPaymentMode = mode },
                            label = { Text(mode.label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("personal_date_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Select date",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(formatFullDate(selectedDateMillis))
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onAddPersonalExpense(
                            title,
                            parsedAmount,
                            selectedCategory,
                            selectedPaymentMode,
                            selectedDateMillis,
                            notes,
                        )
                        title = ""
                        amountText = ""
                        notes = ""
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_personal_expense_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Personal Expense")
                }
            }
        }

        // Search & Filter Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search personal expenses or notes...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personal_search_input"),
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DateRangeFilter.entries) { range ->
                        FilterChip(
                            selected = filterDateRange == range,
                            onClick = { filterDateRange = range },
                            label = { Text(range.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSoftBg,
                                selectedLabelColor = EmeraldDark,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { filterCategory = null },
                            label = { Text("All Categories") },
                        )
                    }
                    items(ExpenseCategory.entries) { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = {
                                filterCategory = if (filterCategory == cat) null else cat
                            },
                            label = { Text(cat.label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${filteredExpenses.size} Expense(s)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "Total: ${formatCurrency(filteredTotal, currency)}",
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Receipt,
                    title = "No Matching Personal Expenses",
                    subtitle = if (expenses.isEmpty()) {
                        "Use the Quick-Add form above to log your daily solo spending."
                    } else {
                        "Try clearing your search or category/date filters."
                    },
                    actionLabel = if (expenses.isNotEmpty()) "Clear Filters" else null,
                    onAction = if (expenses.isNotEmpty()) {
                        {
                            searchQuery = ""
                            filterCategory = null
                            filterDateRange = DateRangeFilter.ALL
                        }
                    } else null,
                )
            }
        } else {
            items(filteredExpenses, key = { it.id }) { item ->
                FintechCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIconBadge(category = item.category, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.category.label} • ${item.paymentMode.label} • ${formatShortDate(item.dateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = item.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatCurrency(item.amount, currency),
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = CoralDebt,
                        )
                        IconButton(
                            onClick = { onDeleteExpense(item.id) },
                            modifier = Modifier.size(36.dp),
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
    }
}
