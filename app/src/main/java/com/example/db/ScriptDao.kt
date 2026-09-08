package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY timestamp DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Query("SELECT * FROM folders ORDER BY timestamp DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM scripts WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getScriptsByFolder(folderId: Long): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE folderId IS NULL ORDER BY timestamp DESC")
    fun getUncategorizedScripts(): Flow<List<Script>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: Script): Long

    @Update
    suspend fun updateScript(script: Script)

    @Delete
    suspend fun deleteScript(script: Script)

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: Long): Script?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)
}
