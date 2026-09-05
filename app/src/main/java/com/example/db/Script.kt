package com.example.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scripts",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class Script(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val folderId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    // Utility functions for smart script features (word count & read time)
    val wordCount: Int
        get() {
            if (content.isBlank()) return 0
            // filter out asterisks for emphasis when counting words
            val cleaned = content.replace("**", "")
            return cleaned.trim().split("\\s+".toRegex()).size
        }

    fun getEstimatedReadTimeSeconds(wordsPerMinute: Int = 130): Int {
        val words = wordCount
        if (words == 0) return 0
        return ((words.toFloat() / wordsPerMinute) * 60f).toInt()
    }
}
