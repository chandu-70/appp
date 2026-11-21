package com.example.data

import com.example.data.local.ExpenseGroupEntity
import com.example.data.local.ExpenseSplitEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.PersonalExpenseEntity
import com.example.data.local.SharedExpenseEntity
import com.example.data.local.SmartSplitDao
import com.example.model.ExpenseCategory
import com.example.model.PaymentMode
import com.example.model.SplitMode
import java.util.Calendar

object SampleDataSeeder {

    suspend fun seed(dao: SmartSplitDao) {
        dao.clearAllData()

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        // Keep sample personal expenses inside the current month so "This Month" card is rich
        val cal = Calendar.getInstance()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        fun daysAgoInMonth(daysAgo: Int): Long {
            val safeDays = daysAgo.coerceAtMost((dayOfMonth - 1).coerceAtLeast(0))
            return now - safeDays * dayMs
        }

        // 1. Flatmates Group
        val flatmatesId = dao.insertGroup(
            ExpenseGroupEntity(
                name = "Flatmates",
                description = "Indiranagar 3BHK shared bills & groceries",
                emoji = "🏠",
                createdAt = now - 20 * dayMs,
            )
        )
        val fmMemberIds = dao.insertMembers(
            listOf(
                GroupMemberEntity(groupId = flatmatesId, name = "You", isCurrentUser = true),
                GroupMemberEntity(groupId = flatmatesId, name = "Aarav", isCurrentUser = false),
                GroupMemberEntity(groupId = flatmatesId, name = "Priya", isCurrentUser = false),
                GroupMemberEntity(groupId = flatmatesId, name = "Rohan", isCurrentUser = false),
            )
        )
        val (fmYou, fmAarav, fmPriya, fmRohan) = fmMemberIds

        // Rent paid by Aarav (32,000 split equally -> 8,000 each -> You owe 8,000)
        dao.insertSharedExpenseWithSplits(
            SharedExpenseEntity(
                groupId = flatmatesId,
                title = "Monthly Apartment Rent",
                totalAmount = 32000.0,
                categoryName = ExpenseCategory.RENT.name,
                paidByMemberId = fmAarav,
                splitModeName = SplitMode.EQUAL.name,
                dateMillis = daysAgoInMonth(12),
                notes = "November flat rent transfer",
            ),
            fmMemberIds.map { memberId ->
                ExpenseSplitEntity(expenseId = 0L, memberId = memberId, amount = 8000.0, percentage = 25.0)
            }
        )

        // Utilities paid by You (3,600 split equally -> 900 each)
        dao.insertSharedExpenseWithSplits(
            SharedExpenseEntity(
                groupId = flatmatesId,
                title = "Fiber Wi-Fi & Electricity",
                totalAmount = 3600.0,
                categoryName = ExpenseCategory.UTILITIES.name,
                paidByMemberId = fmYou,
                splitModeName = SplitMode.EQUAL.name,
                dateMillis = daysAgoInMonth(5),
                notes = "BESCOM + ACT Fibernet bill",
            ),
            fmMemberIds.map { memberId ->
                ExpenseSplitEntity(expenseId = 0L, memberId = memberId, amount = 900.0, percentage = 25.0)
            }
        )

        // Groceries paid by You (2,480 exact amounts)
        dao.insertSharedExpenseWithSplits(
            SharedExpenseEntity(
                groupId = flatmatesId,
                title = "Weekly Groceries & Coffee",
                totalAmount = 2480.0,
                categoryName = ExpenseCategory.FOOD.name,
                paidByMemberId = fmYou,
                splitModeName = SplitMode.EXACT.name,
                dateMillis = daysAgoInMonth(2),
                notes = "Milk, veggies, oats & cold brew",
            ),
            listOf(
                ExpenseSplitEntity(expenseId = 0L, memberId = fmYou, amount = 780.0),
                ExpenseSplitEntity(expenseId = 0L, memberId = fmAarav, amount = 620.0),
                ExpenseSplitEntity(expenseId = 0L, memberId = fmPriya, amount = 540.0),
                ExpenseSplitEntity(expenseId = 0L, memberId = fmRohan, amount = 540.0),
            )
        )

        // 2. Goa Weekend Group
        val goaId = dao.insertGroup(
            ExpenseGroupEntity(
                name = "Goa Weekend",
                description = "Anjuna villa, scooters & beach shacks",
                emoji = "🌴",
                createdAt = now - 10 * dayMs,
            )
        )
        val goaMemberIds = dao.insertMembers(
            listOf(
                GroupMemberEntity(groupId = goaId, name = "You", isCurrentUser = true),
                GroupMemberEntity(groupId = goaId, name = "Aarav", isCurrentUser = false),
                GroupMemberEntity(groupId = goaId, name = "Priya", isCurrentUser = false),
                GroupMemberEntity(groupId = goaId, name = "Kabir", isCurrentUser = false),
            )
        )
        val (goaYou, goaAarav, goaPriya, goaKabir) = goaMemberIds

        // Villa Stay paid by You (18,000 equal -> 4,500 each -> You are owed 13,500 before other bills)
        dao.insertSharedExpenseWithSplits(
            SharedExpenseEntity(
                groupId = goaId,
                title = "Anjuna Beach Villa (2 Nights)",
                totalAmount = 18000.0,
                categoryName = ExpenseCategory.TRAVEL.name,
                paidByMemberId = goaYou,
                splitModeName = SplitMode.EQUAL.name,
                dateMillis = daysAgoInMonth(8),
                notes = "Airbnb deposit & check-in balance",
            ),
            goaMemberIds.map { memberId ->
                ExpenseSplitEntity(expenseId = 0L, memberId = memberId, amount = 4500.0, percentage = 25.0)
            }
        )

        // Scooters paid by Priya (3,200 equal -> 800 each)
        dao.insertSharedExpenseWithSplits(
            SharedExpenseEntity(
                groupId = goaId,
                title = "Scooter Rentals & Fuel",
                totalAmount = 3200.0,
                categoryName = ExpenseCategory.TRAVEL.name,
                paidByMemberId = goaPriya,
                splitModeName = SplitMode.EQUAL.name,
                dateMillis = daysAgoInMonth(6),
                notes = "4 Activas for 2 days",
            ),
            goaMemberIds.map { memberId ->
                ExpenseSplitEntity(expenseId = 0L, memberId = memberId, amount = 800.0, percentage = 25.0)
            }
        )

        // Dinner paid by Kabir (6,400 percentage split: 30%, 25%, 25%, 20%)
        dao.insertSharedExpenseWithSplits(
            SharedExpenseEntity(
                groupId = goaId,
                title = "Thalassa Sunset Dinner",
                totalAmount = 6400.0,
                categoryName = ExpenseCategory.FOOD.name,
                paidByMemberId = goaKabir,
                splitModeName = SplitMode.PERCENT.name,
                dateMillis = daysAgoInMonth(5),
                notes = "Wood-fired pizza, mezze platter & mocktails",
            ),
            listOf(
                ExpenseSplitEntity(expenseId = 0L, memberId = goaYou, amount = 1920.0, percentage = 30.0),
                ExpenseSplitEntity(expenseId = 0L, memberId = goaAarav, amount = 1600.0, percentage = 25.0),
                ExpenseSplitEntity(expenseId = 0L, memberId = goaPriya, amount = 1600.0, percentage = 25.0),
                ExpenseSplitEntity(expenseId = 0L, memberId = goaKabir, amount = 1280.0, percentage = 20.0),
            )
        )

        // 3. Personal Daily Expenses (current month)
        val personalSamples = listOf(
            PersonalExpenseEntity(
                title = "Blue Tokai Iced Pour Over",
                amount = 340.0,
                categoryName = ExpenseCategory.FOOD.name,
                paymentModeName = PaymentMode.UPI.name,
                dateMillis = daysAgoInMonth(0),
                notes = "Morning coffee & croissant",
            ),
            PersonalExpenseEntity(
                title = "Metro & Auto Commute",
                amount = 220.0,
                categoryName = ExpenseCategory.TRAVEL.name,
                paymentModeName = PaymentMode.UPI.name,
                dateMillis = daysAgoInMonth(1),
                notes = "Indiranagar to MG Road",
            ),
            PersonalExpenseEntity(
                title = "Design Systems Handbook",
                amount = 1250.0,
                categoryName = ExpenseCategory.SHOPPING.name,
                paymentModeName = PaymentMode.CREDIT_CARD.name,
                dateMillis = daysAgoInMonth(3),
                notes = "Blossom Book House",
            ),
            PersonalExpenseEntity(
                title = "Indie Cinema IMAX Ticket",
                amount = 680.0,
                categoryName = ExpenseCategory.ENTERTAINMENT.name,
                paymentModeName = PaymentMode.DEBIT_CARD.name,
                dateMillis = daysAgoInMonth(4),
                notes = "Weekend evening show",
            ),
            PersonalExpenseEntity(
                title = "Mobile Postpaid & Cloud",
                amount = 599.0,
                categoryName = ExpenseCategory.UTILITIES.name,
                paymentModeName = PaymentMode.UPI.name,
                dateMillis = daysAgoInMonth(7),
                notes = "Monthly mobile plan",
            ),
            PersonalExpenseEntity(
                title = "Minimalist Desk Lamp",
                amount = 1850.0,
                categoryName = ExpenseCategory.SHOPPING.name,
                paymentModeName = PaymentMode.CREDIT_CARD.name,
                dateMillis = daysAgoInMonth(9),
                notes = "Warm LED study lamp",
            ),
            PersonalExpenseEntity(
                title = "Ramen Bowl & Matcha",
                amount = 640.0,
                categoryName = ExpenseCategory.FOOD.name,
                paymentModeName = PaymentMode.CASH.name,
                dateMillis = daysAgoInMonth(11),
                notes = "Solo lunch",
            ),
        )
        personalSamples.forEach { dao.insertPersonalExpense(it) }
    }
}
