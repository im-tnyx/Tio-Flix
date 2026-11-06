# Android Auth Implementation Notes

This document gives a Kotlin/Jetpack Compose implementation plan for normal auth and Continue with Google.

## Recommended app structure

```text
app/
├── data/
│   ├── auth/
│   │   ├── AuthRepository.kt
│   │   ├── AuthState.kt
│   │   └── AuthResult.kt
│   ├── profile/
│   │   └── ProfileRepository.kt
│   └── movie/
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   ├── SignupScreen.kt
│   │   └── ForgotPasswordScreen.kt
│   └── home/
└── navigation/
```

## Auth state

```kotlin
sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val userId: String) : AuthState
}
```

## Auth repository interface

Keep UI independent from the auth provider.

```kotlin
interface AuthRepository {
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun continueWithGoogle(): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun getAuthState(): AuthState
}
```

## Login screen actions

```text
Email login button → signInWithEmail(email, password)
Signup button → navigate to SignupScreen
Forgot password → forgotPassword(email)
Continue with Google → continueWithGoogle()
```

## Login UI states

The screen should handle:

```text
Idle
Loading
Success
Error(message)
```

## Google login result flow

```text
Google sign-in success
↓
Supabase/Firebase session created
↓
Fetch current user id
↓
Upsert profile row
↓
Navigate Home
```

## Profile upsert logic

After successful login, always create/update profile.

Pseudo logic:

```kotlin
suspend fun syncProfileAfterLogin() {
    val user = auth.currentUser() ?: return

    profileRepository.upsertProfile(
        id = user.id,
        email = user.email,
        fullName = user.name,
        avatarUrl = user.avatarUrl,
        provider = user.provider
    )
}
```

## Session check on app start

```text
SplashScreen
↓
Check saved session
↓
If logged in → Home
↓
If logged out → Login
```

## Secure storage

Use the auth SDK session handling. Do not manually store passwords or raw tokens in SharedPreferences.

## Compose navigation example

```kotlin
NavHost(
    navController = navController,
    startDestination = if (isLoggedIn) "home" else "login"
) {
    composable("login") { LoginScreen() }
    composable("signup") { SignupScreen() }
    composable("forgot-password") { ForgotPasswordScreen() }
    composable("home") { HomeScreen() }
}
```

## Recommended validation

Email:

```text
Not empty
Valid email format
```

Password:

```text
Minimum 8 characters
Show/hide password option
```

## User experience

- Show loading when signing in.
- Disable buttons during loading.
- Show clear error message.
- Keep Continue with Google visible on login and signup screens.
- After login, directly navigate to Home.
- On logout, clear navigation back stack and go to Login.

## Testing checklist

```text
Email signup works
Email login works
Wrong password shows error
Forgot password sends email
Google login works in debug build
Google login works in release build
Logout clears session
App opens Home if session exists
Profile row is created after login
Watch history saves against correct user id
```

## Important production notes

- Add both debug and release SHA-1 keys for Google login.
- Keep API keys in secure config files.
- Never commit secrets to GitHub.
- Use ProGuard/R8 rules if required by SDKs.
- Test login on a real device, not only emulator.

## Implemented password recovery flow

`SendPasswordResetUseCase` requests an email redirect to
`tioflix://auth/reset-password`. Add this exact URL to the Supabase Auth redirect
allow-list before testing email delivery. This repository change does not update
hosted Auth configuration.

The current Android Auth configuration uses the SDK's implicit flow. Recovery
links must contain `type=recovery`, a bearer access token, a refresh token, and a
positive `expires_in`. PKCE code-only recovery links are rejected; changing the
Auth flow requires adapting recovery handling too.

`MainActivity` intercepts recovery links on both cold launch and `onNewIntent`.
The activity-scoped `ResetPasswordViewModel` opens the reset destination in the
single NavHost. Other auth callbacks continue through the existing SDK handler.
Consumed recovery URI data is removed from the Activity intent.

`SupabasePasswordRecoveryRepository` validates the access token with Supabase
before enabling password entry. A separate Auth client uses in-memory session
and code-verifier managers, with automatic storage load/save disabled. Recovery
credentials are never navigation arguments, saved-state fields, or application
logs. Existing login sessions are untouched by invalid or cancelled recovery.

The form requires at least eight characters and matching confirmation. A
successful `updateUser` is followed by local session cleanup and navigation to
Login with a success message. Local cleanup does not revoke sessions on other
devices. If cleanup fails after the password changed, Continue retries cleanup
without submitting the password update again.

Invalid/expired links show a recovery error and a route back to Login to request
a new email. Rotation retains the in-memory form; after process death, reopen a
reset email link. Passwords and recovery sessions are not restored from disk.

Local verification commands:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

Regression tests cover callback parsing, server validation gating, invalid links,
password validation, duplicate submission, cancellation, warm-link replacement,
and cleanup retry. Real email delivery, redirect allow-list configuration, and
expired-link behavior against Supabase still require device acceptance testing.
Unit tests do not prove those live flows.

### API35 emulator smoke verification - 2026-10-13

The debug APK was installed on `Pixel_9_API35` (`emulator-5554`). Verified:

- Cold launch opens Login with empty fields and no route placeholders.
- Cold and warm launches of a reset URL without credentials show the recovery error.
- Recovery screen Back and Android system Back return to Login.
- A synthetic invalid-token link shows validation, then the recovery error without enabling password entry.
- Cancelling pending validation returns to Login; Forgot password remains accessible.
- A simulated `otp_expired` callback shows the recovery error.
- No `AndroidRuntime` crash entry was observed for the tested app process.

Smoke testing found that normal Login navigation used the route pattern and
displayed literal `{email}` and `{message}` values. Both callers now use the
existing `Destinations.login()` builder; the rebuilt APK was retested.

These checks do not verify delivery of a real reset email, hosted redirect
configuration, a valid recovery session/password update, or physical-device
behavior. Those acceptance checks remain pending. Compile, the 20 unit tests,
and debug APK assembly passed after the navigation fix.

### API35 instrumented recovery UI tests - 2026-10-15

`ResetPasswordRouteTest` ran on `Pixel_9_API35` (`emulator-5554`) with a fake
`PasswordRecoveryRepository`: 6/6 passed. The tests cover validation gating,
short and mismatched password errors, successful update-to-Login callback,
cleanup retry without another password update, invalid-link Back, and Android
system Back during pending validation. The app and test APKs were installed only
on that emulator; `:app:assembleDebugAndroidTest` and `:app:testDebugUnitTest`
passed locally.

These instrumented tests exercise the Compose route and real ViewModel with a
fake repository. They do not exercise the full NavHost, Supabase email delivery,
hosted redirect configuration, live credential validation/password update, or
login with a newly changed password. The valid-link end-to-end flow remains
pending a test-account email and user opening its reset link on the emulator.
