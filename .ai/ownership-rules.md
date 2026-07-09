# Ownership Rules

Use these rules when adding or changing code.

## Feature ownership

```text
Auth owns login, signup, Google sign-in, logout, and session checks.
Home owns movie rows, banners, and category browsing.
Search owns query UI and search results.
Movie Detail owns detail metadata and Watch Now entry point.
Player owns ExoPlayer lifecycle, controls, subtitles, audio, quality, and resume playback.
Ads owns IMA integration, ad breaks, ad events, and ad enforcement client flow.
Profile owns user profile, logout entry, and user settings.
Platform owns TV/Fire TV focus and remote behavior.
```

## Repository ownership

Repositories should hide data-source details from ViewModels.

```text
AuthRepository
MovieRepository
WatchHistoryRepository
FavoritesRepository
PlayerRepository
AdsRepository
ProfileRepository
```

## Do not mix boundaries

- Do not put Supabase queries directly inside Compose screens.
- Do not put ExoPlayer lifecycle logic inside unrelated feature repositories.
- Do not put ad enforcement only in client UI.
- Do not put service-role keys or provider secrets anywhere in Android code.
