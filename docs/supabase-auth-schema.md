# Supabase Auth & Database Schema

This document explains how Supabase can be used for normal auth, Continue with Google, and user-related movie app data.

## Supabase role

Supabase is used for:

- Authentication
- User profiles
- Movie metadata
- Watch history
- Favorites
- Continue watching
- Ad event tracking

Video files should not be stored directly in Supabase for production OTT streaming. Use Bunny Stream, Mux, or another video streaming platform for HLS video delivery.

## Auth users

Supabase automatically manages authenticated users in:

```text
auth.users
```

Do not manually edit this table from the Android app.

## Profiles table

Create a public profile table linked to auth.users.

```sql
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  full_name text,
  avatar_url text,
  provider text default 'email',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
```

## Movies table

```sql
create table public.movies (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text,
  poster_url text,
  banner_url text,
  video_url text not null,
  duration_seconds integer,
  release_year integer,
  category text,
  created_at timestamptz default now()
);
```

## Watch history table

```sql
create table public.watch_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade,
  movie_id uuid references public.movies(id) on delete cascade,
  progress_seconds integer default 0,
  completed boolean default false,
  updated_at timestamptz default now(),
  unique(user_id, movie_id)
);
```

## Favorites table

```sql
create table public.favorites (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade,
  movie_id uuid references public.movies(id) on delete cascade,
  created_at timestamptz default now(),
  unique(user_id, movie_id)
);
```

## Ad breaks table

Use this table if each movie has different ad break points.

```sql
create table public.movie_ad_breaks (
  id uuid primary key default gen_random_uuid(),
  movie_id uuid references public.movies(id) on delete cascade,
  break_seconds integer not null,
  ad_tag_url text,
  created_at timestamptz default now()
);
```

Example:

```text
0 seconds = pre-roll ad
600 seconds = mid-roll ad after 10 minutes
1200 seconds = mid-roll ad after 20 minutes
```

## Ad events table

Use this for analytics and reporting.

```sql
create table public.ad_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete set null,
  movie_id uuid references public.movies(id) on delete cascade,
  event_type text not null,
  break_seconds integer,
  created_at timestamptz default now()
);
```

Example event types:

```text
ad_started
ad_completed
ad_skipped
ad_error
```

## Enable Row Level Security

```sql
alter table public.profiles enable row level security;
alter table public.watch_history enable row level security;
alter table public.favorites enable row level security;
alter table public.ad_events enable row level security;
```

## RLS policies

Profiles:

```sql
create policy "Users can read own profile"
on public.profiles
for select
using (auth.uid() = id);

create policy "Users can update own profile"
on public.profiles
for update
using (auth.uid() = id);
```

Watch history:

```sql
create policy "Users can manage own watch history"
on public.watch_history
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);
```

Favorites:

```sql
create policy "Users can manage own favorites"
on public.favorites
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);
```

Ad events:

```sql
create policy "Users can insert own ad events"
on public.ad_events
for insert
with check (auth.uid() = user_id);
```

## Important security warning

Never put the Supabase service role key in the Android app. Android should only use the public anon key. Sensitive admin actions should run on a backend server or Supabase Edge Function.
