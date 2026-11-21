package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.SmartTab
import com.example.ui.components.CurrencyPickerDialog
import com.example.ui.components.ResetSampleDataDialog
import com.example.ui.screens.AiParserScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GroupsSplitsScreen
import com.example.ui.screens.PersonalTrackerScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSoftBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSplitApp(
    viewModel: SmartSplitViewModel = viewModel(),
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val personalExpenses by viewModel.personalExpenses.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()

    val aiInputMode by viewModel.aiInputMode.collectAsStateWithLifecycle()
    val selectedReceiptUri by viewModel.selectedReceiptUri.collectAsStateWithLifecycle()
    val chatInputText by viewModel.chatInputText.collectAsStateWithLifecycle()
    val aiTargetGroup by viewModel.aiTargetGroup.collectAsStateWithLifecycle()
    val aiSelectedPayerId by viewModel.aiSelectedPayerId.collectAsStateWithLifecycle()
    val aiParserState by viewModel.aiParserState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Return to Dashboard on system back when on secondary tabs
    BackHandler(enabled = currentTab != SmartTab.DASHBOARD) {
        viewModel.selectTab(SmartTab.DASHBOARD)
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            current = currency,
            onSelect = viewModel::selectCurrency,
            onDismiss = { showCurrencyDialog = false },
        )
    }

    if (showResetDialog) {
        ResetSampleDataDialog(
            onConfirm = viewModel::resetSampleData,
            onDismiss = { showResetDialog = false },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "SX",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldDark,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = stringResource(R.string.app_tagline),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        actions = {
                            FilledTonalButton(
                                onClick = { showCurrencyDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("currency_selector_button"),
                            ) {
                                Text(
                                    text = "${currency.symbol} ${currency.code}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            IconButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.testTag("reset_sample_data_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = stringResource(R.string.reset_sample_data),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            },
            bottomBar = {
                if (!isWideLayout) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp,
                    ) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            ) {
                                SmartTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = {
                                            Icon(
                                                imageVector = tabIcon(tab, selected),
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isWideLayout) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        Spacer(modifier = Modifier.size(12.dp))
                        SmartTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Crossfade(
                        targetState = currentTab,
                        label = "tabCrossfade",
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1100.dp),
                    ) { tab ->
                        when (tab) {
                            SmartTab.DASHBOARD -> DashboardScreen(
                                summary = dashboardSummary,
                                currency = currency,
                                isWideLayout = isWideLayout,
                                onNavigateTab = viewModel::selectTab,
                            )

                            SmartTab.GROUPS -> GroupsSplitsScreen(
                                groups = groups,
                                selectedGroup = selectedGroup,
                                currency = currency,
                                isWideLayout = isWideLayout,
                                onSelectGroup = viewModel::selectGroup,
                                onCreateGroup = viewModel::createGroup,
                                onAddMember = viewModel::addMember,
                                onAddSharedExpense = viewModel::addSharedExpense,
                                onMarkSettled = viewModel::markSettled,
                                onDeleteExpense = viewModel::deleteSharedExpense,
                            )

                            SmartTab.AI_PARSER -> AiParserScreen(
                                inputMode = aiInputMode,
                                selectedReceiptUri = selectedReceiptUri,
                                chatInputText = chatInputText,
                                parserState = aiParserState,
                                groups = groups,
                                targetGroup = aiTargetGroup,
                                selectedPayerId = aiSelectedPayerId,
                                currency = currency,
                                onSelectInputMode = viewModel::setAiInputMode,
                                onSelectReceiptUri = viewModel::setReceiptUri,
                                onUpdateChatText = viewModel::setChatInputText,
                                onFillSampleChat = viewModel::fillSampleChat,
                                onAnalyzeReceipt = viewModel::analyzeReceiptImage,
                                onAnalyzeChat = viewModel::analyzeChatText,
                                onSelectTargetGroup = viewModel::selectAiTargetGroup,
                                onSelectPayer = viewModel::selectAiPayer,
                                onUpdateDraftMeta = viewModel::updateDraftMeta,
                                onUpdateDraftItem = viewModel::updateDraftItem,
                                onToggleItemMember = viewModel::toggleItemMember,
                                onAddDraftItem = viewModel::addEmptyDraftItem,
                                onRemoveDraftItem = viewModel::removeDraftItem,
                                onClearDraft = viewModel::clearAiDraft,
                                onCommitToGroup = viewModel::commitParsedDraftToGroup,
                            )

                            SmartTab.PERSONAL -> PersonalTrackerScreen(
                                expenses = personalExpenses,
                                currency = currency,
                                onAddPersonalExpense = viewModel::addPersonalExpense,
                                onDeleteExpense = viewModel::deletePersonalExpense,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: SmartTab, selected: Boolean): ImageVector = when (tab) {
    SmartTab.DASHBOARD -> if (selected) Icons.Rounded.Dashboard else Icons.Outlined.Dashboard
    SmartTab.GROUPS -> if (selected) Icons.Rounded.Groups else Icons.Outlined.Groups
    SmartTab.AI_PARSER -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
    SmartTab.PERSONAL -> if (selected) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
}
