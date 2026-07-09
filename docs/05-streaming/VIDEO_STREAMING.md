# Video Streaming

This document explains the recommended video streaming setup for Tio-Flix.

## Recommended approach

Use a dedicated video streaming provider instead of storing large movie files directly in the app backend.

Recommended providers:

```text
Bunny Stream
Mux
AWS S3 + CloudFront + MediaConvert
```

For starting Tio-Flix, recommended option:

```text
Bunny Stream
```

## Why HLS

Use HLS `.m3u8` instead of direct MP4 for OTT playback.

Benefits:

- Adaptive quality
- Better seeking
- Less buffering
- CDN friendly
- Works well with ExoPlayer
- Supports long videos better than direct MP4

## Video upload flow

```text
Admin uploads movie to Bunny Stream / Mux
↓
Provider transcodes video
↓
Provider generates HLS .m3u8 URL
↓
Admin saves video URL in Supabase movies table
↓
Android app fetches movie data
↓
Media3 ExoPlayer plays HLS stream
```

## Movie table video fields

```text
video_url: HLS .m3u8 URL
trailer_url: optional trailer URL
poster_url: image poster URL
banner_url: large banner image URL
```

## Android playback

Media3 ExoPlayer can play HLS streams.

```kotlin
val player = ExoPlayer.Builder(context).build()

player.setMediaItem(
    MediaItem.fromUri(movie.videoUrl)
)

player.prepare()
player.playWhenReady = true
```

## Seeking

Backward/forward seek is handled by ExoPlayer.

```kotlin
player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
player.seekTo((player.currentPosition + 10_000).coerceAtMost(player.duration))
```

## Recommended quality strategy

Start with automatic adaptive quality. Add manual quality selector later.

Version 1:

```text
Auto quality only
```

Future:

```text
Auto
360p
480p
720p
1080p
```

## CDN

Video should be delivered through CDN. Bunny Stream and Mux include CDN-style delivery.

## Do not do this

Avoid:

- Keeping large movie files inside Android app assets
- Serving movies from normal shared hosting
- Using direct MP4 for long movies in production
- Exposing private provider API keys in Android app
- Using unauthorized third-party movie links

## Future production upgrades

- Signed playback URLs
- DRM
- Geo restrictions
- Download protection
- Offline download for licensed content
- Advanced analytics
