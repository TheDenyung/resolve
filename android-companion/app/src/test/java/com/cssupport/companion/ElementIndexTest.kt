package com.cssupport.companion

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [ElementIndex] -- element indexing, filtering, and deduplication.
 *
 * Uses Robolectric so android.graphics.Rect coordinate math works correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ElementIndexTest {

    // ── buildElementIndex ─────────────────────────────────────────────

    @Test
    fun `buildElementIndex should assign 1-based IDs`() {
        val elements = listOf(
            makeElement(text = "Alpha", isClickable = true, bounds = Rect(0, 600, 200, 700)),
            makeElement(text = "Beta", isClickable = true, bounds = Rect(0, 800, 200, 900)),
        )
        val index = ElementIndex.buildElementIndex(elements)
        assertTrue(index.containsKey(1))
        assertTrue(index.containsKey(2))
        assertFalse(index.containsKey(0))
        assertFalse(index.containsKey(3))
    }

    @Test
    fun `buildElementIndex should sort top bar elements left to right`() {
        // Max bottom = 2400, so topBarThreshold = 300, bottomBarThreshold = 2100
        // Elements at y=10..80, centerY=45 -> in top bar
        val elements = listOf(
            makeElement(text = "Right", isClickable = true, bounds = Rect(900, 10, 1000, 80)),
            makeElement(text = "Left", isClickable = true, bounds = Rect(10, 10, 100, 80)),
            makeElement(text = "Anchor", bounds = Rect(0, 2300, 100, 2400)),
        )
        val index = ElementIndex.buildElementIndex(elements)
        assertEquals("Left", index[1]?.text)
        assertEquals("Right", index[2]?.text)
    }

    @Test
    fun `buildElementIndex should sort content elements top to bottom`() {
        val elements = listOf(
            makeElement(text = "Lower", isClickable = true, bounds = Rect(100, 800, 400, 900)),
            makeElement(text = "Upper", isClickable = true, bounds = Rect(100, 400, 400, 500)),
        )
        val index = ElementIndex.buildElementIndex(elements)
        assertEquals("Upper", index[1]?.text)
        assertEquals("Lower", index[2]?.text)
    }

    @Test
    fun `buildElementIndex should sort bottom bar elements left to right`() {
        // screenHeight max = 2400, bottomBarThreshold = 2100
        val elements = listOf(
            makeElement(text = "RightTab", isClickable = true, bounds = Rect(800, 2200, 1080, 2400)),
            makeElement(text = "LeftTab", isClickable = true, bounds = Rect(0, 2200, 300, 2400)),
        )
        val index = ElementIndex.buildElementIndex(elements)
        assertEquals("LeftTab", index[1]?.text)
        assertEquals("RightTab", index[2]?.text)
    }

    @Test
    fun `buildElementIndex should order top then content then bottom`() {
        val elements = listOf(
            makeElement(text = "Bottom", isClickable = true, bounds = Rect(0, 2200, 200, 2400)),
            makeElement(text = "Content", isClickable = true, bounds = Rect(100, 600, 300, 700)),
            makeElement(text = "Top", isClickable = true, bounds = Rect(10, 10, 100, 80)),
        )
        val index = ElementIndex.buildElementIndex(elements)
        assertEquals("Top", index[1]?.text)
        assertEquals("Content", index[2]?.text)
        assertEquals("Bottom", index[3]?.text)
    }

    @Test
    fun `buildElementIndex should return empty map for empty input`() {
        val index = ElementIndex.buildElementIndex(emptyList())
        assertTrue(index.isEmpty())
    }

    // ── filterMeaningful ──────────────────────────────────────────────

    @Test
    fun `filterMeaningful should include elements with text`() {
        val elements = listOf(
            makeElement(text = "Hello", bounds = Rect(100, 100, 300, 200)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertEquals(1, result.size)
    }

    @Test
    fun `filterMeaningful should include elements with contentDescription`() {
        val elements = listOf(
            makeElement(contentDescription = "Back button", bounds = Rect(100, 100, 200, 200)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertEquals(1, result.size)
    }

    @Test
    fun `filterMeaningful should include interactive elements even without text`() {
        val elements = listOf(
            makeElement(isClickable = true, bounds = Rect(100, 100, 200, 200)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertEquals(1, result.size)
    }

    @Test
    fun `filterMeaningful should exclude empty non-interactive elements`() {
        val elements = listOf(
            UIElement(
                id = null, className = "android.view.View",
                text = null, contentDescription = null,
                isClickable = false, isEditable = false, isScrollable = false,
                isCheckable = false, isChecked = null, isFocused = false,
                isEnabled = true, bounds = Rect(0, 0, 1080, 2400), childCount = 5,
            ),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterMeaningful should exclude tiny elements`() {
        val elements = listOf(
            makeElement(text = "Tiny", isClickable = true, bounds = Rect(100, 100, 101, 101)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterMeaningful should exclude offscreen elements`() {
        val elements = listOf(
            makeElement(text = "OffLeft", isClickable = true, bounds = Rect(-200, 500, 0, 600)),
            makeElement(text = "OffTop", isClickable = true, bounds = Rect(100, -200, 300, 0)),
            makeElement(text = "OffRight", isClickable = true, bounds = Rect(1100, 500, 1300, 600)),
            makeElement(text = "OffBottom", isClickable = true, bounds = Rect(100, 2500, 300, 2700)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterMeaningful should include editable fields`() {
        val elements = listOf(
            makeElement(isEditable = true, bounds = Rect(100, 100, 800, 200)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertEquals(1, result.size)
    }

    @Test
    fun `filterMeaningful should include scrollable containers`() {
        val elements = listOf(
            makeElement(isScrollable = true, bounds = Rect(0, 0, 1080, 2000)),
        )
        val result = ElementIndex.filterMeaningful(elements, 1080, 2400)
        assertEquals(1, result.size)
    }

    // ── deduplicateElements ───────────────────────────────────────────

    @Test
    fun `deduplicateElements should remove duplicates at similar positions`() {
        val elements = listOf(
            makeElement(text = "Button", isClickable = true, bounds = Rect(100, 500, 300, 600)),
            makeElement(text = "Button", isClickable = true, bounds = Rect(105, 505, 305, 605)),
        )
        val result = ElementIndex.deduplicateElements(elements)
        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicateElements should keep elements with different labels`() {
        val elements = listOf(
            makeElement(text = "Submit", isClickable = true, bounds = Rect(100, 500, 300, 600)),
            makeElement(text = "Cancel", isClickable = true, bounds = Rect(105, 505, 305, 605)),
        )
        val result = ElementIndex.deduplicateElements(elements)
        assertEquals(2, result.size)
    }

    @Test
    fun `deduplicateElements should keep elements at different positions`() {
        val elements = listOf(
            makeElement(text = "Button", isClickable = true, bounds = Rect(100, 500, 300, 600)),
            makeElement(text = "Button", isClickable = true, bounds = Rect(600, 500, 800, 600)),
        )
        val result = ElementIndex.deduplicateElements(elements)
        assertEquals(2, result.size)
    }

    @Test
    fun `deduplicateElements should differentiate by clickable state`() {
        val elements = listOf(
            makeElement(text = "Label", isClickable = true, bounds = Rect(100, 500, 300, 600)),
            makeElement(text = "Label", isClickable = false, bounds = Rect(105, 505, 305, 605)),
        )
        val result = ElementIndex.deduplicateElements(elements)
        assertEquals(2, result.size)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun makeElement(
        text: String? = null,
        contentDescription: String? = null,
        isClickable: Boolean = false,
        isEditable: Boolean = false,
        isScrollable: Boolean = false,
        isCheckable: Boolean = false,
        bounds: Rect = Rect(0, 0, 100, 100),
        className: String = "android.widget.TextView",
    ): UIElement = UIElement(
        id = null, className = className,
        text = text, contentDescription = contentDescription,
        isClickable = isClickable, isEditable = isEditable,
        isScrollable = isScrollable, isCheckable = isCheckable,
        isChecked = null, isFocused = false, isEnabled = true,
        bounds = bounds, childCount = 0,
    )
}
