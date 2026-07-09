# Project Overview

Tio-Flix is an Android Kotlin OTT streaming app designed for a Netflix/SonyLIV/Prime Video style experience.

## Product vision

Build a clean, fast, modern movie streaming app with:

- Legal HLS video streaming
- Normal email authentication
- Continue with Google
- Netflix-style player controls
- Pre-roll and mid-roll ads
- Watch history
- Continue Watching
- Favorites / My List
- Movie categories
- Search
- Admin-ready backend data structure

## Primary users

```text
Viewer
- Watches movies/shows
- Uses login and Google sign-in
- Saves favorites
- Resumes playback

Admin
- Adds movies
- Manages categories
- Adds posters/banners
- Configures video URLs
- Configures ad breaks
```

## Main app sections

```text
Splash
Login / Signup
Home
Search
Movie Detail
Player
My List
Profile
```

## Core technology direction

```text
Android: Kotlin + Jetpack Compose
Architecture: MVVM + Repository Pattern
Streaming Player: Media3 ExoPlayer
Ads: Google IMA SDK
Auth: Supabase Auth
Database: Supabase PostgreSQL
Video Delivery: Bunny Stream or Mux
```

## Content model

Tio-Flix should not directly host illegal or unauthorized video content. Every video must be owned, licensed, or legally available for distribution.

## Version 1 focus

The first production version should focus on:

- Stable login
- Stable Google sign-in
- Movie listing
- Movie detail page
- HLS playback
- Basic player controls
- Pre-roll and mid-roll ads
- Watch history
- Favorites
