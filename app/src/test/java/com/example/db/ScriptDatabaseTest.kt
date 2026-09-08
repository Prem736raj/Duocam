package com.example.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScriptDatabaseTest {

    private lateinit var database: ScriptDatabase
    private lateinit var dao: ScriptDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ScriptDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        dao = database.scriptDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUpdateAndDeleteScript_preservesExpectedState() = runTest {
        val id = dao.insertScript(
            Script(
                title = "Intro",
                content = "Welcome to DuoCam.",
            ),
        )

        var scripts = dao.getAllScripts().first()
        assertEquals(1, scripts.size)
        assertEquals("Intro", scripts.first().title)

        val updated = scripts.first().copy(
            title = "Updated intro",
            content = "Updated content.",
        )

        dao.updateScript(updated)

        scripts = dao.getAllScripts().first()
        assertEquals("Updated intro", scripts.first().title)

        dao.deleteScript(scripts.first())
        assertTrue(dao.getAllScripts().first().isEmpty())
        assertTrue(id > 0L)
    }
}
