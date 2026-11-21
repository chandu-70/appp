package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActivityFeedItem
import com.example.model.CurrencyOption
import com.example.model.DashboardSummary
import com.example.model.ExpenseCategory
import com.example.model.SmartTab
import com.example.model.formatCurrency
import com.example.model.formatShortDate
import com.example.ui.components.CategoryDonutBreakdown
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

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    currency: CurrencyOption,
    isWideLayout: Boolean,
    onNavigateTab: (SmartTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Summary KPI Cards
        item {
            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryMetricCard(
                        title = "Personal Spend (Month)",
                        amount = formatCurrency(summary.personalSpendThisMonth, currency),
                        subtitle = "Solo daily expenses",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_personal_spend"),
                    )
                    SummaryMetricCard(
                        title = "You Are Owed",
                        amount = formatCurrency(summary.youAreOwed, currency),
                        subtitle = "Net incoming from groups",
                        icon = Icons.Rounded.ArrowDownward,
                        accentColor = EmeraldDark,
                        containerColor = EmeraldSoftBg,
                        borderColor = EmeraldBorder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_you_are_owed"),
                    )
                    SummaryMetricCard(
                        title = "You Owe",
                        amount = formatCurrency(summary.youOwe, currency),
                        subtitle = "Net outgoing debts",
                        icon = Icons.Rounded.ArrowUpward,
                        accentColor = CoralDebt,
                        containerColor = CoralSoftBg,
                        borderColor = CoralBorder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_you_owe"),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryMetricCard(
                        title = "Personal Spend (This Month)",
                        amount = formatCurrency(summary.personalSpendThisMonth, currency),
                        subtitle = "Tracked outside shared group splits",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("metric_personal_spend"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SummaryMetricCard(
                            title = "You Are Owed",
                            amount = formatCurrency(summary.youAreOwed, currency),
                            subtitle = "Receivable",
                            icon = Icons.Rounded.ArrowDownward,
                            accentColor = EmeraldDark,
                            containerColor = EmeraldSoftBg,
                            borderColor = EmeraldBorder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metric_you_are_owed"),
                        )
                        SummaryMetricCard(
                            title = "You Owe",
                            amount = formatCurrency(summary.youOwe, currency),
                            subtitle = "Payable",
                            icon = Icons.Rounded.ArrowUpward,
                            accentColor = CoralDebt,
                            containerColor = CoralSoftBg,
                            borderColor = CoralBorder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metric_you_owe"),
                        )
                    }
                }
            }
        }

        // Quick Action Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { onNavigateTab(SmartTab.AI_PARSER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_scan_receipt_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Receipt / Chat", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = { onNavigateTab(SmartTab.GROUPS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_settle_groups_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Settle Up", maxLines = 1)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Category Breakdown Donut + Activity Feed (Side-by-side on wide screens)
        if (isWideLayout) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    CategoryDonutBreakdown(
                        breakdown = summary.categoryBreakdown,
                        totalSpend = summary.totalCategorySpend,
                        currency = currency,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        modifier = Modifier.weight(1.05f),
                    )
                    FintechCard(modifier = Modifier.weight(0.95f)) {
                        RecentActivityHeader()
                        Spacer(modifier = Modifier.height(12.dp))
                        val filtered = filterActivity(summary.recentActivity, selectedCategory)
                        if (filtered.isEmpty()) {
                            Text(
                                text = "No recent transactions for this filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filtered.take(7).forEach { item ->
                                    ActivityFeedRow(item = item, currency = currency)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                CategoryDonutBreakdown(
                    breakdown = summary.categoryBreakdown,
                    totalSpend = summary.totalCategorySpend,
                    currency = currency,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                )
            }

            item {
                RecentActivityHeader()
            }

            val filtered = filterActivity(summary.recentActivity, selectedCategory)
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No Activity Yet",
                        subtitle = "Add a personal expense or split a group bill to see live activity here.",
                        actionLabel = "Add Personal Expense",
                        onAction = { onNavigateTab(SmartTab.PERSONAL) },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    FintechCard {
                        ActivityFeedRow(item = item, currency = currency)
                    }
                }
            }
        }
    }
}

private fun filterActivity(
    items: List<ActivityFeedItem>,
    selectedCategory: ExpenseCategory?,
): List<ActivityFeedItem> =
    if (selectedCategory == null) items else items.filter { it.category == selectedCategory }

@Composable
private fun SummaryMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    FintechCard(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = borderColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = amount,
            style = TabularAmountStyle.copy(
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentActivityHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Personal & Shared",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityFeedRow(
    item: ActivityFeedItem,
    currency: CurrencyOption,
) {
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isShared) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = if (item.isSettlement) "Settled" else "Group",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "${item.subtitle} • ${formatShortDate(item.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val amountColor = when {
            item.isSettlement -> MaterialTheme.colorScheme.onSurface
            item.isPositive -> EmeraldDark
            else -> CoralDebt
        }
        val formatted = if (item.isSettlement) {
            formatCurrency(item.amount, currency)
        } else if (item.isPositive) {
            "+${formatCurrency(item.amount, currency)}"
        } else {
            "-${formatCurrency(item.amount, currency)}"
        }
        Text(
            text = formatted,
            style = TabularAmountStyle.copy(
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = amountColor,
        )
    }
}
