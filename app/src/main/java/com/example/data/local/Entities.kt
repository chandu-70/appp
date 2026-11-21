package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "expense_groups")
data class ExpenseGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val emoji: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("groupId")]
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val groupId: Long,
    val name: String,
    val isCurrentUser: Boolean = false,
)

@Entity(
    tableName = "shared_expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("groupId")]
)
data class SharedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val groupId: Long,
    val title: String,
    val totalAmount: Double,
    val categoryName: String,
    val paidByMemberId: Long,
    val splitModeName: String,
    val dateMillis: Long,
    val notes: String = "",
    val isSettlement: Boolean = false,
)

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = SharedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("expenseId"), Index("memberId")]
)
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expenseId: Long,
    val memberId: Long,
    val amount: Double,
    val percentage: Double = 0.0,
)

@Entity(tableName = "personal_expenses")
data class PersonalExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double,
    val categoryName: String,
    val paymentModeName: String,
    val dateMillis: Long,
    val notes: String = "",
)
