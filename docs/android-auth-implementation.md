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
