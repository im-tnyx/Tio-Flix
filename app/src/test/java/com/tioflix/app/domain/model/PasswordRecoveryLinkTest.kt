package com.tioflix.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRecoveryLinkTest {
    private val fragment = "type=recovery&access_token=test-access&refresh_token=test-refresh&expires_in=3600&token_type=bearer"

    @Test fun recoveryRedirectExtractsCredentials() {
        val link = PasswordRecoveryLink.parse("tioflix://auth/reset-password#$fragment") as PasswordRecoveryLink.Credentials
        assertEquals("test-access", link.accessToken)
        assertEquals("test-refresh", link.refreshToken)
        assertEquals(3600L, link.expiresIn)
        assertFalse(link.toString().contains("test-access"))
    }

    @Test fun defaultAuthCallbackWithRecoveryTypeIsAccepted() {
        assertTrue(PasswordRecoveryLink.parse("tioflix://auth#$fragment") is PasswordRecoveryLink.Credentials)
    }

    @Test fun signupCallbackIsNotRecovery() {
        assertEquals(PasswordRecoveryLink.NotRecovery, PasswordRecoveryLink.parse("tioflix://auth#type=signup&access_token=test-access"))
    }

    @Test fun foreignHostOrSchemeIsNotAccepted() {
        listOf("https://auth/reset-password", "tioflix://other/reset-password", "tioflix://auth/other").forEach {
            assertEquals(PasswordRecoveryLink.NotRecovery, PasswordRecoveryLink.parse("$it#$fragment"))
        }
    }

    @Test fun barePathCannotReuseAnExistingSession() {
        assertEquals(PasswordRecoveryLink.Invalid, PasswordRecoveryLink.parse("tioflix://auth/reset-password"))
    }

    @Test fun missingOrConflictingCredentialsAreRejected() {
        listOf(
            fragment.replace("access_token=test-access&", ""),
            fragment.replace("refresh_token=test-refresh&", ""),
            fragment.replace("type=recovery", "type=signup"),
            fragment.replace("expires_in=3600", "expires_in=-1"),
            fragment.replace("token_type=bearer", "token_type=unknown"),
            "$fragment&access_token=duplicate",
            "$fragment&error=access_denied"
        ).forEach {
            assertEquals(PasswordRecoveryLink.Invalid, PasswordRecoveryLink.parse("tioflix://auth/reset-password#$it"))
        }
    }

    @Test fun expiredAndPkceLinksShowInvalidWithoutCrashing() {
        listOf(
            "tioflix://auth/reset-password#error=access_denied&error_code=otp_expired",
            "tioflix://auth/reset-password?code=test-code"
        ).forEach { assertEquals(PasswordRecoveryLink.Invalid, PasswordRecoveryLink.parse(it)) }
    }

    @Test fun encodedCredentialCharactersArePreserved() {
        val link = PasswordRecoveryLink.parse("tioflix://auth/reset-password#${fragment.replace("test-access", "test%2Baccess%3D")}") as PasswordRecoveryLink.Credentials
        assertEquals("test+access=", link.accessToken)
    }

    @Test fun malformedRecoveryParametersAreRejectedWithoutSdkFallback() {
        listOf("type=recovery&access_token=%ZZ", "error_description=Link has expired").forEach {
            assertEquals(PasswordRecoveryLink.Invalid, PasswordRecoveryLink.parse("tioflix://auth/reset-password#$it"))
        }
    }
}
