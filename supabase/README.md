# Supabase Setup

This directory contains database migrations for Tio-Flix.

## Current migrations

```text
0001_profiles.sql
0002_catalog.sql
0003_unified_content.sql
```

They create:

- `public.profiles`
- User-owned profile RLS policies
- Auth user to profile sync trigger
- Categories and unified catalog content
- Movies and web series through `content.content_type`
- Series seasons and episodes
- Published-content RLS policies
- Catalog indexes and timestamp triggers

## Catalog model

```text
content
├── MOVIE
└── SERIES
    └── series_seasons
        └── series_episodes
```

Both movies and series can be attached to the same categories through `content_categories`. A movie keeps its playback `stream_key` on `content`; series playback keys belong to individual episodes.

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

## Security rules

- Never commit the Supabase secret/service-role key.
- Android uses only the publishable key.
- Keep RLS enabled on every table exposed through the Data API.
- Only published catalog rows are readable by authenticated clients.
- Catalog administration belongs in a trusted backend or admin tool.
- Sensitive playback URLs must be issued by backend-controlled playback sessions.
- Review SQL before applying it to production.

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
