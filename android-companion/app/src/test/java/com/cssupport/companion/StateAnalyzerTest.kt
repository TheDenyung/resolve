package com.cssupport.companion

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [StateAnalyzer] -- pattern detection, fingerprinting, and
 * differential state tracking.
 *
 * Uses Robolectric so android.graphics.Rect coordinate math works correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class StateAnalyzerTest {

    // ── detectScreenPattern ───────────────────────────────────────────

    @Test
    fun `detectScreenPattern should recognize chat messaging interface`() {
        val elements = listOf(
            makeElement(text = "Type a message", isEditable = true, bounds = Rect(100, 2100, 800, 2200)),
            makeElement(text = "Send", isClickable = true, bounds = Rect(850, 2100, 1000, 2200)),
        )
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.contains("Chat/messaging"))
    }

    @Test
    fun `detectScreenPattern should recognize order list`() {
        val elements = listOf(
            makeElement(text = "My Orders", bounds = Rect(100, 200, 500, 300)),
            makeElement(text = "Order #12345 - Delivered", isClickable = true, bounds = Rect(100, 400, 900, 500)),
            makeElement(text = "Track your order", isClickable = true, bounds = Rect(100, 600, 500, 700)),
        )
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.contains("Order list"))
    }

    @Test
    fun `detectScreenPattern should recognize help support page`() {
        val elements = listOf(
            makeElement(text = "Help Center", bounds = Rect(100, 200, 500, 300)),
            makeElement(text = "Contact Us", isClickable = true, bounds = Rect(100, 400, 500, 500)),
            makeElement(text = "FAQ", isClickable = true, bounds = Rect(100, 600, 500, 700)),
        )
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.contains("Help/support"))
    }

    @Test
    fun `detectScreenPattern should recognize profile account page`() {
        val elements = listOf(
            makeElement(text = "My Account", bounds = Rect(100, 200, 500, 300)),
            makeElement(text = "Edit Profile", isClickable = true, bounds = Rect(100, 400, 500, 500)),
            makeElement(text = "Settings", isClickable = true, bounds = Rect(100, 600, 500, 700)),
        )
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.contains("Profile/account"))
    }

    @Test
    fun `detectScreenPattern should recognize home feed with many items`() {
        val elements = (1..20).map { i ->
            makeElement(text = "Item $i", isClickable = true, bounds = Rect(0, i * 100, 1080, i * 100 + 80))
        }
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.contains("Home/feed"))
    }

    @Test
    fun `detectScreenPattern should recognize bottom navigation`() {
        val elements = listOf(
            makeElement(text = "Tab1", isClickable = true, bounds = Rect(0, 2200, 360, 2400)),
            makeElement(text = "Tab2", isClickable = true, bounds = Rect(360, 2200, 720, 2400)),
            makeElement(text = "Tab3", isClickable = true, bounds = Rect(720, 2200, 1080, 2400)),
            makeElement(text = "Content", bounds = Rect(100, 600, 500, 700)),
        )
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.contains("bottom navigation"))
    }

    @Test
    fun `detectScreenPattern should return empty for unrecognized screens`() {
        val elements = listOf(
            makeElement(text = "Random text", bounds = Rect(100, 600, 500, 700)),
        )
        val pattern = StateAnalyzer.detectScreenPattern(elements)
        assertTrue(pattern.isEmpty())
    }

    // ── fingerprint ───────────────────────────────────────────────────

    @Test
    fun `fingerprint should be consistent for identical screens`() {
        val screen1 = makeScreen(
            elements = listOf(
                makeElement(text = "Home", bounds = Rect(100, 600, 300, 700)),
                makeElement(text = "Orders", bounds = Rect(400, 600, 600, 700)),
            ),
        )
        val screen2 = makeScreen(
            elements = listOf(
                makeElement(text = "Home", bounds = Rect(100, 600, 300, 700)),
                makeElement(text = "Orders", bounds = Rect(400, 600, 600, 700)),
            ),
        )
        assertEquals(StateAnalyzer.fingerprint(screen1), StateAnalyzer.fingerprint(screen2))
    }

    @Test
    fun `fingerprint should differ when package changes`() {
        val screen1 = makeScreen(packageName = "com.app.one", elements = listOf(makeElement(text = "A", bounds = Rect(100, 100, 300, 200))))
        val screen2 = makeScreen(packageName = "com.app.two", elements = listOf(makeElement(text = "A", bounds = Rect(100, 100, 300, 200))))
        assertNotEquals(StateAnalyzer.fingerprint(screen1), StateAnalyzer.fingerprint(screen2))
    }

    @Test
    fun `fingerprint should differ when element text changes`() {
        val screen1 = makeScreen(elements = listOf(makeElement(text = "Submit", bounds = Rect(100, 100, 300, 200))))
        val screen2 = makeScreen(elements = listOf(makeElement(text = "Cancel", bounds = Rect(100, 100, 300, 200))))
        assertNotEquals(StateAnalyzer.fingerprint(screen1), StateAnalyzer.fingerprint(screen2))
    }

    @Test
    fun `fingerprint should differ when positions change significantly`() {
        val screen1 = makeScreen(elements = listOf(makeElement(text = "Btn", bounds = Rect(100, 100, 300, 200))))
        val screen2 = makeScreen(elements = listOf(makeElement(text = "Btn", bounds = Rect(800, 800, 1000, 900))))
        assertNotEquals(StateAnalyzer.fingerprint(screen1), StateAnalyzer.fingerprint(screen2))
    }

    @Test
    fun `fingerprint should handle empty element list`() {
        val screen = makeScreen(elements = emptyList())
        val fp = StateAnalyzer.fingerprint(screen)
        assertTrue(fp.isNotBlank())
    }

    @Test
    fun `fingerprint should be deterministic`() {
        val screen = makeScreen(elements = listOf(makeElement(text = "Hello", bounds = Rect(100, 100, 300, 200))))
        assertEquals(StateAnalyzer.fingerprint(screen), StateAnalyzer.fingerprint(screen))
    }

    // ── toMaskedSummary ───────────────────────────────────────────────

    @Test
    fun `toMaskedSummary should include package and element counts`() {
        val screen = makeScreen(
            packageName = "com.example.app",
            activityName = "com.example.app.HomeActivity",
            elements = listOf(
                makeElement(text = "Btn", isClickable = true, bounds = Rect(100, 500, 300, 600)),
                makeElement(text = "Input", isEditable = true, bounds = Rect(100, 700, 800, 800)),
                makeElement(text = "Label", bounds = Rect(100, 900, 300, 1000)),
            ),
        )
        val summary = StateAnalyzer.toMaskedSummary(screen, "Navigating")
        assertTrue(summary.contains("com.example.app"))
        assertTrue(summary.contains("HomeActivity"))
        assertTrue(summary.contains("3 elements"))
        assertTrue(summary.contains("2 interactive"))
        assertTrue(summary.contains("Navigating"))
    }

    @Test
    fun `toMaskedSummary should handle null activity name`() {
        val screen = makeScreen(activityName = null, elements = emptyList())
        val summary = StateAnalyzer.toMaskedSummary(screen, "phase")
        assertTrue(summary.contains("unknown"))
    }

    // ── newElementLabels ──────────────────────────────────────────────

    @Test
    fun `newElementLabels should return labels present in current but not in other`() {
        val prev = makeScreen(elements = listOf(makeElement(text = "Home")))
        val curr = makeScreen(elements = listOf(makeElement(text = "Home"), makeElement(text = "Chat")))
        val newLabels = StateAnalyzer.newElementLabels(curr, prev)
        assertTrue(newLabels.contains("Chat"))
        assertTrue(newLabels.none { it == "Home" })
    }

    @Test
    fun `newElementLabels should return empty when other is null`() {
        val screen = makeScreen(elements = listOf(makeElement(text = "Hello")))
        assertTrue(StateAnalyzer.newElementLabels(screen, null).isEmpty())
    }

    @Test
    fun `newElementLabels should cap at 10`() {
        val prev = makeScreen(elements = emptyList())
        val curr = makeScreen(elements = (1..20).map { makeElement(text = "Item $it") })
        assertTrue(StateAnalyzer.newElementLabels(curr, prev).size <= 10)
    }

    @Test
    fun `newElementLabels should truncate to 40 chars`() {
        val prev = makeScreen(elements = emptyList())
        val curr = makeScreen(elements = listOf(makeElement(text = "A".repeat(80))))
        val labels = StateAnalyzer.newElementLabels(curr, prev)
        assertTrue(labels.isNotEmpty())
        assertTrue(labels[0].length <= 40)
    }

    // ── removedElementLabels ──────────────────────────────────────────

    @Test
    fun `removedElementLabels should return labels missing from current`() {
        val prev = makeScreen(elements = listOf(makeElement(text = "Home"), makeElement(text = "Loading")))
        val curr = makeScreen(elements = listOf(makeElement(text = "Home")))
        val removed = StateAnalyzer.removedElementLabels(curr, prev)
        assertTrue(removed.contains("Loading"))
        assertTrue(removed.none { it == "Home" })
    }

    @Test
    fun `removedElementLabels should return empty when other is null`() {
        val screen = makeScreen(elements = listOf(makeElement(text = "Hello")))
        assertTrue(StateAnalyzer.removedElementLabels(screen, null).isEmpty())
    }

    @Test
    fun `removedElementLabels should cap at 10`() {
        val prev = makeScreen(elements = (1..20).map { makeElement(text = "Old $it") })
        val curr = makeScreen(elements = emptyList())
        assertTrue(StateAnalyzer.removedElementLabels(curr, prev).size <= 10)
    }

    // ── elementLabels ─────────────────────────────────────────────────

    @Test
    fun `elementLabels should return all unique labels`() {
        val screen = makeScreen(elements = listOf(
            makeElement(text = "Home"),
            makeElement(text = "Orders"),
            makeElement(contentDescription = "Profile"),
            makeElement(text = null, contentDescription = null),
        ))
        val labels = StateAnalyzer.elementLabels(screen)
        assertEquals(3, labels.size)
        assertTrue(labels.contains("Home"))
        assertTrue(labels.contains("Orders"))
        assertTrue(labels.contains("Profile"))
    }

    @Test
    fun `elementLabels should deduplicate`() {
        val screen = makeScreen(elements = listOf(
            makeElement(text = "Button"),
            makeElement(text = "Button"),
        ))
        assertEquals(1, StateAnalyzer.elementLabels(screen).size)
    }

    @Test
    fun `elementLabels should truncate to 40 chars`() {
        val screen = makeScreen(elements = listOf(makeElement(text = "Z".repeat(100))))
        val labels = StateAnalyzer.elementLabels(screen)
        assertTrue(labels.first().length <= 40)
    }

    @Test
    fun `elementLabels should return empty for empty screen`() {
        val screen = makeScreen(elements = emptyList())
        assertTrue(StateAnalyzer.elementLabels(screen).isEmpty())
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun makeScreen(
        packageName: String = "com.example.app",
        activityName: String? = "com.example.app.TestActivity",
        elements: List<UIElement> = emptyList(),
    ): ScreenState = ScreenState(
        packageName = packageName,
        activityName = activityName,
        elements = elements,
        focusedElement = null,
        timestamp = System.currentTimeMillis(),
    )

    private fun makeElement(
        text: String? = null,
        contentDescription: String? = null,
        isClickable: Boolean = false,
        isEditable: Boolean = false,
        isScrollable: Boolean = false,
        bounds: Rect = Rect(0, 0, 100, 100),
    ): UIElement = UIElement(
        id = null, className = "android.widget.TextView",
        text = text, contentDescription = contentDescription,
        isClickable = isClickable, isEditable = isEditable,
        isScrollable = isScrollable, isCheckable = false,
        isChecked = null, isFocused = false, isEnabled = true,
        bounds = bounds, childCount = 0,
    )
}
