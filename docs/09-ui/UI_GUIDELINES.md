# UI Guidelines

This document defines the visual direction for Tio-Flix across Android Mobile, Android Tablet, Android TV, and Fire TV.

## Design goal

Tio-Flix should feel like a premium OTT app:

- Dark theme first
- Large movie banners
- Poster rows
- Smooth scrolling
- Clean typography
- Minimal distractions
- Strong focus on content
- Remote-friendly TV experience
- Touch-friendly mobile experience

## Architecture rule for UI

All feature UI should follow:

```text
Route + Screen + ViewModel + UiState + Action
```

Rules:

- Screens are dumb UI.
- Screens render state and emit actions only.
- Routes connect ViewModel state to Screen.
- Navigation is handled through route-level callbacks and the single NavHost.
- No Supabase, repository, player setup, or ad setup inside dumb Screens.

## Theme

Recommended default:

```text
Dark theme
Black/dark background
High contrast text
Accent color for primary actions
```

## Platform UI strategy

```text
Mobile: touch-first UI
Tablet: responsive touch UI with larger layout opportunities
Android TV: D-pad and focus-first 10-foot UI
Fire TV: D-pad and remote-first 10-foot UI
```

Mobile and TV can reuse domain/data logic, but UI layouts should adapt.

## Main screens

```text
SplashScreen
LoginScreen
SignupScreen
HomeScreen
SearchScreen
MovieDetailScreen
PlayerScreen
MyListScreen
ProfileScreen
```

## Home screen layout

### Mobile / Tablet

```text
Top app bar
↓
Featured hero banner
↓
Continue Watching row
↓
Trending row
↓
Category rows
↓
Recommended row
```

### Android TV / Fire TV

```text
Left or top navigation rail
↓
Large hero banner
↓
Focusable horizontal rows
↓
Large poster cards
↓
Clear focused card state
```

TV home rows must support D-pad left/right/up/down navigation.

## Movie card

Movie card should include:

```text
Poster image
Rounded corners
Optional progress bar for continue watching
Title only when needed
```

Mobile rules:

- Cards should be thumb-friendly.
- Text should not overcrowd posters.
- Touch targets should be easy to tap.

TV / Fire TV rules:

- Cards must be focusable.
- Focused card should visibly scale, glow, outline, or elevate.
- Poster/title should be readable from a sofa distance.
- Avoid tiny icons on TV cards.

## Movie detail screen

Should include:

```text
Large banner
Poster/title
Description
Release year
Duration
Language
Age rating
Watch Now button
Add to My List button
```

TV detail screen rules:

- Primary action should get initial focus.
- Description should be readable at TV distance.
- Buttons should be large and clearly focused.
- Back behavior should be predictable.

## Player UI

Player should be distraction-free.

Controls:

```text
Back
Title
Play/Pause
Back 10 seconds
Forward 10 seconds
Seekbar
Subtitle
Audio
Quality
Lock
```

Mobile player rules:

- Controls should be large enough for thumb use.
- Touch gestures can be added later.
- Avoid placing important buttons near system gesture edges.

TV / Fire TV player rules:

- Controls must be focusable.
- Focus state must be visually obvious.
- Seekbar must work with D-pad left/right.
- Select button should activate focused control.
- Play/pause remote key should work.
- Back should hide controls first, then exit if controls are hidden.
- Do not rely on touch-only gestures.

## Buttons

Primary button:

```text
Watch Now
Continue Watching
Login
Signup
```

Secondary button:

```text
Add to My List
Trailer
Cancel
```

Button rules:

- Primary action should be visually strongest.
- Disabled state must be clear.
- Loading state should prevent duplicate actions.
- TV buttons must show clear focus state.

## Loading states

Use skeleton loading/shimmer for:

```text
Home rows
Movie detail banner
Poster cards
```

Use normal loader for:

```text
Login
Signup
Player preparation
Ad loading
```

TV loading rules:

- Avoid tiny spinners only.
- Keep layout stable to prevent focus jumps.

## Empty states

Examples:

```text
No movies found
No favorites yet
No watch history yet
```

Empty states should show one clear next action when possible.

## Error states

Show clear messages:

```text
Something went wrong
Check your internet connection
Video is not available
Ad failed to load, continuing playback
```

Error rules:

- Do not expose internal error traces to users.
- Provide retry action when useful.
- On TV/Fire TV, retry button must receive focus.

## Responsive layout rules

```text
Phone: compact vertical layout
Tablet: larger banners and wider rows
TV/Fire TV: 10-foot layout with large spacing and focus states
```

Use platform/window size to adapt layout instead of duplicating business logic.

## Accessibility rules

- Provide content descriptions where useful.
- Do not over-describe decorative posters.
- Maintain high contrast text.
- Support large text where practical.
- TV focus order must be predictable.
- Avoid color-only state communication.

## UX rules

- Keep important actions visible.
- Do not overload movie cards with too much text.
- Use smooth transitions between detail and player.
- Login should be simple and fast.
- Continue with Google should be visible on login and signup screens.
- Keep player controls calm and predictable.
- Respect platform expectations: touch on mobile, remote on TV/Fire TV.
