package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Script::class, Folder::class], version = 1, exportSchema = true)
abstract class ScriptDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: ScriptDatabase? = null

        fun getDatabase(context: Context): ScriptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScriptDatabase::class.java,
                    "script_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
