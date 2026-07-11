package com.tioflix.app.data.profile

import com.tioflix.app.domain.repository.ProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : ProfileRepository {

    override suspend fun syncCurrentUserProfile(): Result<Unit> = runCatching {
        val user = auth.currentUserOrNull()
            ?: error("No authenticated user is available for profile sync.")

        val metadata = user.userMetadata
        val provider = user.appMetadata["provider"]?.toString()?.trim('"')
            ?: user.identities?.firstOrNull()?.provider
            ?: "email"

        val profile = ProfileDto(
            id = user.id,
            email = user.email,
            fullName = metadata?.get("full_name")?.toString()?.trim('"')
                ?: metadata?.get("name")?.toString()?.trim('"'),
            avatarUrl = metadata?.get("avatar_url")?.toString()?.trim('"')
                ?: metadata?.get("picture")?.toString()?.trim('"'),
            provider = provider
        )

        postgrest["profiles"].insert(
            value = profile,
            upsert = true,
            onConflict = "id"
        )
    }
}
