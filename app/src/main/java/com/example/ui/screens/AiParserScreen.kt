package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CurrencyOption
import com.example.model.ExpenseCategory
import com.example.model.ExpenseGroupDetail
import com.example.model.ParsedExpenseDraft
import com.example.model.ParsedLineItem
import com.example.model.formatCurrency
import com.example.ui.AiInputMode
import com.example.ui.AiParserUiState
import com.example.ui.components.FintechCard
import com.example.ui.components.ShimmerSkeletonBox
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralDebt
import com.example.ui.theme.CoralSoftBg
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.TabularAmountStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiParserScreen(
    inputMode: AiInputMode,
    selectedReceiptUri: Uri?,
    chatInputText: String,
    parserState: AiParserUiState,
    groups: List<ExpenseGroupDetail>,
    targetGroup: ExpenseGroupDetail?,
    selectedPayerId: Long?,
    currency: CurrencyOption,
    onSelectInputMode: (AiInputMode) -> Unit,
    onSelectReceiptUri: (Uri?) -> Unit,
    onUpdateChatText: (String) -> Unit,
    onFillSampleChat: () -> Unit,
    onAnalyzeReceipt: () -> Unit,
    onAnalyzeChat: () -> Unit,
    onSelectTargetGroup: (Long) -> Unit,
    onSelectPayer: (Long) -> Unit,
    onUpdateDraftMeta: (title: String?, category: ExpenseCategory?, taxOrTip: Double?) -> Unit,
    onUpdateDraftItem: (itemId: String, name: String, quantity: Int, totalPrice: Double) -> Unit,
    onToggleItemMember: (itemId: String, memberName: String) -> Unit,
    onAddDraftItem: () -> Unit,
    onRemoveDraftItem: (String) -> Unit,
    onClearDraft: () -> Unit,
    onCommitToGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectReceiptUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header + Target Group + Mode Toggle
        item {
            FintechCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldSoftBg,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = EmeraldDark,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "AI Receipt & Chat Parser",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Extract line items, prices, tax & participants automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Group selector
                if (groups.isNotEmpty()) {
                    Text(
                        text = "Target Group",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(groups, key = { it.id }) { group ->
                            FilterChip(
                                selected = targetGroup?.id == group.id,
                                onClick = { onSelectTargetGroup(group.id) },
                                label = { Text("${group.emoji} ${group.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSoftBg,
                                    selectedLabelColor = EmeraldDark,
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Input Mode Segmented Toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = inputMode == AiInputMode.RECEIPT_IMAGE,
                        onClick = { onSelectInputMode(AiInputMode.RECEIPT_IMAGE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("ai_mode_receipt"),
                    ) {
                        Text("Receipt Image", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = inputMode == AiInputMode.MESSY_CHAT,
                        onClick = { onSelectInputMode(AiInputMode.MESSY_CHAT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("ai_mode_chat"),
                    ) {
                        Text("Messy Chat Log", maxLines = 1)
                    }
                }
            }
        }

        // Input Card
        item {
            if (inputMode == AiInputMode.RECEIPT_IMAGE) {
                FintechCard(modifier = Modifier.fillMaxWidth()) {
                    if (selectedReceiptUri == null) {
                        Image(
                            painter = painterResource(id = R.drawable.img_receipt_hero),
                            contentDescription = "Receipt scanner preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Upload a restaurant bill, grocery receipt, or booking screenshot.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_receipt_image_button"),
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Receipt Image")
                        }
                    } else {
                        Text("Receipt Preview", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = selectedReceiptUri,
                            contentDescription = "Selected receipt image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Change Image")
                            }
                            Button(
                                onClick = onAnalyzeReceipt,
                                enabled = parserState !is AiParserUiState.Loading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("analyze_receipt_button"),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Receipt")
                            }
                        }
                    }
                }
            } else {
                FintechCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Paste WhatsApp / Group Chat",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        FilledTonalButton(
                            onClick = onFillSampleChat,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("try_sample_chat_button"),
                        ) {
                            Text("Try Sample Chat", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chatInputText,
                        onValueChange = onUpdateChatText,
                        placeholder = {
                            Text("e.g., Aarav paid 1850 for pizza split bw all 4; You booked cab 640 for You and Priya...")
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_log_input"),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAnalyzeChat,
                        enabled = chatInputText.isNotBlank() && parserState !is AiParserUiState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("parse_chat_button"),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parse Chat Log")
                    }
                }
            }
        }

        // Loading Skeleton / Error / Editable Review Table
        when (parserState) {
            is AiParserUiState.Idle -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Parsed line items will appear here in an editable table so you can assign friends and confirm before adding to a group.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            is AiParserUiState.Loading -> {
                item {
                    FintechCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = parserState.modeLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldDark,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth(0.65f).height(22.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth().height(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth().height(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth(0.85f).height(42.dp))
                    }
                }
            }

            is AiParserUiState.Error -> {
                item {
                    FintechCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = CoralSoftBg,
                        borderColor = CoralBorder,
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Error",
                                tint = CoralDebt,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Parsing Failed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CoralDebt,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = parserState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (inputMode == AiInputMode.RECEIPT_IMAGE) {
                                        onAnalyzeReceipt()
                                    } else {
                                        onAnalyzeChat()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry")
                            }
                            TextButton(onClick = onClearDraft) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            is AiParserUiState.Success -> {
                item {
                    EditableParsedReviewCard(
                        draft = parserState.draft,
                        targetGroup = targetGroup,
                        selectedPayerId = selectedPayerId,
                        currency = currency,
                        onSelectPayer = onSelectPayer,
                        onUpdateDraftMeta = onUpdateDraftMeta,
                        onUpdateDraftItem = onUpdateDraftItem,
                        onToggleItemMember = onToggleItemMember,
                        onAddDraftItem = onAddDraftItem,
                        onRemoveDraftItem = onRemoveDraftItem,
                        onClearDraft = onClearDraft,
                        onCommitToGroup = onCommitToGroup,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableParsedReviewCard(
    draft: ParsedExpenseDraft,
    targetGroup: ExpenseGroupDetail?,
    selectedPayerId: Long?,
    currency: CurrencyOption,
    onSelectPayer: (Long) -> Unit,
    onUpdateDraftMeta: (title: String?, category: ExpenseCategory?, taxOrTip: Double?) -> Unit,
    onUpdateDraftItem: (itemId: String, name: String, quantity: Int, totalPrice: Double) -> Unit,
    onToggleItemMember: (itemId: String, memberName: String) -> Unit,
    onAddDraftItem: () -> Unit,
    onRemoveDraftItem: (String) -> Unit,
    onClearDraft: () -> Unit,
    onCommitToGroup: () -> Unit,
) {
    val members = targetGroup?.members.orEmpty()
    val subtotal = draft.items.sumOf { it.totalPrice }
    val grandTotal = subtotal + draft.taxOrTip
    var taxText by remember(draft.taxOrTip) {
        mutableStateOf(if (draft.taxOrTip > 0.0) draft.taxOrTip.toString() else "0")
    }

    FintechCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_review_table_card"),
        borderColor = EmeraldBorder,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Editable Review Table",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldDark,
                )
                Text(
                    text = "Assign each line item to members & adjust prices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClearDraft) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.title,
            onValueChange = { onUpdateDraftMeta(it, null, null) },
            label = { Text("Merchant / Expense Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text("Category", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ExpenseCategory.entries) { cat ->
                FilterChip(
                    selected = draft.category == cat,
                    onClick = { onUpdateDraftMeta(null, cat, null) },
                    label = { Text(cat.label) },
                )
            }
        }

        if (members.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Paid By", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                members.forEach { m ->
                    FilterChip(
                        selected = selectedPayerId == m.id,
                        onClick = { onSelectPayer(m.id) },
                        label = { Text(m.name) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Line Items (${draft.items.size})",
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = onAddDraftItem) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Row")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            draft.items.forEach { item ->
                EditableLineItemRow(
                    item = item,
                    memberNames = members.map { it.name }.ifEmpty { listOf("You") },
                    currency = currency,
                    canDelete = draft.items.size > 1,
                    onUpdate = { name, qty, total -> onUpdateDraftItem(item.id, name, qty, total) },
                    onToggleMember = { memberName -> onToggleItemMember(item.id, memberName) },
                    onRemove = { onRemoveDraftItem(item.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = taxText,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    taxText = input
                    onUpdateDraftMeta(null, null, input.toDoubleOrNull() ?: 0.0)
                }
            },
            label = { Text("Tax / Tip / Delivery Fee (${currency.symbol})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EmeraldSoftBg,
            border = BorderStroke(1.dp, EmeraldBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Grand Total",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldDark,
                    )
                    Text(
                        text = "Tax/tip split proportionally",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatCurrency(grandTotal, currency),
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = EmeraldDark,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onCommitToGroup,
            enabled = targetGroup != null && grandTotal > 0.0,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("add_to_group_button"),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (targetGroup != null) {
                    "Add to ${targetGroup.name}"
                } else {
                    "Create a Group First"
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableLineItemRow(
    item: ParsedLineItem,
    memberNames: List<String>,
    currency: CurrencyOption,
    canDelete: Boolean,
    onUpdate: (name: String, qty: Int, totalPrice: Double) -> Unit,
    onToggleMember: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var qtyText by remember(item.id, item.quantity) { mutableStateOf(item.quantity.toString()) }
    var totalText by remember(item.id, item.totalPrice) { mutableStateOf(item.totalPrice.toString()) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(it, item.quantity, item.totalPrice) },
                    label = { Text("Item") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d{1,3}$"))) {
                            qtyText = v
                            onUpdate(item.name, v.toIntOrNull() ?: 1, item.totalPrice)
                        }
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(64.dp),
                )
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            totalText = v
                            onUpdate(item.name, item.quantity, v.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    label = { Text("Price (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(0.95f),
                )
                if (canDelete) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove item",
                            tint = CoralDebt,
                        )
                    }
                }
            }

            Text(
                text = "Split between:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                memberNames.forEach { memberName ->
                    val selected = item.assignedMemberNames.any { it.equals(memberName, ignoreCase = true) }
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleMember(memberName) },
                        label = { Text(memberName, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSoftBg,
                            selectedLabelColor = EmeraldDark,
                        ),
                    )
                }
            }
        }
    }
}
