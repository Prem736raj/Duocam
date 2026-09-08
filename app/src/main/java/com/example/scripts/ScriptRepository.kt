package com.example.scripts

import com.example.db.Script
import com.example.db.ScriptDao
import kotlinx.coroutines.flow.Flow

internal class ScriptRepository(
    private val dao: ScriptDao,
) {
    val scripts: Flow<List<Script>>
        get() = dao.getAllScripts()

    suspend fun getById(id: Long): Script? = dao.getScriptById(id)

    suspend fun save(
        existing: Script?,
        title: String,
        content: String,
    ) {
        if (existing == null) {
            dao.insertScript(
                Script(title = title, content = content),
            )
        } else {
            dao.updateScript(
                existing.copy(
                    title = title,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun delete(script: Script) {
        dao.deleteScript(script)
    }
}
