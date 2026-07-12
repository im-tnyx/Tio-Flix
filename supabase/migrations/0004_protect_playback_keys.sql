-- Playback identifiers must not be readable by Android clients.
-- The trusted playback backend reads them using server-side credentials and returns short-lived signed URLs.

revoke select on public.content from authenticated;
revoke select on public.series_episodes from authenticated;

grant select (
    id,
    content_type,
    title,
    description,
    poster_url,
    backdrop_url,
    trailer_url,
    release_year,
    duration_minutes,
    maturity_rating,
    language,
    is_featured,
    is_published,
    published_at,
    total_seasons,
    created_at,
    updated_at
) on public.content to authenticated;

grant select (
    id,
    season_id,
    episode_number,
    title,
    description,
    thumbnail_url,
    duration_minutes,
    is_published,
    published_at,
    created_at,
    updated_at
) on public.series_episodes to authenticated;

-- Intentionally excluded:
-- public.content.stream_key
-- public.series_episodes.stream_key
