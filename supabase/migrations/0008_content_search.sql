-- Ranked full-text search over published movies and series.

alter table public.content
add column if not exists search_vector tsvector
generated always as (
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(language, '')), 'C') ||
    setweight(to_tsvector('simple', coalesce(release_year::text, '')), 'C')
) stored;

create index if not exists content_search_vector_idx
on public.content using gin (search_vector);

create or replace function public.search_content(
    search_text text,
    result_limit integer default 30
)
returns setof public.content
language sql
stable
security invoker
set search_path = ''
as $$
    select content.*
    from public.content as content
    where content.is_published = true
      and nullif(btrim(search_text), '') is not null
      and content.search_vector @@ websearch_to_tsquery('simple', search_text)
    order by
      ts_rank_cd(content.search_vector, websearch_to_tsquery('simple', search_text)) desc,
      content.is_featured desc,
      content.published_at desc nulls last
    limit least(greatest(result_limit, 1), 50);
$$;

revoke all on function public.search_content(text, integer) from public, anon;
grant execute on function public.search_content(text, integer) to authenticated;

comment on function public.search_content(text, integer) is
'Authenticated ranked search over published movie and series metadata.';
