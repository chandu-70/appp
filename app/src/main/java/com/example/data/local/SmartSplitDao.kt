package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartSplitDao {
    @Query("SELECT * FROM expense_groups ORDER BY createdAt ASC")
    fun observeGroups(): Flow<List<ExpenseGroupEntity>>

    @Query("SELECT * FROM group_members ORDER BY id ASC")
    fun observeAllMembers(): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM shared_expenses ORDER BY dateMillis DESC, id DESC")
    fun observeAllSharedExpenses(): Flow<List<SharedExpenseEntity>>

    @Query("SELECT * FROM expense_splits")
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM personal_expenses ORDER BY dateMillis DESC, id DESC")
    fun observePersonalExpenses(): Flow<List<PersonalExpenseEntity>>

    @Query("SELECT COUNT(*) FROM expense_groups")
    suspend fun getGroupCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ExpenseGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedExpense(expense: SharedExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Transaction
    suspend fun insertSharedExpenseWithSplits(
        expense: SharedExpenseEntity,
        splits: List<ExpenseSplitEntity>,
    ): Long {
        val expenseId = insertSharedExpense(expense)
        insertSplits(splits.map { it.copy(expenseId = expenseId) })
        return expenseId
    }

    @Query("DELETE FROM shared_expenses WHERE id = :expenseId")
    suspend fun deleteSharedExpense(expenseId: Long)

    @Query("DELETE FROM expense_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalExpense(expense: PersonalExpenseEntity): Long

    @Query("DELETE FROM personal_expenses WHERE id = :expenseId")
    suspend fun deletePersonalExpense(expenseId: Long)

    @Query("DELETE FROM expense_splits")
    suspend fun clearSplits()

    @Query("DELETE FROM shared_expenses")
    suspend fun clearSharedExpenses()

    @Query("DELETE FROM group_members")
    suspend fun clearMembers()

    @Query("DELETE FROM expense_groups")
    suspend fun clearGroups()

    @Query("DELETE FROM personal_expenses")
    suspend fun clearPersonalExpenses()

    @Transaction
    suspend fun clearAllData() {
        clearSplits()
        clearSharedExpenses()
        clearMembers()
        clearGroups()
        clearPersonalExpenses()
    }
}
