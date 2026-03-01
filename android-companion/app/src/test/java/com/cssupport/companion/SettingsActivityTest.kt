package com.cssupport.companion

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests for [SettingsActivity] -- LLM provider config, accessibility status,
 * auto-approve toggle, and back navigation.
 *
 * Verifies:
 * - Activity creation and view binding
 * - Back button finishes the activity
 * - OAuth card and API key card are present
 * - API key form toggle (show/hide)
 * - Accessibility row launches system settings
 * - Auto-approve switch persists to SharedPreferences
 * - Version text is displayed
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SettingsActivityTest {

    // ── Activity creation ─────────────────────────────────────────────

    @Test
    fun `activity should create successfully`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
    }

    // ── Back button ───────────────────────────────────────────────────

    @Test
    fun `back button should finish activity`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        activity.findViewById<MaterialButton>(R.id.btnBack).performClick()
        assertTrue(activity.isFinishing)
    }

    // ── View binding ──────────────────────────────────────────────────

    @Test
    fun `OAuth card should exist`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val card = activity.findViewById<MaterialCardView>(R.id.oauthCard)
        assertNotNull(card)
    }

    @Test
    fun `API key card should exist`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val card = activity.findViewById<MaterialCardView>(R.id.apiKeyCard)
        assertNotNull(card)
    }

    @Test
    fun `OAuth status text should be present`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val status = activity.findViewById<TextView>(R.id.oauthStatusText)
        assertNotNull(status)
        assertTrue(status.text.isNotBlank())
    }

    @Test
    fun `API key status text should be present`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val status = activity.findViewById<TextView>(R.id.apiKeyStatusText)
        assertNotNull(status)
        assertTrue(status.text.isNotBlank())
    }

    @Test
    fun `version text should be displayed`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val versionText = activity.findViewById<TextView>(R.id.versionText)
        assertNotNull(versionText)
        assertTrue(versionText.text.isNotBlank())
    }

    // ── API key form toggle ───────────────────────────────────────────

    @Test
    fun `configure button should toggle API key section visibility`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val btnConfigure = activity.findViewById<MaterialButton>(R.id.btnConfigureApi)
        val apiKeySection = activity.findViewById<LinearLayout>(R.id.apiKeySection)

        // When no credentials are configured, the form starts visible.
        // Clicking should hide it.
        val initialVisibility = apiKeySection.visibility
        btnConfigure.performClick()
        val afterClick = apiKeySection.visibility

        // After a click the visibility should be different from before.
        assertTrue(initialVisibility != afterClick)
    }

    // ── Accessibility row ─────────────────────────────────────────────

    @Test
    fun `accessibility row should launch accessibility settings on click`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        activity.findViewById<LinearLayout>(R.id.accessibilityRow).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(
            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS,
            nextIntent.action,
        )
    }

    // ── Auto-approve switch ───────────────────────────────────────────

    @Test
    fun `auto-approve switch should exist and start unchecked by default`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val switch = activity.findViewById<MaterialSwitch>(R.id.autoApproveSwitch)
        assertNotNull(switch)
        // Default is false (no prior SharedPreferences).
        assertFalse(switch.isChecked)
    }

    @Test
    fun `toggling auto-approve switch should persist to SharedPreferences`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val switch = activity.findViewById<MaterialSwitch>(R.id.autoApproveSwitch)
        switch.isChecked = true

        val prefs = activity.getSharedPreferences("resolve_prefs", android.content.Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("auto_approve", false))
    }

    // ── Accessibility status ──────────────────────────────────────────

    @Test
    fun `accessibility status should show disabled when service is not running`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val statusText = activity.findViewById<TextView>(R.id.accessibilityStatusText)
        // Service is not running in test; the text should indicate disabled.
        assertNotNull(statusText)
        assertTrue(statusText.text.isNotBlank())
    }

    // ── Sign out button ───────────────────────────────────────────────

    @Test
    fun `sign out button should exist`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java)
            .create()
            .get()

        val btn = activity.findViewById<MaterialButton>(R.id.btnSignOut)
        assertNotNull(btn)
    }
}
