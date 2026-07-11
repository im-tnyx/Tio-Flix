# Supabase Setup

This directory contains database migrations for Tio-Flix.

## Current migrations

```text
0001_profiles.sql
```

It creates:

- `public.profiles`
- User-owned RLS policies
- `updated_at` trigger
- Auth user to profile sync trigger

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
- Keep RLS enabled on user-owned tables.
- Sensitive admin actions belong in Edge Functions or trusted backend code.
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
