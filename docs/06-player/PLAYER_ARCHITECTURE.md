# Player Architecture

This document defines the Netflix/SonyLIV/Prime-style player plan for Tio-Flix.

## Recommended player

```text
Android Media3 ExoPlayer
```

## Player goals

The player should support:

- HLS `.m3u8` playback
- Fullscreen immersive mode
- Play and pause
- Seekbar
- 10-second forward/backward
- Auto-hide controls
- Resume playback
- Pre-roll ads
- Mid-roll ads
- Subtitles
- Audio tracks
- Quality selector
- Next episode
- Skip intro
- Lock controls

## Player screen layout

```text
Top controls
- Back button
- Movie title

Center controls
- Play / Pause
- Back 10 sec
- Forward 10 sec

Bottom controls
- Current time
- Seekbar
- Total duration
- Subtitle button
- Audio button
- Quality button
- Fullscreen/lock option
```

## Playback flow

```text
PlayerScreen opens
↓
Load movie details
↓
Load last watch progress
↓
Prepare ExoPlayer with HLS URL
↓
Play pre-roll ad if configured
↓
Start movie playback
↓
Save progress periodically
↓
Trigger mid-roll ad at configured break
↓
Resume movie after ad
```

## Resume playback

Save progress to `watch_history` table.

Recommended save timing:

```text
Every 10–15 seconds
On pause
On app background
On player release
```

## Forward/backward controls

```kotlin
fun seekBack10(player: ExoPlayer) {
    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
}

fun seekForward10(player: ExoPlayer) {
    player.seekTo((player.currentPosition + 10_000).coerceAtMost(player.duration))
}
```

## Player lifecycle

Release player when screen is destroyed.

```kotlin
DisposableEffect(Unit) {
    onDispose {
        player.release()
    }
}
```

## Ads integration

Ads should be handled by Google IMA SDK with Media3 IMA extension.

Ad positions:

```text
0 seconds = pre-roll
600 seconds = mid-roll after 10 minutes
1200 seconds = mid-roll after 20 minutes
```

## Player ViewModel responsibilities

```text
Load movie
Load watch progress
Load ad breaks
Track playback position
Save progress
Handle player UI state
Handle errors
```

## Player UI state example

```kotlin
data class PlayerUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val showControls: Boolean = true,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val errorMessage: String? = null
)
```

## Future upgrades

- Double-tap seek gestures
- Brightness gesture
- Volume gesture
- Playback speed
- Picture-in-picture
- Cast support
- DRM
- Offline download
