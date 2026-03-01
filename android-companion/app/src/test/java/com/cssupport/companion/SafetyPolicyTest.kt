package com.cssupport.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafetyPolicyTest {

    private lateinit var policy: SafetyPolicy

    @Before
    fun setUp() {
        policy = SafetyPolicy()
    }

    // ── Iteration limit ─────────────────────────────────────────────────

    @Test
    fun `should block when iteration count reaches maximum`() {
        val action = AgentAction.Wait(reason = "waiting")
        val result = policy.validate(action, iterationCount = SafetyPolicy.MAX_ITERATIONS)
        assertTrue(result is PolicyResult.Blocked)
        assertTrue((result as PolicyResult.Blocked).reason.contains("Maximum iteration limit"))
    }

    @Test
    fun `should allow action one iteration below maximum`() {
        val action = AgentAction.Wait(reason = "waiting")
        val result = policy.validate(action, iterationCount = SafetyPolicy.MAX_ITERATIONS - 1)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should respect custom max iterations`() {
        val strictPolicy = SafetyPolicy(maxIterations = 5)
        val action = AgentAction.Wait(reason = "waiting")

        assertEquals(PolicyResult.Allowed, strictPolicy.validate(action, iterationCount = 4))
        assertTrue(strictPolicy.validate(action, iterationCount = 5) is PolicyResult.Blocked)
    }

    // ── Always-safe actions ─────────────────────────────────────────────

    @Test
    fun `should allow MarkResolved without restrictions`() {
        val result = policy.validate(AgentAction.MarkResolved(summary = "Issue resolved"), 0)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow RequestHumanReview without restrictions`() {
        val result = policy.validate(
            AgentAction.RequestHumanReview(reason = "Need help"),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow ScrollDown without restrictions`() {
        val result = policy.validate(AgentAction.ScrollDown(reason = "looking"), 0)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow ScrollUp without restrictions`() {
        val result = policy.validate(AgentAction.ScrollUp(reason = "looking"), 0)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow PressBack without restrictions`() {
        val result = policy.validate(AgentAction.PressBack(reason = "going back"), 0)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow Wait without restrictions`() {
        val result = policy.validate(AgentAction.Wait(reason = "loading"), 0)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow UploadFile without restrictions`() {
        val result = policy.validate(AgentAction.UploadFile(fileDescription = "screenshot"), 0)
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow UpdatePlan without restrictions`() {
        val result = policy.validate(
            AgentAction.UpdatePlan(explanation = "new plan", steps = emptyList()),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    // ── SSN detection ───────────────────────────────────────────────────

    @Test
    fun `should block message containing SSN with dashes`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "My SSN is 123-45-6789"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.Blocked)
        assertTrue((result as PolicyResult.Blocked).reason.contains("Social Security Number"))
    }

    @Test
    fun `should block message containing 9 consecutive digits resembling SSN`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "Number is 123456789"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.Blocked)
    }

    @Test
    fun `should allow message with normal short numbers`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "My order number is 12345"),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    // ── Credit card detection (Luhn) ────────────────────────────────────

    @Test
    fun `should block message containing valid Visa card number`() {
        // 4111 1111 1111 1111 is a known test Visa number that passes Luhn
        val result = policy.validate(
            AgentAction.TypeMessage(text = "Card: 4111 1111 1111 1111"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.Blocked)
        assertTrue((result as PolicyResult.Blocked).reason.contains("credit card"))
    }

    @Test
    fun `should block message containing card number with dashes`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "Use 4111-1111-1111-1111 to pay"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.Blocked)
    }

    @Test
    fun `should allow message with digit sequence that fails Luhn check`() {
        // 1234 5678 9012 3456 does NOT pass Luhn
        val result = policy.validate(
            AgentAction.TypeMessage(text = "Reference: 1234 5678 9012 3456"),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    // ── Password detection ──────────────────────────────────────────────

    @Test
    fun `should flag message containing password assignment`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "My password: hunter2"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
        assertTrue((result as PolicyResult.NeedsApproval).reason.contains("password"))
    }

    @Test
    fun `should flag message containing pwd assignment`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "pwd=mySecret123"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    @Test
    fun `should flag message containing passwd assignment`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "passwd: abc123xyz"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    @Test
    fun `should allow message that mentions password without a value`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "I forgot my password"),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    // ── Message length limit ────────────────────────────────────────────

    @Test
    fun `should block message exceeding maximum length`() {
        val longText = "a".repeat(SafetyPolicy.MAX_MESSAGE_LENGTH + 1)
        val result = policy.validate(
            AgentAction.TypeMessage(text = longText),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.Blocked)
        assertTrue((result as PolicyResult.Blocked).reason.contains("maximum length"))
    }

    @Test
    fun `should allow message at exactly maximum length`() {
        val exactText = "a".repeat(SafetyPolicy.MAX_MESSAGE_LENGTH)
        val result = policy.validate(
            AgentAction.TypeMessage(text = exactText),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    // ── Financial click actions ─────────────────────────────────────────

    @Test
    fun `should require approval for clicking Pay button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Pay Now"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
        assertTrue((result as PolicyResult.NeedsApproval).reason.contains("financial"))
    }

    @Test
    fun `should require approval for clicking Purchase button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Complete Purchase"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    @Test
    fun `should require approval for Add to Cart button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Add to Cart"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    @Test
    fun `should allow clicking non-financial button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Next"),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    // ── Destructive click actions ───────────────────────────────────────

    @Test
    fun `should require approval for Delete Account button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Delete Account"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
        assertTrue((result as PolicyResult.NeedsApproval).reason.contains("destructive"))
    }

    @Test
    fun `should require approval for Cancel Subscription button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Cancel Subscription"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    @Test
    fun `should require approval for Deactivate button`() {
        val result = policy.validate(
            AgentAction.ClickElement(label = "Deactivate Account"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    // ── ClickElement with no label ──────────────────────────────────────

    @Test
    fun `should allow click with no label and no expected outcome`() {
        val result = policy.validate(
            AgentAction.ClickElement(elementId = 5),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should check expectedOutcome when label is null`() {
        val result = policy.validate(
            AgentAction.ClickElement(elementId = 5, expectedOutcome = "Confirm Payment"),
            iterationCount = 0,
        )
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    // ── Auto-approve safe actions ───────────────────────────────────────

    @Test
    fun `should auto-approve non-financial click when autoApproveSafeActions is true`() {
        val autoPolicy = SafetyPolicy(autoApproveSafeActions = true)
        val result = autoPolicy.validate(
            AgentAction.ClickElement(label = "Delete Account"),
            iterationCount = 0,
        )
        // Destructive but non-financial should be auto-approved
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should NOT auto-approve financial click even when autoApproveSafeActions is true`() {
        val autoPolicy = SafetyPolicy(autoApproveSafeActions = true)
        val result = autoPolicy.validate(
            AgentAction.ClickElement(label = "Pay Now"),
            iterationCount = 0,
        )
        // Financial actions always need approval regardless of auto-approve setting
        assertTrue(result is PolicyResult.NeedsApproval)
    }

    // ── Clean messages ──────────────────────────────────────────────────

    @Test
    fun `should allow normal support message`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "I have an issue with my order #12345. It arrived damaged."),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }

    @Test
    fun `should allow message with phone number`() {
        val result = policy.validate(
            AgentAction.TypeMessage(text = "Please call me at 555-123-4567"),
            iterationCount = 0,
        )
        assertEquals(PolicyResult.Allowed, result)
    }
}
