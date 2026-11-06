package com.tioflix.app.domain.model

import java.net.URI
import java.net.URLDecoder

sealed interface PasswordRecoveryLink {
    data object NotRecovery : PasswordRecoveryLink
    data object Invalid : PasswordRecoveryLink

    // Credentials stay in memory and never become navigation or saved-state arguments.
    class Credentials(val accessToken: String, val refreshToken: String, val expiresIn: Long) : PasswordRecoveryLink {
        override fun toString(): String = "PasswordRecoveryLink.Credentials(redacted)"
    }

    companion object {
        fun parse(url: String): PasswordRecoveryLink {
            val uri = runCatching { URI(url.substringBefore('#').substringBefore('?')) }.getOrNull() ?: return NotRecovery
            if (uri.scheme != "tioflix" || uri.host != "auth") return NotRecovery
            val resetPath = uri.path == "/reset-password"
            val fragment = runCatching { parameters(url.substringAfter('#', "")) }.getOrNull()
                ?: return if (resetPath) Invalid else NotRecovery
            val recoveryType = fragment["type"] == "recovery"
            if (!resetPath && !(uri.path.isNullOrEmpty() && recoveryType)) return NotRecovery
            if (!recoveryType || fragment.containsKey("error")) return Invalid
            val accessToken = fragment["access_token"]?.takeIf { it.isNotBlank() } ?: return Invalid
            val refreshToken = fragment["refresh_token"]?.takeIf { it.isNotBlank() } ?: return Invalid
            val expiresIn = fragment["expires_in"]?.toLongOrNull()?.takeIf { it in 1..86400 } ?: return Invalid
            if (!fragment["token_type"].equals("bearer", ignoreCase = true)) return Invalid
            return Credentials(accessToken, refreshToken, expiresIn)
        }

        private fun parameters(raw: String?): Map<String, String> {
            val result = mutableMapOf<String, String>()
            raw?.split('&')?.filter { it.isNotEmpty() }?.forEach {
                val pair = it.split('=', limit = 2)
                val key = URLDecoder.decode(pair[0], "UTF-8")
                check(!result.containsKey(key))
                result[key] = URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
            }
            return result
        }
    }
}
