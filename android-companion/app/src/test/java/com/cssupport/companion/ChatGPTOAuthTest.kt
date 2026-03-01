package com.cssupport.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

/**
 * Tests for ChatGPTOAuth pure/deterministic methods.
 *
 * Note: generateCodeVerifier/generateCodeChallenge/generateState use android.util.Base64
 * which returns empty strings in unit tests (via isReturnDefaultValues = true).
 * We test structural properties where possible and focus primarily on buildAuthorizationUrl
 * which is fully deterministic.
 */
class ChatGPTOAuthTest {

    // ── buildAuthorizationUrl ────────────────────────────────────────────

    @Test
    fun `buildAuthorizationUrl should start with OpenAI auth endpoint`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("test_challenge", "test_state")
        assertTrue(url.startsWith("https://auth.openai.com/oauth/authorize?"))
    }

    @Test
    fun `buildAuthorizationUrl should include response_type=code`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state")
        assertTrue(url.contains("response_type=code"))
    }

    @Test
    fun `buildAuthorizationUrl should include client_id`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state")
        assertTrue(url.contains("client_id="))
        // Verify client ID value is present (URL-encoded)
        assertTrue(url.contains(ChatGPTOAuth.CLIENT_ID) || url.contains(java.net.URLEncoder.encode(ChatGPTOAuth.CLIENT_ID, "UTF-8")))
    }

    @Test
    fun `buildAuthorizationUrl should include redirect_uri`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state")
        // REDIRECT_URI = "http://localhost:1455/auth/callback"
        val encodedRedirect = java.net.URLEncoder.encode(ChatGPTOAuth.REDIRECT_URI, "UTF-8")
        assertTrue(url.contains("redirect_uri=$encodedRedirect"))
    }

    @Test
    fun `buildAuthorizationUrl should include code_challenge`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("my_challenge_value", "state")
        val encoded = java.net.URLEncoder.encode("my_challenge_value", "UTF-8")
        assertTrue(url.contains("code_challenge=$encoded"))
    }

    @Test
    fun `buildAuthorizationUrl should use S256 challenge method`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state")
        assertTrue(url.contains("code_challenge_method=S256"))
    }

    @Test
    fun `buildAuthorizationUrl should include state parameter`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("challenge", "my_state_123")
        val encoded = java.net.URLEncoder.encode("my_state_123", "UTF-8")
        assertTrue(url.contains("state=$encoded"))
    }

    @Test
    fun `buildAuthorizationUrl should include offline_access scope`() {
        val url = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state")
        // The scope includes "openid profile email offline_access" which gets URL-encoded
        val decoded = URLDecoder.decode(url, "UTF-8")
        assertTrue(decoded.contains("offline_access"))
        assertTrue(decoded.contains("openid"))
        assertTrue(decoded.contains("profile"))
        assertTrue(decoded.contains("email"))
    }

    @Test
    fun `buildAuthorizationUrl should URL-encode special characters in challenge`() {
        val challenge = "abc+def/ghi=jkl"
        val url = ChatGPTOAuth.buildAuthorizationUrl(challenge, "state")
        // Special chars should be encoded
        assertFalse(url.contains("code_challenge=abc+def/ghi=jkl"))
        val encoded = java.net.URLEncoder.encode(challenge, "UTF-8")
        assertTrue(url.contains("code_challenge=$encoded"))
    }

    @Test
    fun `buildAuthorizationUrl should produce different URLs for different states`() {
        val url1 = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state_A")
        val url2 = ChatGPTOAuth.buildAuthorizationUrl("challenge", "state_B")
        assertNotEquals(url1, url2)
    }

    @Test
    fun `buildAuthorizationUrl should produce different URLs for different challenges`() {
        val url1 = ChatGPTOAuth.buildAuthorizationUrl("challenge_A", "state")
        val url2 = ChatGPTOAuth.buildAuthorizationUrl("challenge_B", "state")
        assertNotEquals(url1, url2)
    }

    // ── Constants / configuration ────────────────────────────────────────

    @Test
    fun `REDIRECT_URI should use localhost on port 1455`() {
        assertEquals("http://localhost:1455/auth/callback", ChatGPTOAuth.REDIRECT_URI)
    }

    @Test
    fun `DEFAULT_MODEL should be gpt-5_3-codex`() {
        assertEquals("gpt-5.3-codex", ChatGPTOAuth.DEFAULT_MODEL)
    }

    @Test
    fun `CLIENT_ID should not be blank`() {
        assertTrue(ChatGPTOAuth.CLIENT_ID.isNotBlank())
    }

    // ── OAuthCallbackResult data class ───────────────────────────────────

    @Test
    fun `OAuthCallbackResult should store code and state`() {
        val result = OAuthCallbackResult(code = "auth_code_123", state = "csrf_state_456")
        assertEquals("auth_code_123", result.code)
        assertEquals("csrf_state_456", result.state)
    }

    @Test
    fun `OAuthCallbackResult equality should be structural`() {
        val a = OAuthCallbackResult(code = "code", state = "state")
        val b = OAuthCallbackResult(code = "code", state = "state")
        assertEquals(a, b)
    }

    // ── OAuthTokenResponse data class ────────────────────────────────────

    @Test
    fun `OAuthTokenResponse should store all fields`() {
        val response = OAuthTokenResponse(
            accessToken = "access_123",
            refreshToken = "refresh_456",
            expiresIn = 3600,
            tokenType = "Bearer",
        )
        assertEquals("access_123", response.accessToken)
        assertEquals("refresh_456", response.refreshToken)
        assertEquals(3600L, response.expiresIn)
        assertEquals("Bearer", response.tokenType)
    }

    @Test
    fun `OAuthTokenResponse should allow null refresh token`() {
        val response = OAuthTokenResponse(
            accessToken = "access_123",
            refreshToken = null,
            expiresIn = 3600,
            tokenType = "Bearer",
        )
        assertEquals(null, response.refreshToken)
    }

    // ── OAuthException ───────────────────────────────────────────────────

    @Test
    fun `OAuthException should carry message`() {
        val ex = OAuthException("invalid_grant: token expired")
        assertEquals("invalid_grant: token expired", ex.message)
    }

    @Test
    fun `OAuthException should carry optional cause`() {
        val cause = RuntimeException("network error")
        val ex = OAuthException("failed", cause)
        assertEquals(cause, ex.cause)
    }
}
