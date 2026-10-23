# Platform Support

Tio-Flix is planned as a cross-device Android OTT app.

## Supported platforms

```text
Android Mobile: Supported
Android Tablet: Supported
Android TV / Google TV: Supported
Amazon Fire TV / Fire TV Stick: Supported
```

## Future / optional platforms

```text
Chromecast with Google TV: Future
Web: Future
Wear OS: Not planned
Android Auto: Not planned
```

## Shared architecture

The same backend and streaming system should work for all supported platforms.

```text
Android Mobile / Tablet / TV / Fire TV
↓
Supabase Auth
↓
Supabase PostgreSQL
↓
Bunny Stream / Mux HLS video
↓
Media3 ExoPlayer
↓
Google IMA Ads
```

## Mobile requirements

Mobile app should support:

- Touch navigation
- Portrait and landscape screens
- Fullscreen player
- Gesture controls
- Bottom navigation
- Login and Google sign-in
- Movie detail screen
- Continue Watching
- Favorites

## Android TV requirements

The manifest references `app/src/main/res/drawable/tv_banner.xml` as the default
launcher banner. It contains outlined Tio-Flix lettering on the shared dark/red
palette, with a 320 x 180 viewport (320 x 180 pixels at xhdpi). This supplies a
launcher resource; it does not establish TV remote, playback, or release acceptance.

Local validation on 2026-10-13 passed `:app:compileDebugKotlin`,
`:app:testDebugUnitTest` (20 tests), `:app:assembleDebug`, and `:app:lintDebug`
(0 errors, 59 warnings). Lint fixes also include a function-scoped Media3
`UnstableApi` opt-in and a Glide 4.16.0 dependency constraint for IMA's legacy
Glide dependency. No notification permission or lint suppression was added.
The Android CI workflow now includes `:app:lintDebug`; hosted CI has not run
for these local changes.

The rebuilt APK passed Login, invalid recovery-link, and Back-to-Login smoke
checks on `Pixel_9_API35`. A valid recovery email/password update, actual TV
launcher rendering, TV remote behavior, and IMA ad playback remain unverified.

Android TV app should support:

- D-pad remote navigation
- Focusable cards and buttons
- 10-foot UI layout
- TV launcher banner
- TV-safe spacing and typography
- Player controls usable by remote
- Back button behavior
- Search with TV keyboard/voice input if available

## Fire TV / Fire TV Stick requirements

Fire TV app should support:

- Fire TV remote navigation
- D-pad focus handling
- Select/play/pause/back buttons
- TV-friendly home rows
- Player controls usable by Fire TV remote
- Amazon Appstore release readiness

## UI strategy

Use shared business logic and separate responsive UI behavior.

```text
Shared:
- AuthRepository
- MovieRepository
- WatchHistoryRepository
- FavoritesRepository
- Player logic
- Supabase data sources

Platform-specific:
- Mobile navigation UI
- TV focus UI
- Fire TV remote behavior
- TV launcher assets
```

## Player strategy

Use Media3 ExoPlayer for all Android platforms.

```text
Mobile: Touch controls + gestures
Android TV: Remote-friendly controls
Fire TV: Remote-friendly controls
```

## Design rule

Mobile UI and TV UI should not be exactly the same. TV screens need larger spacing, bigger cards, clear focus state, and simpler navigation.

## Release targets

```text
Google Play Store: Android mobile/tablet
Google Play Store TV: Android TV / Google TV
Amazon Appstore: Fire TV / Fire TV Stick
```
