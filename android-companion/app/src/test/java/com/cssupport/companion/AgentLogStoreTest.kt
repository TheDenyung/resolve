package com.cssupport.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentLogStoreTest {

    @Before
    fun setUp() {
        AgentLogStore.clear()
    }

    // ── Basic logging ───────────────────────────────────────────────────

    @Test
    fun `should start with empty entries`() {
        assertEquals(emptyList<LogEntry>(), AgentLogStore.entries.value)
    }

    @Test
    fun `should add a single log entry`() {
        AgentLogStore.log("Test message", LogCategory.DEBUG)

        val entries = AgentLogStore.entries.value
        assertEquals(1, entries.size)
        assertEquals("Test message", entries[0].displayMessage)
        assertEquals(LogCategory.DEBUG, entries[0].category)
    }

    @Test
    fun `should use display parameter when provided`() {
        AgentLogStore.log("Internal detail", LogCategory.AGENT_ACTION, display = "User-facing text")

        val entries = AgentLogStore.entries.value
        assertEquals("User-facing text", entries[0].displayMessage)
    }

    @Test
    fun `should use message as display when display is null`() {
        AgentLogStore.log("Fallback message", LogCategory.STATUS_UPDATE)

        val entries = AgentLogStore.entries.value
        assertEquals("Fallback message", entries[0].displayMessage)
    }

    @Test
    fun `should default to DEBUG category`() {
        AgentLogStore.log("No category specified")

        val entries = AgentLogStore.entries.value
        assertEquals(LogCategory.DEBUG, entries[0].category)
    }

    // ── Entry ordering and IDs ──────────────────────────────────────────

    @Test
    fun `should assign incrementing IDs to entries`() {
        AgentLogStore.log("First", LogCategory.DEBUG)
        AgentLogStore.log("Second", LogCategory.DEBUG)
        AgentLogStore.log("Third", LogCategory.DEBUG)

        val entries = AgentLogStore.entries.value
        assertEquals(3, entries.size)
        assertTrue(entries[0].id < entries[1].id)
        assertTrue(entries[1].id < entries[2].id)
    }

    @Test
    fun `should preserve chronological order`() {
        AgentLogStore.log("First", LogCategory.AGENT_ACTION)
        AgentLogStore.log("Second", LogCategory.AGENT_MESSAGE)
        AgentLogStore.log("Third", LogCategory.ERROR)

        val entries = AgentLogStore.entries.value
        assertEquals("First", entries[0].displayMessage)
        assertEquals("Second", entries[1].displayMessage)
        assertEquals("Third", entries[2].displayMessage)
    }

    // ── 100-entry limit ─────────────────────────────────────────────────

    @Test
    fun `should enforce 100-entry limit by dropping oldest entries`() {
        repeat(110) { i ->
            AgentLogStore.log("Entry $i", LogCategory.DEBUG)
        }

        val entries = AgentLogStore.entries.value
        assertEquals(100, entries.size)
        // The oldest entries (0-9) should have been dropped
        assertEquals("Entry 10", entries[0].displayMessage)
        assertEquals("Entry 109", entries[99].displayMessage)
    }

    @Test
    fun `should keep exactly 100 entries at the limit`() {
        repeat(100) { i ->
            AgentLogStore.log("Entry $i", LogCategory.DEBUG)
        }

        assertEquals(100, AgentLogStore.entries.value.size)
        assertEquals("Entry 0", AgentLogStore.entries.value[0].displayMessage)
    }

    // ── Clear ───────────────────────────────────────────────────────────

    @Test
    fun `clear should remove all entries`() {
        AgentLogStore.log("First", LogCategory.DEBUG)
        AgentLogStore.log("Second", LogCategory.DEBUG)

        AgentLogStore.clear()

        assertEquals(emptyList<LogEntry>(), AgentLogStore.entries.value)
    }

    @Test
    fun `clear should reset ID counter so new entries start from 1`() {
        AgentLogStore.log("Before clear", LogCategory.DEBUG)
        val idBeforeClear = AgentLogStore.entries.value[0].id

        AgentLogStore.clear()
        AgentLogStore.log("After clear", LogCategory.DEBUG)

        val idAfterClear = AgentLogStore.entries.value[0].id
        assertEquals(1L, idAfterClear)
        assertTrue(idAfterClear <= idBeforeClear)
    }

    // ── All categories ──────────────────────────────────────────────────

    @Test
    fun `should support all log categories`() {
        LogCategory.entries.forEach { category ->
            AgentLogStore.log("Message for $category", category)
        }

        val entries = AgentLogStore.entries.value
        assertEquals(LogCategory.entries.size, entries.size)

        val categories = entries.map { it.category }.toSet()
        assertEquals(LogCategory.entries.toSet(), categories)
    }

    // ── LogEntry properties ─────────────────────────────────────────────

    @Test
    fun `log entries should have timestamps`() {
        val before = System.currentTimeMillis()
        AgentLogStore.log("Timestamped", LogCategory.DEBUG)
        val after = System.currentTimeMillis()

        val entry = AgentLogStore.entries.value[0]
        assertTrue(entry.timestamp in before..after)
    }

    @Test
    fun `log entries should have formatted time strings`() {
        AgentLogStore.log("Formatted", LogCategory.DEBUG)

        val entry = AgentLogStore.entries.value[0]
        assertTrue(entry.formattedTime.isNotBlank())
        // Format is HH:mm, so should match pattern like "14:30"
        assertTrue(entry.formattedTime.matches(Regex("""\d{2}:\d{2}""")))
    }
}
