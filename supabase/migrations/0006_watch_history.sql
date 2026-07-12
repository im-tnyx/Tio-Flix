-- User-owned playback progress and continue-watching state.

create table if not exists public.watch_history (
    user_id uuid not null references auth.users(id) on delete cascade,
    content_id uuid not null references public.content(id) on delete cascade,
    episode_id uuid references public.series_episodes(id) on delete cascade,
    position_ms bigint not null default 0 check (position_ms >= 0),
    duration_ms bigint not null default 0 check (duration_ms >= 0),
    completed boolean not null default false,
    last_watched_at timestamptz not null default timezone('utc', now()),
    created_at timestamptz not null default timezone('utc', now()),
    updated_at timestamptz not null default timezone('utc', now()),
    primary key (user_id, content_id)
);

create index if not exists watch_history_user_recent_idx
on public.watch_history (user_id, last_watched_at desc);

create index if not exists watch_history_continue_idx
on public.watch_history (user_id, completed, last_watched_at desc);

alter table public.watch_history enable row level security;

drop policy if exists "Users can read own watch history" on public.watch_history;
create policy "Users can read own watch history"
on public.watch_history
for select
to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can insert own watch history" on public.watch_history;
create policy "Users can insert own watch history"
on public.watch_history
for insert
to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update own watch history" on public.watch_history;
create policy "Users can update own watch history"
on public.watch_history
for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop trigger if exists watch_history_set_updated_at on public.watch_history;
create trigger watch_history_set_updated_at
before update on public.watch_history
for each row execute function public.set_updated_at();

grant select, insert, update on public.watch_history to authenticated;

comment on table public.watch_history is
'Latest user-owned playback progress per content item. Series rows point to the last watched episode.';
