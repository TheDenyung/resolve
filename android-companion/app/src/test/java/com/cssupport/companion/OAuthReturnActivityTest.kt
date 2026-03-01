package com.cssupport.companion

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests for [OAuthReturnActivity] -- the invisible trampoline that handles
 * the OAuth callback from the Chrome Custom Tab.
 *
 * Verifies:
 * - Activity finishes immediately on create
 * - When isTaskRoot, redirects to WelcomeActivity before finishing
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class OAuthReturnActivityTest {

    @Test
    fun `activity should finish immediately`() {
        val activity = Robolectric.buildActivity(OAuthReturnActivity::class.java)
            .create()
            .get()

        assertTrue("OAuthReturnActivity should finish itself", activity.isFinishing)
    }

    @Test
    fun `activity should redirect to WelcomeActivity when isTaskRoot`() {
        // When isTaskRoot is true (the app task was killed), the activity
        // should start WelcomeActivity before finishing. Robolectric's
        // default Activity.isTaskRoot() returns true for a fresh launch.
        val activity = Robolectric.buildActivity(OAuthReturnActivity::class.java)
            .create()
            .get()

        val shadow = shadowOf(activity)
        val nextIntent = shadow.nextStartedActivity

        if (activity.isTaskRoot) {
            assertNotNull("Should start WelcomeActivity when isTaskRoot", nextIntent)
            assertEquals(WelcomeActivity::class.java.name, nextIntent.component?.className)
            assertTrue(nextIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
            assertTrue(nextIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
        }

        assertTrue(activity.isFinishing)
    }
}
