# Tio-Flix

Tio-Flix is an Android Kotlin OTT/movie streaming app project. This repository is documented for a Netflix/SonyLIV/Prime Video style app experience with legal video streaming, normal email auth, Continue with Google, ads, watch history, favorites, and a production-style player architecture.

## Core goals

The app should support:

1. Authentication
   - Email and password signup
   - Email and password login
   - Forgot password
   - Continue with Google
   - Secure logout

2. OTT features
   - Home screen categories
   - Movie detail page
   - HLS video playback
   - Netflix-style player controls
   - Resume playback / Continue Watching
   - Favorites / My List
   - Search
   - Subtitles and audio tracks

3. Ads model
   - Pre-roll ads before movie start
   - Mid-roll ads during movie playback
   - Ad event tracking
   - Google IMA / Google Ad Manager integration

## Recommended stack

```text
Android App: Kotlin + Jetpack Compose
Architecture: MVVM + Repository Pattern
Player: Android Media3 ExoPlayer
Ads: Google IMA SDK / Google Ad Manager
Auth: Supabase Auth
Database: Supabase PostgreSQL
Video Streaming: Bunny Stream / Mux
Backend: Supabase Edge Functions / Ktor / Spring Boot
```

## Documentation

### Foundation

- [Project Overview](docs/01-project/PROJECT_OVERVIEW.md)
- [Roadmap](docs/01-project/ROADMAP.md)
- [System Architecture](docs/02-architecture/SYSTEM_ARCHITECTURE.md)
- [App Architecture](docs/02-architecture/APP_ARCHITECTURE.md)

### Auth

- [Auth Overview](docs/auth-overview.md)
- [Google Sign-In Setup](docs/google-signin-setup.md)
- [Supabase Auth & Tables](docs/supabase-auth-schema.md)
- [Android Auth Implementation](docs/android-auth-implementation.md)

### OTT Core

- [Database Schema](docs/04-database/DATABASE_SCHEMA.md)
- [Video Streaming](docs/05-streaming/VIDEO_STREAMING.md)
- [Player Architecture](docs/06-player/PLAYER_ARCHITECTURE.md)
- [Ads Architecture](docs/07-ads/ADS_ARCHITECTURE.md)
- [API Specification](docs/08-api/API_SPEC.md)
- [UI Guidelines](docs/09-ui/UI_GUIDELINES.md)

## Legal note

Use this project only with legal/licensed video content. Authentication and app security do not replace content licensing, DRM, or proper CDN/video protection.
