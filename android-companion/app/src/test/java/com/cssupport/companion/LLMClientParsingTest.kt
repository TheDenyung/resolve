package com.cssupport.companion

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for LLMClient's pure parsing functions:
 * - parseToolCallFromJson: maps tool name + JSON args -> AgentAction
 * - parseElementId: handles int/Long/String/missing element IDs
 */
class LLMClientParsingTest {

    private lateinit var client: LLMClient

    @Before
    fun setUp() {
        // LLMClient needs a config but we only test internal parsing functions.
        client = LLMClient(LLMConfig.openAI(apiKey = "test-key"))
    }

    // ── parseToolCallFromJson: type_message ──────────────────────────────

    @Test
    fun `parseToolCallFromJson should parse type_message`() {
        val input = JSONObject().put("text", "Hello world").put("elementId", 5)
        val action = client.parseToolCallFromJson("type_message", input)

        assertTrue(action is AgentAction.TypeMessage)
        val typed = action as AgentAction.TypeMessage
        assertEquals("Hello world", typed.text)
        assertEquals(5, typed.elementId)
    }

    @Test
    fun `parseToolCallFromJson should parse type_message without elementId`() {
        val input = JSONObject().put("text", "Hello")
        val action = client.parseToolCallFromJson("type_message", input)

        assertTrue(action is AgentAction.TypeMessage)
        assertNull((action as AgentAction.TypeMessage).elementId)
    }

    @Test
    fun `parseToolCallFromJson should default text to empty string`() {
        val input = JSONObject()
        val action = client.parseToolCallFromJson("type_message", input) as AgentAction.TypeMessage
        assertEquals("", action.text)
    }

    // ── parseToolCallFromJson: click_element ─────────────────────────────

    @Test
    fun `parseToolCallFromJson should parse click_element`() {
        val input = JSONObject()
            .put("elementId", 7)
            .put("label", "Submit")
            .put("expectedOutcome", "Form submitted")
        val action = client.parseToolCallFromJson("click_element", input)

        assertTrue(action is AgentAction.ClickElement)
        val click = action as AgentAction.ClickElement
        assertEquals(7, click.elementId)
        assertEquals("Submit", click.label)
        assertEquals("Form submitted", click.expectedOutcome)
    }

    @Test
    fun `parseToolCallFromJson should parse click_element with blank label as null`() {
        val input = JSONObject().put("elementId", 3).put("label", "")
        val action = client.parseToolCallFromJson("click_element", input) as AgentAction.ClickElement
        assertNull(action.label)
    }

    // ── parseToolCallFromJson: click_button (backward compat) ────────────

    @Test
    fun `parseToolCallFromJson should parse click_button as ClickElement`() {
        val input = JSONObject().put("buttonLabel", "Cancel Order")
        val action = client.parseToolCallFromJson("click_button", input)

        assertTrue(action is AgentAction.ClickElement)
        val click = action as AgentAction.ClickElement
        assertNull(click.elementId)
        assertEquals("Cancel Order", click.label)
    }

    @Test
    fun `parseToolCallFromJson click_button should fallback to label field`() {
        val input = JSONObject().put("label", "OK")
        val action = client.parseToolCallFromJson("click_button", input) as AgentAction.ClickElement
        assertEquals("OK", action.label)
    }

    // ── parseToolCallFromJson: scroll_down / scroll_up ───────────────────

    @Test
    fun `parseToolCallFromJson should parse scroll_down`() {
        val input = JSONObject().put("reason", "Need to see more items")
        val action = client.parseToolCallFromJson("scroll_down", input)

        assertTrue(action is AgentAction.ScrollDown)
        assertEquals("Need to see more items", (action as AgentAction.ScrollDown).reason)
    }

    @Test
    fun `parseToolCallFromJson should parse scroll_up`() {
        val input = JSONObject().put("reason", "Go back to top")
        val action = client.parseToolCallFromJson("scroll_up", input)

        assertTrue(action is AgentAction.ScrollUp)
        assertEquals("Go back to top", (action as AgentAction.ScrollUp).reason)
    }

    // ── parseToolCallFromJson: wait_for_response ─────────────────────────

    @Test
    fun `parseToolCallFromJson should parse wait_for_response`() {
        val input = JSONObject().put("reason", "Page loading")
        val action = client.parseToolCallFromJson("wait_for_response", input)

        assertTrue(action is AgentAction.Wait)
        assertEquals("Page loading", (action as AgentAction.Wait).reason)
    }

    // ── parseToolCallFromJson: upload_file ───────────────────────────────

    @Test
    fun `parseToolCallFromJson should parse upload_file`() {
        val input = JSONObject().put("fileDescription", "Screenshot of error")
        val action = client.parseToolCallFromJson("upload_file", input)

        assertTrue(action is AgentAction.UploadFile)
        assertEquals("Screenshot of error", (action as AgentAction.UploadFile).fileDescription)
    }

    // ── parseToolCallFromJson: press_back ────────────────────────────────

    @Test
    fun `parseToolCallFromJson should parse press_back`() {
        val input = JSONObject().put("reason", "Wrong screen")
        val action = client.parseToolCallFromJson("press_back", input)

        assertTrue(action is AgentAction.PressBack)
        assertEquals("Wrong screen", (action as AgentAction.PressBack).reason)
    }

    // ── parseToolCallFromJson: request_human_review ──────────────────────

    @Test
    fun `parseToolCallFromJson should parse request_human_review`() {
        val input = JSONObject()
            .put("reason", "Need OTP")
            .put("needsInput", true)
            .put("inputPrompt", "Please enter the OTP")
        val action = client.parseToolCallFromJson("request_human_review", input)

        assertTrue(action is AgentAction.RequestHumanReview)
        val review = action as AgentAction.RequestHumanReview
        assertEquals("Need OTP", review.reason)
        assertTrue(review.needsInput)
        assertEquals("Please enter the OTP", review.inputPrompt)
    }

    @Test
    fun `parseToolCallFromJson request_human_review defaults needsInput to false`() {
        val input = JSONObject().put("reason", "Stuck")
        val action = client.parseToolCallFromJson("request_human_review", input) as AgentAction.RequestHumanReview
        assertFalse(action.needsInput)
    }

    @Test
    fun `parseToolCallFromJson request_human_review should have null inputPrompt when missing`() {
        val input = JSONObject().put("reason", "Stuck")
        val action = client.parseToolCallFromJson("request_human_review", input) as AgentAction.RequestHumanReview
        assertNull(action.inputPrompt)
    }

    // ── parseToolCallFromJson: mark_resolved ─────────────────────────────

    @Test
    fun `parseToolCallFromJson should parse mark_resolved`() {
        val input = JSONObject().put("summary", "Refund processed successfully")
        val action = client.parseToolCallFromJson("mark_resolved", input)

        assertTrue(action is AgentAction.MarkResolved)
        assertEquals("Refund processed successfully", (action as AgentAction.MarkResolved).summary)
    }

    @Test
    fun `parseToolCallFromJson mark_resolved should default summary`() {
        val input = JSONObject()
        val action = client.parseToolCallFromJson("mark_resolved", input) as AgentAction.MarkResolved
        assertEquals("Issue resolved", action.summary)
    }

    // ── parseToolCallFromJson: update_plan ───────────────────────────────

    @Test
    fun `parseToolCallFromJson should parse update_plan with steps`() {
        val steps = JSONArray()
            .put(JSONObject().put("step", "Open app").put("status", "completed"))
            .put(JSONObject().put("step", "Navigate to orders").put("status", "in_progress"))
            .put(JSONObject().put("step", "Request refund").put("status", "pending"))
        val input = JSONObject()
            .put("explanation", "Working on refund")
            .put("steps", steps)

        val action = client.parseToolCallFromJson("update_plan", input)

        assertTrue(action is AgentAction.UpdatePlan)
        val plan = action as AgentAction.UpdatePlan
        assertEquals("Working on refund", plan.explanation)
        assertEquals(3, plan.steps.size)
        assertEquals("Open app", plan.steps[0].step)
        assertEquals("completed", plan.steps[0].status)
        assertEquals("in_progress", plan.steps[1].status)
        assertEquals("pending", plan.steps[2].status)
    }

    @Test
    fun `parseToolCallFromJson update_plan should handle empty steps`() {
        val input = JSONObject().put("explanation", "Initial plan").put("steps", JSONArray())
        val action = client.parseToolCallFromJson("update_plan", input) as AgentAction.UpdatePlan
        assertEquals(0, action.steps.size)
    }

    @Test
    fun `parseToolCallFromJson update_plan should handle missing steps array`() {
        val input = JSONObject().put("explanation", "Just thinking")
        val action = client.parseToolCallFromJson("update_plan", input) as AgentAction.UpdatePlan
        assertEquals(0, action.steps.size)
    }

    // ── parseToolCallFromJson: unknown tool ──────────────────────────────

    @Test
    fun `parseToolCallFromJson should return Wait for unknown tool name`() {
        val input = JSONObject()
        val action = client.parseToolCallFromJson("nonexistent_tool", input)

        assertTrue(action is AgentAction.Wait)
        assertTrue((action as AgentAction.Wait).reason.contains("Unknown tool"))
    }

    // ── parseElementId ───────────────────────────────────────────────────

    @Test
    fun `parseElementId should parse integer value`() {
        val json = JSONObject().put("elementId", 42)
        val result = client.parseElementId(json, "elementId")
        assertEquals(42, result)
    }

    @Test
    fun `parseElementId should parse string value`() {
        val json = JSONObject().put("elementId", "15")
        val result = client.parseElementId(json, "elementId")
        assertEquals(15, result)
    }

    @Test
    fun `parseElementId should return null for missing key`() {
        val json = JSONObject()
        val result = client.parseElementId(json, "elementId")
        assertNull(result)
    }

    @Test
    fun `parseElementId should return null for zero`() {
        val json = JSONObject().put("elementId", 0)
        val result = client.parseElementId(json, "elementId")
        assertNull(result)
    }

    @Test
    fun `parseElementId should return null for negative values`() {
        val json = JSONObject().put("elementId", -5)
        val result = client.parseElementId(json, "elementId")
        assertNull(result)
    }

    @Test
    fun `parseElementId should handle string with whitespace`() {
        val json = JSONObject().put("elementId", " 10 ")
        val result = client.parseElementId(json, "elementId")
        assertEquals(10, result)
    }

    @Test
    fun `parseElementId should return null for non-numeric string`() {
        val json = JSONObject().put("elementId", "abc")
        val result = client.parseElementId(json, "elementId")
        assertNull(result)
    }

    @Test
    fun `parseElementId should handle Long value`() {
        val json = JSONObject().put("elementId", 99L)
        val result = client.parseElementId(json, "elementId")
        assertEquals(99, result)
    }

    @Test
    fun `parseElementId should return null for negative string value`() {
        val json = JSONObject().put("elementId", "-3")
        val result = client.parseElementId(json, "elementId")
        assertNull(result)
    }

    // ── LLMConfig factory methods ────────────────────────────────────────

    @Test
    fun `LLMConfig azureDefault should use correct provider and model`() {
        val config = LLMConfig.azureDefault(apiKey = "key", endpoint = "https://my.azure.com")
        assertEquals(LLMProvider.AZURE_OPENAI, config.provider)
        assertEquals("gpt-5-nano", config.model)
        assertEquals("https://my.azure.com", config.endpoint)
        assertEquals("2024-10-21", config.apiVersion)
    }

    @Test
    fun `LLMConfig openAI should default to gpt-5-mini`() {
        val config = LLMConfig.openAI(apiKey = "key")
        assertEquals(LLMProvider.OPENAI, config.provider)
        assertEquals("gpt-5-mini", config.model)
    }

    @Test
    fun `LLMConfig anthropic should default to claude-sonnet model`() {
        val config = LLMConfig.anthropic(apiKey = "key")
        assertEquals(LLMProvider.ANTHROPIC, config.provider)
        assertTrue(config.model.startsWith("claude-sonnet"))
    }

    @Test
    fun `LLMConfig custom should use all provided values`() {
        val config = LLMConfig.custom(apiKey = "key", endpoint = "https://local:8080", model = "my-model")
        assertEquals(LLMProvider.CUSTOM, config.provider)
        assertEquals("my-model", config.model)
        assertEquals("https://local:8080", config.endpoint)
    }

    // ── Data class tests ─────────────────────────────────────────────────

    @Test
    fun `AgentDecision wait factory should create Wait action`() {
        val decision = AgentDecision.wait("test reason")
        assertTrue(decision.action is AgentAction.Wait)
        assertEquals("test reason", (decision.action as AgentAction.Wait).reason)
        assertEquals("test reason", decision.reasoning)
    }

    @Test
    fun `PlanStep should default status to pending`() {
        val step = PlanStep(step = "Open settings")
        assertEquals("pending", step.status)
    }

    @Test
    fun `ConversationMessage types should hold correct data`() {
        val obs = ConversationMessage.UserObservation("screen content")
        assertEquals("screen content", obs.content)

        val call = ConversationMessage.AssistantToolCall("id1", "click_element", "{}", "thinking")
        assertEquals("id1", call.toolCallId)
        assertEquals("click_element", call.toolName)

        val result = ConversationMessage.ToolResult("id1", "success")
        assertEquals("success", result.result)
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.Assert.assertFalse(condition)
    }
}
