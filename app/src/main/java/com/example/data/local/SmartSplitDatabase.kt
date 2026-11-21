package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExpenseGroupEntity::class,
        GroupMemberEntity::class,
        SharedExpenseEntity::class,
        ExpenseSplitEntity::class,
        PersonalExpenseEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SmartSplitDatabase : RoomDatabase() {
    abstract fun smartSplitDao(): SmartSplitDao

    companion object {
        @Volatile
        private var INSTANCE: SmartSplitDatabase? = null

        fun getInstance(context: Context): SmartSplitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartSplitDatabase::class.java,
                    "smartsplit_x.db",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
