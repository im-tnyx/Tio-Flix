# Supabase Setup

This directory contains database migrations and Edge Functions for Tio-Flix.

## Current migrations

```text
0001_profiles.sql
0002_catalog.sql
0003_unified_content.sql
0004_protect_playback_keys.sql
0005_playback_sessions.sql
```

They create:

- `public.profiles`
- User-owned profile RLS policies
- Auth user to profile sync trigger
- Categories and unified catalog content
- Movies and web series through `content.content_type`
- Series seasons and episodes
- Published-content RLS policies
- Protected server-only playback keys
- Playback session audit and rate-limit data

## Catalog model

```text
content
├── MOVIE
└── SERIES
    └── series_seasons
        └── series_episodes
```

Both movies and series can be attached to the same categories through `content_categories`. A movie keeps its playback `stream_key` on `content`; series playback keys belong to individual episodes. Android clients cannot select either stream-key column.

## Local setup

Install and authenticate the Supabase CLI, then link the repository to the correct project.

```bash
supabase login
supabase link --project-ref YOUR_PROJECT_REF
```

Apply migrations:

```bash
supabase db push
```

Check migration status:

```bash
supabase migration list
```

## Playback Edge Function

The Android app calls:

```text
POST https://YOUR_PROJECT_REF.functions.supabase.co/playback-session
Authorization: Bearer <user access token>
```

The function validates the user, checks published content, rate-limits session creation, reads the protected stream key with a server secret, generates a short-lived signed URL, and records an audit row.

Required project secrets:

```text
PLAYBACK_CDN_BASE_URL=https://cdn.example.com
PLAYBACK_SIGNING_SECRET=replace-with-a-long-random-secret
PLAYBACK_URL_TTL_SECONDS=300
```

Set and deploy using your installed Supabase CLI version after checking command help:

```bash
supabase secrets --help
supabase functions deploy --help
supabase secrets set PLAYBACK_CDN_BASE_URL=https://cdn.example.com PLAYBACK_SIGNING_SECRET=replace-me PLAYBACK_URL_TTL_SECONDS=300
supabase functions deploy playback-session
```

`supabase/config.toml` sets `verify_jwt = false` because the function supports Supabase's current publishable/secret key model and performs explicit user-token validation inside the handler.

The included signer uses the `hmac-query-v1` contract:

```text
signature input = path:expiry:userId:sessionId
query params    = exp, uid, sid, sig
```

Your CDN gateway or origin must validate this exact contract before serving the HLS manifest and segments. Replace the signer adapter when using a provider-specific token format such as Bunny Stream or Mux.

## Security rules

- Never commit the Supabase secret/service-role key.
- Android uses only the publishable key.
- Keep RLS enabled on every table exposed through the Data API.
- Only published catalog rows are readable by authenticated clients.
- Catalog administration belongs in a trusted backend or admin tool.
- Sensitive playback URLs must be short-lived and issued by the Edge Function.
- Run database lint/advisors and test the function before production deployment.

## Profiles flow

```text
Supabase Auth user created
↓
Database trigger inserts minimal profile
↓
User signs in through Tio-Flix
↓
Android profile repository upserts email/name/avatar/provider
```

The authenticated user can select, insert, and update only the profile row where `profiles.id = auth.uid()`.
