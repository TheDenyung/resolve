package com.cssupport.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationKnowledgeTest {

    // ── Exact package name lookup ───────────────────────────────────────

    @Test
    fun `should find Swiggy by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("in.swiggy.android")
        assertNotNull(profile)
        assertEquals("Swiggy", profile!!.appName)
    }

    @Test
    fun `should find Dominos by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("com.dominos")
        assertNotNull(profile)
        assertEquals("Domino's", profile!!.appName)
    }

    @Test
    fun `should find Zomato by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("com.application.zomato")
        assertNotNull(profile)
        assertEquals("Zomato", profile!!.appName)
    }

    @Test
    fun `should find Amazon by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("in.amazon.mshop.android.shopping")
        assertNotNull(profile)
        assertEquals("Amazon", profile!!.appName)
    }

    @Test
    fun `should find Flipkart by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("com.flipkart.android")
        assertNotNull(profile)
        assertEquals("Flipkart", profile!!.appName)
    }

    @Test
    fun `should find Uber by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("com.ubercab")
        assertNotNull(profile)
        assertEquals("Uber", profile!!.appName)
    }

    @Test
    fun `should find Ola by exact package name`() {
        val profile = AppNavigationKnowledge.lookup("com.olacabs.customer")
        assertNotNull(profile)
        assertEquals("Ola", profile!!.appName)
    }

    // ── Case insensitivity and trimming ─────────────────────────────────

    @Test
    fun `should match package name case-insensitively`() {
        val profile = AppNavigationKnowledge.lookup("IN.SWIGGY.ANDROID")
        assertNotNull(profile)
        assertEquals("Swiggy", profile!!.appName)
    }

    @Test
    fun `should trim whitespace from package name`() {
        val profile = AppNavigationKnowledge.lookup("  com.dominos  ")
        assertNotNull(profile)
        assertEquals("Domino's", profile!!.appName)
    }

    // ── Partial package name matching ───────────────────────────────────

    @Test
    fun `should match when input contains the known package name`() {
        val profile = AppNavigationKnowledge.lookup("com.dominos.india")
        assertNotNull(profile)
        assertEquals("Domino's", profile!!.appName)
    }

    // ── Unknown apps ────────────────────────────────────────────────────

    @Test
    fun `should return null for unknown package name`() {
        val profile = AppNavigationKnowledge.lookup("com.unknown.app")
        assertNull(profile)
    }

    @Test
    fun `should return null for empty string`() {
        val profile = AppNavigationKnowledge.lookup("")
        assertNull(profile)
    }

    // ── Fuzzy name lookup ───────────────────────────────────────────────

    @Test
    fun `should find Swiggy by app name`() {
        val profile = AppNavigationKnowledge.lookupByName("swiggy")
        assertNotNull(profile)
        assertEquals("Swiggy", profile!!.appName)
    }

    @Test
    fun `should find Dominos by partial app name`() {
        val profile = AppNavigationKnowledge.lookupByName("domino")
        assertNotNull(profile)
        assertEquals("Domino's", profile!!.appName)
    }

    @Test
    fun `should match app name case-insensitively`() {
        val profile = AppNavigationKnowledge.lookupByName("ZOMATO")
        assertNotNull(profile)
        assertEquals("Zomato", profile!!.appName)
    }

    @Test
    fun `should find Amazon by name`() {
        val profile = AppNavigationKnowledge.lookupByName("amazon")
        assertNotNull(profile)
        assertEquals("Amazon", profile!!.appName)
    }

    @Test
    fun `should find Flipkart by name`() {
        val profile = AppNavigationKnowledge.lookupByName("flipkart")
        assertNotNull(profile)
        assertEquals("Flipkart", profile!!.appName)
    }

    @Test
    fun `should find Uber by name`() {
        val profile = AppNavigationKnowledge.lookupByName("uber")
        assertNotNull(profile)
        assertEquals("Uber", profile!!.appName)
    }

    @Test
    fun `should find Ola by name`() {
        val profile = AppNavigationKnowledge.lookupByName("ola")
        assertNotNull(profile)
        assertEquals("Ola", profile!!.appName)
    }

    @Test
    fun `should return null for unknown app name`() {
        val profile = AppNavigationKnowledge.lookupByName("netflix")
        assertNull(profile)
    }

    // ── Profile completeness ────────────────────────────────────────────

    @Test
    fun `all profiles should have non-empty support paths`() {
        val appNames = listOf("swiggy", "domino", "zomato", "amazon", "flipkart", "uber", "ola")
        for (name in appNames) {
            val profile = AppNavigationKnowledge.lookupByName(name)
            assertNotNull("Profile for $name should exist", profile)
            assertTrue(
                "Support path for $name should not be empty",
                profile!!.supportPath.isNotEmpty(),
            )
        }
    }

    @Test
    fun `all profiles should have non-empty pitfalls`() {
        val appNames = listOf("swiggy", "domino", "zomato", "amazon", "flipkart", "uber", "ola")
        for (name in appNames) {
            val profile = AppNavigationKnowledge.lookupByName(name)
            assertNotNull("Profile for $name should exist", profile)
            assertTrue(
                "Pitfalls for $name should not be empty",
                profile!!.pitfalls.isNotEmpty(),
            )
        }
    }

    @Test
    fun `all profiles should have profile location`() {
        val appNames = listOf("swiggy", "domino", "zomato", "amazon", "flipkart", "uber", "ola")
        for (name in appNames) {
            val profile = AppNavigationKnowledge.lookupByName(name)
            assertNotNull("Profile for $name should exist", profile)
            assertTrue(
                "Profile location for $name should not be blank",
                profile!!.profileLocation.isNotBlank(),
            )
        }
    }

    // ── toPromptBlock format ────────────────────────────────────────────

    @Test
    fun `toPromptBlock should contain app name as header`() {
        val profile = AppNavigationKnowledge.lookup("in.swiggy.android")!!
        val block = profile.toPromptBlock()
        assertTrue(block.contains("## Swiggy Navigation"))
    }

    @Test
    fun `toPromptBlock should contain numbered support steps`() {
        val profile = AppNavigationKnowledge.lookup("in.swiggy.android")!!
        val block = profile.toPromptBlock()
        assertTrue(block.contains("1."))
        assertTrue(block.contains("2."))
    }

    @Test
    fun `toPromptBlock should contain AVOID section with pitfalls`() {
        val profile = AppNavigationKnowledge.lookup("in.swiggy.android")!!
        val block = profile.toPromptBlock()
        assertTrue(block.contains("AVOID:"))
        assertTrue(block.contains("  - "))
    }

    @Test
    fun `toPromptBlock should include profile location`() {
        val profile = AppNavigationKnowledge.lookup("in.swiggy.android")!!
        val block = profile.toPromptBlock()
        assertTrue(block.contains("Profile/Account:"))
        assertTrue(block.contains("ACCOUNT"))
    }

    @Test
    fun `toPromptBlock should include order history location`() {
        val profile = AppNavigationKnowledge.lookup("in.swiggy.android")!!
        val block = profile.toPromptBlock()
        assertTrue(block.contains("Orders:"))
    }
}
