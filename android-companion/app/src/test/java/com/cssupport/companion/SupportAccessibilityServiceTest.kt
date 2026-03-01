package com.cssupport.companion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [SupportAccessibilityService] companion object static methods.
 *
 * The service itself requires the Android accessibility framework, but its
 * companion object methods (isRunning, getEngine) are testable:
 * - When no service instance is set, isRunning returns false
 * - When no service instance is set, getEngine returns null
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SupportAccessibilityServiceTest {

    @Test
    fun `isRunning should return false when no instance is set`() {
        // The companion object's instance has a private setter and defaults to null.
        assertFalse(SupportAccessibilityService.isRunning())
    }

    @Test
    fun `getEngine should return null when no instance is set`() {
        assertNull(SupportAccessibilityService.getEngine())
    }
}
