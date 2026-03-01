package com.cssupport.companion

import android.graphics.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [ScreenParser] -- the LLM presentation layer extracted from AccessibilityEngine.
 *
 * Uses Robolectric so android.graphics.Rect coordinate math works correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ScreenParserTest {

    // ── isNavigationElement ─────────────────────────────────────────────

    @Test
    fun `isNavigationElement should recognize tab widget classes`() {
        val el = makeElement(text = "Explore", className = "android.widget.TabWidget", isClickable = true)
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should recognize help keyword`() {
        val el = makeElement(text = "Help", isClickable = true)
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should recognize support keyword`() {
        val el = makeElement(text = "Contact Support", isClickable = true)
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should recognize account keyword`() {
        val el = makeElement(text = "My Account", isClickable = true)
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should recognize exact match keywords like home`() {
        val el = makeElement(text = "home", isClickable = true)
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should not flag product items`() {
        val el = makeElement(text = "Margherita Pizza - $12.99", isClickable = true)
        assertFalse(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should recognize unlabeled image buttons`() {
        val el = makeElement(
            text = null,
            className = "android.widget.ImageButton",
            isClickable = true,
        )
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    @Test
    fun `isNavigationElement should recognize resource ID hints`() {
        val el = UIElement(
            id = "com.app:id/bottom_bar_account",
            className = "android.widget.TextView",
            text = "Tab 3",
            contentDescription = null,
            isClickable = true,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isChecked = null,
            isFocused = false,
            isEnabled = true,
            bounds = Rect(100, 600, 300, 700),
            childCount = 0,
        )
        assertTrue(ScreenParser.isNavigationElement(el))
    }

    // ── buttonTypeHint ──────────────────────────────────────────────────

    @Test
    fun `buttonTypeHint should classify ImageButton as icon-btn`() {
        val el = makeElement(text = "img", className = "android.widget.ImageButton", isClickable = true)
        assertTrue(ScreenParser.buttonTypeHint(el) == "icon-btn")
    }

    @Test
    fun `buttonTypeHint should classify CheckBox as checkbox`() {
        val el = makeElement(text = "Agree", className = "android.widget.CheckBox", isClickable = true, isCheckable = true)
        assertTrue(ScreenParser.buttonTypeHint(el) == "checkbox")
    }

    @Test
    fun `buttonTypeHint should classify tab as tab`() {
        val el = makeElement(text = "Home", className = "android.widget.TabWidget", isClickable = true)
        assertTrue(ScreenParser.buttonTypeHint(el) == "tab")
    }

    @Test
    fun `buttonTypeHint should classify back as nav-btn`() {
        val el = makeElement(text = "back", className = "android.widget.Button", isClickable = true)
        assertTrue(ScreenParser.buttonTypeHint(el) == "nav-btn")
    }

    @Test
    fun `buttonTypeHint should default to btn for regular buttons`() {
        val el = makeElement(text = "Submit", className = "android.widget.Button", isClickable = true)
        assertTrue(ScreenParser.buttonTypeHint(el) == "btn")
    }

    // ── positionHint ────────────────────────────────────────────────────

    @Test
    fun `positionHint should return far-left for unlabeled element at left edge`() {
        val bounds = Rect(10, 10, 80, 80)
        val hint = ScreenParser.positionHint(bounds, 1080, isUnlabeled = true)
        assertTrue(hint.contains("far-left"))
    }

    @Test
    fun `positionHint should return far-right for unlabeled element at right edge`() {
        val bounds = Rect(950, 10, 1050, 80)
        val hint = ScreenParser.positionHint(bounds, 1080, isUnlabeled = true)
        assertTrue(hint.contains("far-right"))
    }

    @Test
    fun `positionHint should return left for labeled element in left third`() {
        val bounds = Rect(50, 500, 200, 600)
        val hint = ScreenParser.positionHint(bounds, 1080, isUnlabeled = false)
        assertTrue(hint.contains("left"))
    }

    @Test
    fun `positionHint should return empty for labeled element in center`() {
        val bounds = Rect(400, 500, 600, 600)
        val hint = ScreenParser.positionHint(bounds, 1080, isUnlabeled = false)
        assertTrue(hint.isEmpty())
    }

    // ── formatSingleElement ─────────────────────────────────────────────

    @Test
    fun `formatSingleElement should include element ID in brackets`() {
        val el = makeElement(text = "Help", isClickable = true, bounds = Rect(100, 600, 300, 700))
        val line = ScreenParser.formatSingleElement(1, el, 1080, null)
        assertTrue(line.contains("[1]"))
    }

    @Test
    fun `formatSingleElement should include type hint`() {
        val el = makeElement(text = "Search", isEditable = true, bounds = Rect(100, 600, 800, 700))
        val line = ScreenParser.formatSingleElement(1, el, 1080, null)
        assertTrue(line.contains("INPUT"))
    }

    @Test
    fun `formatSingleElement should truncate long labels`() {
        val longText = "A".repeat(200)
        val el = makeElement(text = longText, isClickable = true, bounds = Rect(100, 600, 800, 700))
        val line = ScreenParser.formatSingleElement(1, el, 1080, null)
        assertTrue(line.contains("..."))
        assertFalse(line.contains("A".repeat(200)))
    }

    @Test
    fun `formatSingleElement should mark CHECKED checkboxes`() {
        val el = UIElement(
            id = null, className = "android.widget.CheckBox",
            text = "Terms", contentDescription = null,
            isClickable = true, isEditable = false, isScrollable = false,
            isCheckable = true, isChecked = true, isFocused = false,
            isEnabled = true, bounds = Rect(100, 600, 400, 700), childCount = 0,
        )
        val line = ScreenParser.formatSingleElement(1, el, 1080, null)
        assertTrue(line.contains("[CHECKED]"))
    }

    @Test
    fun `formatSingleElement should mark DISABLED buttons`() {
        val el = UIElement(
            id = null, className = "android.widget.Button",
            text = "Submit", contentDescription = null,
            isClickable = true, isEditable = false, isScrollable = false,
            isCheckable = false, isChecked = null, isFocused = false,
            isEnabled = false, bounds = Rect(100, 600, 300, 700), childCount = 0,
        )
        val line = ScreenParser.formatSingleElement(1, el, 1080, null)
        assertTrue(line.contains("[DISABLED]"))
    }

    @Test
    fun `formatSingleElement should mark NEW elements`() {
        val el = makeElement(text = "Chat with us", isClickable = true, bounds = Rect(100, 600, 300, 700))
        val prevLabels = setOf("Home", "Orders")
        val line = ScreenParser.formatSingleElement(1, el, 1080, prevLabels)
        assertTrue(line.contains("NEW"))
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
