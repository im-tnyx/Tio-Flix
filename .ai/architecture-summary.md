# Architecture Summary

Tio-Flix uses MVVM + Repository Pattern.

## Layer direction

```text
UI Layer
↓
ViewModel Layer
↓
Repository Layer
↓
Data Source Layer
↓
Supabase / Backend / Player / Ads SDK
```

## Core rule

UI should render state and send user events. Business logic belongs in ViewModels, use cases, repositories, or data sources.

## Main systems

- Auth: Supabase Auth
- Database: Supabase PostgreSQL
- Streaming: Bunny Stream / Mux HLS URLs
- Player: Media3 ExoPlayer
- Ads: Google IMA SDK / Google Ad Manager
- Platform: Mobile, Tablet, Android TV, Fire TV

## Sensitive operations

Sensitive operations should be backend-controlled:

- Admin movie creation
- Playback token generation
- Signed video URL generation
- Ad enforcement verification
- Video provider API calls
