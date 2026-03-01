package com.cssupport.companion

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONObject

/**
 * Tests for AgentLoop's pure helper methods:
 * - toolNameFromAction: AgentAction -> tool name string
 * - toolArgsFromAction: AgentAction -> JSON args string
 * - describeAction: AgentAction -> human-readable description
 * - classifyLLMError: Exception -> LLMErrorClass (retryable vs terminal)
 */
class AgentLoopPureMethodsTest {

    private lateinit var agentLoop: AgentLoop

    @Before
    fun setUp() {
        agentLoop = AgentLoop(
            engine = mockk(relaxed = true),
            llmClient = mockk(relaxed = true),
            caseContext = CaseContext(
                caseId = "test-001",
                customerName = "Test User",
                issue = "Test issue",
                desiredOutcome = "Resolution",
                orderId = "ORD-123",
                hasAttachments = false,
                targetPlatform = "com.example.app",
            ),
        )
    }

    // ── toolNameFromAction ───────────────────────────────────────────────

    @Test
    fun `toolNameFromAction should return type_message for TypeMessage`() {
        assertEquals("type_message", agentLoop.toolNameFromAction(AgentAction.TypeMessage("hi")))
    }

    @Test
    fun `toolNameFromAction should return click_element for ClickElement`() {
        assertEquals("click_element", agentLoop.toolNameFromAction(AgentAction.ClickElement(elementId = 1)))
    }

    @Test
    fun `toolNameFromAction should return scroll_down for ScrollDown`() {
        assertEquals("scroll_down", agentLoop.toolNameFromAction(AgentAction.ScrollDown("reason")))
    }

    @Test
    fun `toolNameFromAction should return scroll_up for ScrollUp`() {
        assertEquals("scroll_up", agentLoop.toolNameFromAction(AgentAction.ScrollUp("reason")))
    }

    @Test
    fun `toolNameFromAction should return wait_for_response for Wait`() {
        assertEquals("wait_for_response", agentLoop.toolNameFromAction(AgentAction.Wait("reason")))
    }

    @Test
    fun `toolNameFromAction should return upload_file for UploadFile`() {
        assertEquals("upload_file", agentLoop.toolNameFromAction(AgentAction.UploadFile("desc")))
    }

    @Test
    fun `toolNameFromAction should return press_back for PressBack`() {
        assertEquals("press_back", agentLoop.toolNameFromAction(AgentAction.PressBack("reason")))
    }

    @Test
    fun `toolNameFromAction should return request_human_review for RequestHumanReview`() {
        assertEquals("request_human_review", agentLoop.toolNameFromAction(AgentAction.RequestHumanReview("reason")))
    }

    @Test
    fun `toolNameFromAction should return mark_resolved for MarkResolved`() {
        assertEquals("mark_resolved", agentLoop.toolNameFromAction(AgentAction.MarkResolved("done")))
    }

    @Test
    fun `toolNameFromAction should return update_plan for UpdatePlan`() {
        assertEquals("update_plan", agentLoop.toolNameFromAction(AgentAction.UpdatePlan("plan", emptyList())))
    }

    // ── toolArgsFromAction ───────────────────────────────────────────────

    @Test
    fun `toolArgsFromAction TypeMessage should produce valid JSON with text`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.TypeMessage("Hello"))
        val json = JSONObject(args)
        assertEquals("Hello", json.getString("text"))
        assertFalse(json.has("elementId"))
    }

    @Test
    fun `toolArgsFromAction TypeMessage with elementId should include it`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.TypeMessage("Hello", elementId = 5))
        val json = JSONObject(args)
        assertEquals("Hello", json.getString("text"))
        assertEquals(5, json.getInt("elementId"))
    }

    @Test
    fun `toolArgsFromAction TypeMessage should escape quotes in text`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.TypeMessage("He said \"hi\""))
        val json = JSONObject(args)
        assertEquals("He said \"hi\"", json.getString("text"))
    }

    @Test
    fun `toolArgsFromAction ClickElement should include elementId and label`() {
        val args = agentLoop.toolArgsFromAction(
            AgentAction.ClickElement(elementId = 3, label = "Submit", expectedOutcome = "Form sent")
        )
        val json = JSONObject(args)
        assertEquals(3, json.getInt("elementId"))
        assertEquals("Submit", json.getString("label"))
        assertEquals("Form sent", json.getString("expectedOutcome"))
    }

    @Test
    fun `toolArgsFromAction ClickElement without elementId should omit it`() {
        val args = agentLoop.toolArgsFromAction(
            AgentAction.ClickElement(label = "OK", expectedOutcome = "")
        )
        val json = JSONObject(args)
        assertFalse(json.has("elementId"))
        assertEquals("OK", json.getString("label"))
    }

    @Test
    fun `toolArgsFromAction ScrollDown should produce valid JSON`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.ScrollDown("see more"))
        val json = JSONObject(args)
        assertEquals("see more", json.getString("reason"))
    }

    @Test
    fun `toolArgsFromAction Wait should produce valid JSON`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.Wait("loading"))
        val json = JSONObject(args)
        assertEquals("loading", json.getString("reason"))
    }

    @Test
    fun `toolArgsFromAction MarkResolved should produce valid JSON`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.MarkResolved("Refund done"))
        val json = JSONObject(args)
        assertEquals("Refund done", json.getString("summary"))
    }

    @Test
    fun `toolArgsFromAction UpdatePlan should produce valid JSON with steps`() {
        val action = AgentAction.UpdatePlan(
            explanation = "Planning",
            steps = listOf(
                PlanStep("Step 1", "completed"),
                PlanStep("Step 2", "pending"),
            ),
        )
        val args = agentLoop.toolArgsFromAction(action)
        val json = JSONObject(args)
        assertEquals("Planning", json.getString("explanation"))
        val steps = json.getJSONArray("steps")
        assertEquals(2, steps.length())
        assertEquals("Step 1", steps.getJSONObject(0).getString("step"))
        assertEquals("completed", steps.getJSONObject(0).getString("status"))
    }

    @Test
    fun `toolArgsFromAction UploadFile should produce valid JSON`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.UploadFile("screenshot"))
        val json = JSONObject(args)
        assertEquals("screenshot", json.getString("fileDescription"))
    }

    @Test
    fun `toolArgsFromAction PressBack should produce valid JSON`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.PressBack("wrong page"))
        val json = JSONObject(args)
        assertEquals("wrong page", json.getString("reason"))
    }

    @Test
    fun `toolArgsFromAction RequestHumanReview should produce valid JSON`() {
        val args = agentLoop.toolArgsFromAction(AgentAction.RequestHumanReview("need help"))
        val json = JSONObject(args)
        assertEquals("need help", json.getString("reason"))
    }

    // ── describeAction ───────────────────────────────────────────────────

    @Test
    fun `describeAction TypeMessage should include text preview`() {
        val desc = agentLoop.describeAction(AgentAction.TypeMessage("Hello world"))
        assertTrue(desc.contains("Type message"))
        assertTrue(desc.contains("Hello world"))
    }

    @Test
    fun `describeAction TypeMessage should truncate long text`() {
        val longText = "A".repeat(100)
        val desc = agentLoop.describeAction(AgentAction.TypeMessage(longText))
        assertTrue(desc.contains("..."))
        assertTrue(desc.length < 100)
    }

    @Test
    fun `describeAction ClickElement with elementId and label`() {
        val desc = agentLoop.describeAction(AgentAction.ClickElement(elementId = 7, label = "Submit"))
        assertTrue(desc.contains("Click"))
        assertTrue(desc.contains("[7]"))
        assertTrue(desc.contains("Submit"))
    }

    @Test
    fun `describeAction ClickElement with only elementId`() {
        val desc = agentLoop.describeAction(AgentAction.ClickElement(elementId = 3))
        assertTrue(desc.contains("[3]"))
    }

    @Test
    fun `describeAction ClickElement with only label`() {
        val desc = agentLoop.describeAction(AgentAction.ClickElement(label = "OK"))
        assertTrue(desc.contains("OK"))
    }

    @Test
    fun `describeAction ClickElement with nothing should say unknown`() {
        val desc = agentLoop.describeAction(AgentAction.ClickElement())
        assertTrue(desc.contains("unknown"))
    }

    @Test
    fun `describeAction ScrollDown should include reason`() {
        val desc = agentLoop.describeAction(AgentAction.ScrollDown("see more items"))
        assertTrue(desc.contains("Scroll down"))
        assertTrue(desc.contains("see more items"))
    }

    @Test
    fun `describeAction ScrollUp should include reason`() {
        val desc = agentLoop.describeAction(AgentAction.ScrollUp("back to top"))
        assertTrue(desc.contains("Scroll up"))
        assertTrue(desc.contains("back to top"))
    }

    @Test
    fun `describeAction Wait should include reason`() {
        val desc = agentLoop.describeAction(AgentAction.Wait("page loading"))
        assertTrue(desc.contains("Wait"))
        assertTrue(desc.contains("page loading"))
    }

    @Test
    fun `describeAction MarkResolved should include summary`() {
        val desc = agentLoop.describeAction(AgentAction.MarkResolved("Refund processed"))
        assertTrue(desc.contains("Mark resolved"))
        assertTrue(desc.contains("Refund processed"))
    }

    @Test
    fun `describeAction UpdatePlan should include step count`() {
        val action = AgentAction.UpdatePlan(
            "Planning route",
            listOf(PlanStep("a"), PlanStep("b"), PlanStep("c")),
        )
        val desc = agentLoop.describeAction(action)
        assertTrue(desc.contains("Plan"))
        assertTrue(desc.contains("3 steps"))
    }

    @Test
    fun `describeAction UploadFile should include description`() {
        val desc = agentLoop.describeAction(AgentAction.UploadFile("receipt photo"))
        assertTrue(desc.contains("Upload file"))
        assertTrue(desc.contains("receipt photo"))
    }

    @Test
    fun `describeAction PressBack should include reason`() {
        val desc = agentLoop.describeAction(AgentAction.PressBack("wrong screen"))
        assertTrue(desc.contains("Press back"))
        assertTrue(desc.contains("wrong screen"))
    }

    @Test
    fun `describeAction RequestHumanReview should include reason`() {
        val desc = agentLoop.describeAction(AgentAction.RequestHumanReview("Need OTP"))
        assertTrue(desc.contains("Request human review"))
        assertTrue(desc.contains("Need OTP"))
    }

    // ── classifyLLMError ─────────────────────────────────────────────────

    @Test
    fun `classifyLLMError 401 should be terminal`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 401 Unauthorized"))
        assertFalse(result.retryable)
        assertTrue(result.userMessage.contains("API key"))
    }

    @Test
    fun `classifyLLMError 403 should be terminal`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 403 Forbidden"))
        assertFalse(result.retryable)
        assertTrue(result.userMessage.contains("Access denied"))
    }

    @Test
    fun `classifyLLMError 404 should be terminal`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 404 Not Found"))
        assertFalse(result.retryable)
        assertTrue(result.userMessage.contains("Model not found"))
    }

    @Test
    fun `classifyLLMError quota should be terminal`() {
        val result = agentLoop.classifyLLMError(Exception("You have exceeded your quota"))
        assertFalse(result.retryable)
        assertTrue(result.userMessage.contains("quota"))
    }

    @Test
    fun `classifyLLMError billing should be terminal`() {
        val result = agentLoop.classifyLLMError(Exception("billing issue detected"))
        assertFalse(result.retryable)
        assertTrue(result.userMessage.contains("quota") || result.userMessage.contains("billing"))
    }

    @Test
    fun `classifyLLMError 429 should be retryable`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 429 Too Many Requests"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("Rate limited"))
    }

    @Test
    fun `classifyLLMError 429 with retry-after should include delay`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 429: try again in 30 seconds"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("30s"))
    }

    @Test
    fun `classifyLLMError 500 should be retryable`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 500 Internal Server Error"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("temporarily unavailable"))
    }

    @Test
    fun `classifyLLMError 502 should be retryable`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 502 Bad Gateway"))
        assertTrue(result.retryable)
    }

    @Test
    fun `classifyLLMError 503 should be retryable`() {
        val result = agentLoop.classifyLLMError(Exception("HTTP 503 Service Unavailable"))
        assertTrue(result.retryable)
    }

    @Test
    fun `classifyLLMError SocketTimeoutException should be retryable`() {
        val result = agentLoop.classifyLLMError(java.net.SocketTimeoutException("Read timed out"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("timed out"))
    }

    @Test
    fun `classifyLLMError UnknownHostException should be retryable`() {
        val result = agentLoop.classifyLLMError(java.net.UnknownHostException("api.openai.com"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("internet"))
    }

    @Test
    fun `classifyLLMError ConnectException should be retryable`() {
        val result = agentLoop.classifyLLMError(java.net.ConnectException("Connection refused"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("Cannot reach"))
    }

    @Test
    fun `classifyLLMError unknown exception should default to retryable`() {
        val result = agentLoop.classifyLLMError(RuntimeException("something weird"))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("something weird"))
    }

    @Test
    fun `classifyLLMError with blank message should use class name`() {
        val result = agentLoop.classifyLLMError(RuntimeException(""))
        assertTrue(result.retryable)
        assertTrue(result.userMessage.contains("RuntimeException"))
    }

    // ── LLMErrorClass data class ─────────────────────────────────────────

    @Test
    fun `LLMErrorClass should support structural equality`() {
        val a = LLMErrorClass(retryable = true, userMessage = "msg")
        val b = LLMErrorClass(retryable = true, userMessage = "msg")
        assertEquals(a, b)
    }
}
