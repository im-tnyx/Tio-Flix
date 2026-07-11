-- Unify movies and web series under one catalog model while preserving existing data.

alter table public.movies rename to content;
alter table public.movie_categories rename to content_categories;
alter table public.content_categories rename column movie_id to content_id;

alter table public.content
    add column if not exists content_type text not null default 'MOVIE',
    add column if not exists total_seasons integer;

alter table public.content
    drop constraint if exists content_content_type_check;
alter table public.content
    add constraint content_content_type_check
    check (content_type in ('MOVIE', 'SERIES'));

alter table public.content
    drop constraint if exists content_movie_duration_check;
alter table public.content
    add constraint content_movie_duration_check
    check (
        (content_type = 'MOVIE' and duration_minutes is not null and duration_minutes > 0)
        or
        (content_type = 'SERIES' and duration_minutes is null)
    ) not valid;

create table if not exists public.series_seasons (
    id uuid primary key default gen_random_uuid(),
    content_id uuid not null references public.content(id) on delete cascade,
    season_number integer not null check (season_number > 0),
    title text,
    description text,
    poster_url text,
    is_published boolean not null default false,
    created_at timestamptz not null default timezone('utc', now()),
    updated_at timestamptz not null default timezone('utc', now()),
    unique (content_id, season_number)
);

create table if not exists public.series_episodes (
    id uuid primary key default gen_random_uuid(),
    season_id uuid not null references public.series_seasons(id) on delete cascade,
    episode_number integer not null check (episode_number > 0),
    title text not null,
    description text,
    thumbnail_url text,
    duration_minutes integer not null check (duration_minutes > 0),
    stream_key text,
    is_published boolean not null default false,
    published_at timestamptz,
    created_at timestamptz not null default timezone('utc', now()),
    updated_at timestamptz not null default timezone('utc', now()),
    unique (season_id, episode_number)
);

create index if not exists content_type_published_idx
on public.content (content_type, is_published, published_at desc);

create index if not exists seasons_content_sort_idx
on public.series_seasons (content_id, season_number);

create index if not exists episodes_season_sort_idx
on public.series_episodes (season_id, episode_number);

alter table public.series_seasons enable row level security;
alter table public.series_episodes enable row level security;

-- Replace legacy catalog policy names with unified content terminology.
drop policy if exists "Authenticated users can read published movies" on public.content;
drop policy if exists "Authenticated users can read published movie categories" on public.content_categories;

create policy "Authenticated users can read published content"
on public.content
for select
to authenticated
using (is_published = true);

create policy "Authenticated users can read published content categories"
on public.content_categories
for select
to authenticated
using (
    exists (
        select 1
        from public.content
        where content.id = content_categories.content_id
          and content.is_published = true
    )
    and exists (
        select 1
        from public.categories
        where categories.id = content_categories.category_id
          and categories.is_active = true
    )
);

create policy "Authenticated users can read published seasons"
on public.series_seasons
for select
to authenticated
using (
    is_published = true
    and exists (
        select 1
        from public.content
        where content.id = series_seasons.content_id
          and content.content_type = 'SERIES'
          and content.is_published = true
    )
);

create policy "Authenticated users can read published episodes"
on public.series_episodes
for select
to authenticated
using (
    is_published = true
    and exists (
        select 1
        from public.series_seasons
        join public.content on content.id = series_seasons.content_id
        where series_seasons.id = series_episodes.season_id
          and series_seasons.is_published = true
          and content.content_type = 'SERIES'
          and content.is_published = true
    )
);

-- Reuse the timestamp trigger function created by the profiles migration.
drop trigger if exists content_set_updated_at on public.content;
create trigger content_set_updated_at
before update on public.content
for each row execute function public.set_updated_at();

drop trigger if exists series_seasons_set_updated_at on public.series_seasons;
create trigger series_seasons_set_updated_at
before update on public.series_seasons
for each row execute function public.set_updated_at();

drop trigger if exists series_episodes_set_updated_at on public.series_episodes;
create trigger series_episodes_set_updated_at
before update on public.series_episodes
for each row execute function public.set_updated_at();

grant select on public.content to authenticated;
grant select on public.content_categories to authenticated;
grant select on public.series_seasons to authenticated;
grant select on public.series_episodes to authenticated;
