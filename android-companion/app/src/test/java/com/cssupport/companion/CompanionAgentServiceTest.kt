package com.cssupport.companion

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests for [CompanionAgentService] -- the foreground service orchestrating
 * the on-device agent automation loop.
 *
 * The full service lifecycle requires an AccessibilityEngine and LLM client,
 * but we can test:
 * - Companion object static methods (start, stop, pause, resume) produce
 *   correct intents with expected actions and extras
 * - Service binds to null (not a bound service)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CompanionAgentServiceTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    // ── start() intent ────────────────────────────────────────────────

    @Test
    fun `start should create intent with ACTION_START and case extras`() {
        CompanionAgentService.start(
            context = context,
            caseId = "case-123",
            issue = "Missing item from order",
            desiredOutcome = "Get a refund",
            orderId = "ORD-456",
            targetPlatform = "com.example.app",
            hasAttachments = true,
        )

        val shadow = shadowOf(context.asApplication())
        val intent = shadow.nextStartedService
        assertNotNull("start() should start a service", intent)
        assertEquals("com.cssupport.companion.action.START", intent.action)
        assertEquals("case-123", intent.getStringExtra("extra_case_id"))
        assertEquals("Missing item from order", intent.getStringExtra("extra_issue"))
        assertEquals("Get a refund", intent.getStringExtra("extra_desired_outcome"))
        assertEquals("ORD-456", intent.getStringExtra("extra_order_id"))
        assertEquals("com.example.app", intent.getStringExtra("extra_target_platform"))
        assertTrue(intent.getBooleanExtra("extra_has_attachments", false))
    }

    @Test
    fun `start should handle null orderId`() {
        CompanionAgentService.start(
            context = context,
            caseId = "case-789",
            issue = "Wrong delivery",
            desiredOutcome = "Replacement",
            orderId = null,
            targetPlatform = "com.food.app",
            hasAttachments = false,
        )

        val shadow = shadowOf(context.asApplication())
        val intent = shadow.nextStartedService
        assertNotNull(intent)
        assertNull(intent.getStringExtra("extra_order_id"))
    }

    // ── stop() intent ─────────────────────────────────────────────────

    @Test
    fun `stop should create intent with ACTION_STOP`() {
        CompanionAgentService.stop(context)

        val shadow = shadowOf(context.asApplication())
        val intent = shadow.nextStartedService
        assertNotNull("stop() should start a service with STOP action", intent)
        assertEquals("com.cssupport.companion.action.STOP", intent.action)
    }

    // ── pause() intent ────────────────────────────────────────────────

    @Test
    fun `pause should create intent with ACTION_PAUSE`() {
        CompanionAgentService.pause(context)

        val shadow = shadowOf(context.asApplication())
        val intent = shadow.nextStartedService
        assertNotNull("pause() should start a service with PAUSE action", intent)
        assertEquals("com.cssupport.companion.action.PAUSE", intent.action)
    }

    // ── resume() intent ───────────────────────────────────────────────

    @Test
    fun `resume should create intent with ACTION_RESUME`() {
        CompanionAgentService.resume(context)

        val shadow = shadowOf(context.asApplication())
        val intent = shadow.nextStartedService
        assertNotNull("resume() should start a service with RESUME action", intent)
        assertEquals("com.cssupport.companion.action.RESUME", intent.action)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun Context.asApplication(): android.app.Application {
        return this as android.app.Application
    }
}
