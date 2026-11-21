package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.model.ExpenseCategory
import com.example.model.ParsedExpenseDraft
import com.example.model.ParsedLineItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "ReceiptChatParser"

class ReceiptChatParserService {

    private val parsedExpenseSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Merchant name or concise summary title of the expense"),
            "category" to Schema.enumeration(
                values = listOf("Food", "Rent", "Travel", "Utilities", "Shopping", "Entertainment"),
                description = "Best matching expense category"
            ),
            "payerName" to Schema.string(description = "Name of the person who paid, or 'You' if unclear"),
            "taxOrTip" to Schema.double(description = "Combined tax, service charge, delivery fee, or tip amount as a number (0 if none)"),
            "notes" to Schema.string(description = "Brief helpful note summarizing context from the receipt or chat"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Line item or sub-expense name"),
                        "quantity" to Schema.integer(description = "Item quantity, default 1"),
                        "unitPrice" to Schema.double(description = "Price per unit as a number"),
                        "totalPrice" to Schema.double(description = "Total price for this line item (quantity * unitPrice)"),
                        "participants" to Schema.array(
                            items = Schema.string(),
                            description = "Names of people sharing this item (e.g. ['You', 'Aarav']). Empty if split among everyone."
                        )
                    ),
                    optionalProperties = listOf("participants")
                ),
                description = "Extracted line items with prices and participants"
            )
        ),
        optionalProperties = listOf("notes", "taxOrTip")
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = parsedExpenseSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun parseReceiptImage(
        context: Context,
        imageUri: Uri,
        availableMemberNames: List<String>,
    ): ParsedExpenseDraft {
        val bitmap = withContext(Dispatchers.IO) {
            decodeScaledBitmap(context, imageUri, maxDimension = 1600)
        } ?: throw IllegalArgumentException("Could not read the selected image file.")

        val membersHint = if (availableMemberNames.isNotEmpty()) {
            "Known group members: ${availableMemberNames.joinToString(", ")}."
        } else {
            "Known members: You."
        }

        val prompt = """
            Analyze this receipt image and extract every line item accurately.
            Return:
            - title: merchant/store/restaurant name
            - category: one of Food, Rent, Travel, Utilities, Shopping, Entertainment
            - payerName: 'You' unless a specific payer is clearly printed
            - taxOrTip: sum of taxes, GST, VAT, service charge, delivery fee, or tip (0.0 if none)
            - items: every purchased line item with name, quantity (integer >= 1), unitPrice, and totalPrice.
              For participants, leave empty or assign known group members if handwritten notes indicate who ordered what.
            $membersHint
            Ensure numeric values are plain numbers without currency symbols.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
        }

        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
            throw IllegalStateException("Gemini returned an empty response for the receipt.")
        }
        return parseJsonToDraft(jsonText, availableMemberNames)
    }

    suspend fun parseChatText(
        chatText: String,
        availableMemberNames: List<String>,
    ): ParsedExpenseDraft {
        val membersHint = if (availableMemberNames.isNotEmpty()) {
            "Known group members (match names to these when possible): ${availableMemberNames.joinToString(", ")}."
        } else {
            "Known members: You."
        }

        val prompt = """
            Parse this messy chat log about shared expenses into structured line items.
            Identify who paid (if multiple payers appear, pick the primary payer or mention details in notes),
            every individual expense item, quantity, unitPrice, totalPrice, any tax/tip/fee, and which participants share each item.
            If a message says "split bw all" or "everyone", include all known members in participants.
            If a message mentions "I" or "me", map that to "You" when appropriate.
            $membersHint

            Chat Log:
            $chatText
        """.trimIndent()

        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }

        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
            throw IllegalStateException("Gemini returned an empty response for the chat log.")
        }
        return parseJsonToDraft(jsonText, availableMemberNames)
    }

    private fun parseJsonToDraft(
        jsonString: String,
        availableMemberNames: List<String>,
    ): ParsedExpenseDraft {
        try {
            val root = JSONObject(jsonString)
            val title = root.optString("title", "Parsed Expense").ifBlank { "Parsed Expense" }
            val category = ExpenseCategory.fromName(root.optString("category", "Food"))
            val rawPayer = root.optString("payerName", "You").ifBlank { "You" }
            val matchedPayer = matchMemberName(rawPayer, availableMemberNames) ?: rawPayer
            val taxOrTip = roundTwoDecimals(root.optDouble("taxOrTip", 0.0).coerceAtLeast(0.0))
            val notes = root.optString("notes", "")

            val itemsArray = root.optJSONArray("items")
            val parsedItems = mutableListOf<ParsedLineItem>()
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val name = obj.optString("name", "Item ${i + 1}").ifBlank { "Item ${i + 1}" }
                    val qty = obj.optInt("quantity", 1).coerceAtLeast(1)
                    val total = roundTwoDecimals(obj.optDouble("totalPrice", 0.0).coerceAtLeast(0.0))
                    val unit = roundTwoDecimals(
                        obj.optDouble("unitPrice", if (qty > 0) total / qty else total).coerceAtLeast(0.0)
                    )
                    val finalTotal = if (total > 0.0) total else roundTwoDecimals(unit * qty)

                    val partArray = obj.optJSONArray("participants")
                    val rawParticipants = mutableSetOf<String>()
                    if (partArray != null) {
                        for (j in 0 until partArray.length()) {
                            val rawName = partArray.optString(j).trim()
                            if (rawName.isNotBlank()) {
                                rawParticipants.add(matchMemberName(rawName, availableMemberNames) ?: rawName)
                            }
                        }
                    }
                    // Default to all available members if none specified
                    val assigned = if (rawParticipants.isNotEmpty()) {
                        rawParticipants
                    } else {
                        availableMemberNames.toSet()
                    }

                    parsedItems.add(
                        ParsedLineItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            quantity = qty,
                            unitPrice = if (unit > 0.0) unit else roundTwoDecimals(finalTotal / qty),
                            totalPrice = finalTotal,
                            assignedMemberNames = assigned,
                        )
                    )
                }
            }

            if (parsedItems.isEmpty()) {
                throw IllegalStateException("No expense items could be extracted from the input.")
            }

            return ParsedExpenseDraft(
                title = title,
                category = category,
                payerName = matchedPayer,
                taxOrTip = taxOrTip,
                items = parsedItems,
                notes = notes,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini JSON: $jsonString", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to parse AI output.")
        }
    }

    private fun matchMemberName(raw: String, available: List<String>): String? {
        val clean = raw.trim()
        return available.firstOrNull { it.equals(clean, ignoreCase = true) }
            ?: available.firstOrNull {
                clean.equals("me", ignoreCase = true) || clean.equals("i", ignoreCase = true)
            }?.let { available.firstOrNull { m -> m.equals("You", ignoreCase = true) } }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (max(origW, origH) / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    private fun roundTwoDecimals(value: Double): Double =
        (value * 100.0).roundToInt() / 100.0
}
