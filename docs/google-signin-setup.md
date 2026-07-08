# Google Sign-In Setup

This document explains how to add Continue with Google in the Android Kotlin app.

## Recommended approach

Use Google sign-in through the auth provider instead of building your own OAuth backend from scratch.

Recommended:

```text
Android App
↓
Google Sign-In
↓
Supabase Auth OAuth
↓
Supabase user session
↓
App profile row
```

## Google Cloud setup

Create OAuth credentials in Google Cloud Console.

Required clients:

1. Android OAuth client
   - Package name: your Android app package
   - SHA-1 fingerprint: debug and release signing keys

2. Web OAuth client
   - Used by Supabase/Firebase as OAuth backend client

## Android SHA-1

For debug SHA-1:

```bash
./gradlew signingReport
```

Add the SHA-1 fingerprint to Google Cloud OAuth credentials.

## Supabase provider setup

In Supabase dashboard:

```text
Authentication
↓
Providers
↓
Google
↓
Enable Google provider
↓
Add Google Web Client ID
↓
Add Google Web Client Secret
```

Also configure redirect URL in Google Cloud Console.

Supabase callback URL usually looks like:

```text
https://YOUR_PROJECT_REF.supabase.co/auth/v1/callback
```

## App flow

```text
User taps Continue with Google
↓
Google account picker opens
↓
User selects account
↓
Auth provider verifies Google token
↓
Supabase returns session
↓
App saves/updates user profile
↓
Navigate to Home screen
```

## Data to store in profile table

From Google account, save only useful app profile fields:

```text
id: Supabase auth user id
email: Google email
full_name: Google display name
avatar_url: Google profile picture
provider: google
```

## Do not store

Do not store:

- Google password
- Raw OAuth tokens unless absolutely needed
- Service role key inside Android app
- Sensitive private profile data unnecessarily

## Error handling

Handle these cases:

```text
User cancels Google sign-in
Network error
Google account has no email
Supabase provider not configured
Invalid SHA-1 fingerprint
Release build SHA-1 missing
```

## Common issue

If Google login works in debug but fails in release, release SHA-1 is probably missing in Google Cloud OAuth credentials.
