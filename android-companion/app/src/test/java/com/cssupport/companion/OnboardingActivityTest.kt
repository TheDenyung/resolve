package com.cssupport.companion

import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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
 * Tests for [OnboardingActivity] -- the two-phase onboarding screen.
 *
 * Verifies:
 * - Activity creation and view binding
 * - Restricted settings card visibility based on SDK version
 * - Button presence and clickability
 * - "Allow Access" button launches accessibility settings intent
 * - "Open App Info" button launches app info intent
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class OnboardingActivityTest {

    // ── Activity creation ─────────────────────────────────────────────

    @Test
    fun `activity should create successfully`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
    }

    // ── View binding ──────────────────────────────────────────────────

    @Test
    fun `allow access button should exist`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()

        val btn = activity.findViewById<MaterialButton>(R.id.btnAllowAccess)
        assertNotNull(btn)
        assertTrue(btn.isEnabled)
    }

    @Test
    fun `open app info button should exist`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()

        val btn = activity.findViewById<MaterialButton>(R.id.btnOpenAppInfo)
        assertNotNull(btn)
    }

    // ── Restricted settings card (SDK-dependent) ──────────────────────

    @Test
    @Config(sdk = [28])
    fun `restricted settings card should be hidden on SDK 28`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()

        val card = activity.findViewById<MaterialCardView>(R.id.cardRestricted)
        assertEquals(View.GONE, card.visibility)
    }

    @Test
    @Config(sdk = [33])
    fun `restricted settings card should be visible on SDK 33`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()

        val card = activity.findViewById<MaterialCardView>(R.id.cardRestricted)
        assertEquals(View.VISIBLE, card.visibility)
    }

    // ── Button clicks ─────────────────────────────────────────────────

    @Test
    fun `allow access button should launch accessibility settings`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()

        activity.findViewById<MaterialButton>(R.id.btnAllowAccess).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(
            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS,
            nextIntent.action,
        )
    }

    @Test
    @Config(sdk = [33])
    fun `open app info button should launch app details settings`() {
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .get()

        activity.findViewById<MaterialButton>(R.id.btnOpenAppInfo).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            nextIntent.action,
        )
    }

    // ── onResume without accessibility service ────────────────────────

    @Test
    fun `onResume should not finish when accessibility service is not running`() {
        // SupportAccessibilityService.isRunning() returns false by default (instance is null).
        val activity = Robolectric.buildActivity(OnboardingActivity::class.java)
            .create()
            .resume()
            .get()

        assertFalse(activity.isFinishing)
    }
}
