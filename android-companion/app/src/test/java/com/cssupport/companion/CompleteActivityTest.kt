package com.cssupport.companion

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests for [CompleteActivity] -- the case completion screen.
 *
 * Verifies:
 * - Activity creation and view binding
 * - Intent extras populate the correct views
 * - Default values when extras are missing
 * - Transcript expand/collapse toggle
 * - "New Issue" button navigation
 * - "Share" button intent
 * - Step count display
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CompleteActivityTest {

    @Before
    fun setUp() {
        // Clear any stale log entries from previous tests.
        AgentLogStore.clear()
    }

    // ── Activity creation ─────────────────────────────────────────────

    @Test
    fun `activity should create successfully with no extras`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `activity should create successfully with all extras`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_SUMMARY, "Refund approved for $49.99")
            putExtra(CompleteActivity.EXTRA_TARGET_APP, "Amazon")
            putExtra(CompleteActivity.EXTRA_OUTCOME, "Refund approved")
            putExtra(CompleteActivity.EXTRA_START_TIME, System.currentTimeMillis() - 120_000)
            putExtra(CompleteActivity.EXTRA_TRANSCRIPT, "Agent: Hello\nUser: Hi")
            putExtra(CompleteActivity.EXTRA_STEP_COUNT, 12)
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()
        assertNotNull(activity)
    }

    // ── View binding with extras ──────────────────────────────────────

    @Test
    fun `resolution summary should display intent extra`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_SUMMARY, "Refund of $49.99 approved")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val summaryView = activity.findViewById<TextView>(R.id.resolutionSummary)
        assertEquals("Refund of $49.99 approved", summaryView.text.toString())
    }

    @Test
    fun `detail views should display intent extras`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TARGET_APP, "Swiggy")
            putExtra(CompleteActivity.EXTRA_OUTCOME, "Resolved")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        assertEquals("Swiggy", activity.findViewById<TextView>(R.id.detailApp).text.toString())
        assertEquals("Resolved", activity.findViewById<TextView>(R.id.detailOutcome).text.toString())
    }

    @Test
    fun `step count should display from EXTRA_STEP_COUNT`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_STEP_COUNT, 7)
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        assertEquals("7", activity.findViewById<TextView>(R.id.detailSteps).text.toString())
    }

    // ── Default values ────────────────────────────────────────────────

    @Test
    fun `resolution summary should show default when no extra provided`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()

        val summary = activity.findViewById<TextView>(R.id.resolutionSummary).text.toString()
        assertEquals("Issue resolved successfully.", summary)
    }

    @Test
    fun `target app should default to App`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()

        assertEquals("App", activity.findViewById<TextView>(R.id.detailApp).text.toString())
    }

    @Test
    fun `outcome should default to Resolved`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()

        assertEquals("Resolved", activity.findViewById<TextView>(R.id.detailOutcome).text.toString())
    }

    @Test
    fun `time taken should show N-A when start time is zero`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()

        assertEquals("N/A", activity.findViewById<TextView>(R.id.detailTimeTaken).text.toString())
    }

    // ── Transcript expand / collapse ──────────────────────────────────

    @Test
    fun `transcript should start collapsed`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TRANSCRIPT, "Some transcript text")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val transcriptText = activity.findViewById<TextView>(R.id.transcriptText)
        assertEquals(View.GONE, transcriptText.visibility)
    }

    @Test
    fun `clicking transcript header should expand transcript`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TRANSCRIPT, "Transcript content here")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val header = activity.findViewById<View>(R.id.transcriptHeader)
        val transcriptText = activity.findViewById<TextView>(R.id.transcriptText)

        // Click to expand.
        header.performClick()
        assertEquals(View.VISIBLE, transcriptText.visibility)
    }

    @Test
    fun `clicking transcript header twice should collapse transcript`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TRANSCRIPT, "Content")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val header = activity.findViewById<View>(R.id.transcriptHeader)
        val transcriptText = activity.findViewById<TextView>(R.id.transcriptText)

        header.performClick() // Expand
        header.performClick() // Collapse
        assertEquals(View.GONE, transcriptText.visibility)
    }

    @Test
    fun `expand icon should rotate when transcript is expanded`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TRANSCRIPT, "Content")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val header = activity.findViewById<View>(R.id.transcriptHeader)
        val expandIcon = activity.findViewById<ImageView>(R.id.transcriptExpandIcon)

        assertEquals(0f, expandIcon.rotation, 0.01f)
        header.performClick()
        assertEquals(180f, expandIcon.rotation, 0.01f)
    }

    // ── "New Issue" navigation ────────────────────────────────────────

    @Test
    fun `new issue button should navigate to MainActivity`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()

        activity.findViewById<MaterialButton>(R.id.btnNewIssue).performClick()

        val shadow = shadowOf(activity)
        val nextIntent = shadow.nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(MainActivity::class.java.name, nextIntent.component?.className)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `new issue intent should have CLEAR_TOP and NEW_TASK flags`() {
        val activity = Robolectric.buildActivity(CompleteActivity::class.java)
            .create()
            .get()

        activity.findViewById<MaterialButton>(R.id.btnNewIssue).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertTrue(nextIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(nextIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    // ── Share button ──────────────────────────────────────────────────

    @Test
    fun `share button should launch ACTION_SEND chooser`() {
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TARGET_APP, "Zomato")
            putExtra(CompleteActivity.EXTRA_OUTCOME, "Refund processed")
            putExtra(CompleteActivity.EXTRA_SUMMARY, "Got my refund")
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        activity.findViewById<MaterialButton>(R.id.btnShare).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(nextIntent)
        // The chooser wraps an ACTION_SEND intent.
        assertEquals(Intent.ACTION_CHOOSER, nextIntent.action)
    }

    // ── Transcript content ────────────────────────────────────────────

    @Test
    fun `transcript should display provided text`() {
        val transcriptContent = "[12:01] Agent: Hello\n[12:02] Support: How can I help?"
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_TRANSCRIPT, transcriptContent)
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val transcriptText = activity.findViewById<TextView>(R.id.transcriptText)
        assertEquals(transcriptContent, transcriptText.text.toString())
    }

    // ── Time formatting ───────────────────────────────────────────────

    @Test
    fun `time taken should format minutes and seconds`() {
        val twoMinutesAgo = System.currentTimeMillis() - 125_000 // ~2 min 5 sec
        val intent = Intent().apply {
            putExtra(CompleteActivity.EXTRA_START_TIME, twoMinutesAgo)
        }
        val activity = Robolectric.buildActivity(CompleteActivity::class.java, intent)
            .create()
            .get()

        val timeTaken = activity.findViewById<TextView>(R.id.detailTimeTaken).text.toString()
        assertTrue("Should contain 'min'", timeTaken.contains("min"))
        assertTrue("Should contain 'sec'", timeTaken.contains("sec"))
    }
}
