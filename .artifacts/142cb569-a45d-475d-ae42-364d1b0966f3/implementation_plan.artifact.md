# Fix App Crash during Sign-in (Ktor 3 Compatibility)

The app crashes when instantiating `HomeViewModel` because the Google Generative AI SDK (Gemini) version 0.9.0 is incompatible with Ktor 3.x. The SDK was compiled against Ktor 2.x where `HttpTimeout` was a class/companion object, whereas in Ktor 3.x it is a top-level property, leading to a `java.lang.NoClassDefFoundError`.

## User Review Required

> [!IMPORTANT]
> This fix requires downgrading **Ktor** from `3.0.1` to `2.3.12` and **Supabase** from `3.1.4` to `2.6.1` to ensure compatibility across all dependencies.
>
> While Supabase 3.x is newer, it requires Ktor 3.x, which is currently incompatible with the Gemini SDK. Downgrading both ensures a stable runtime environment.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/gradle/libs.versions.toml)
- Downgrade `ktor` version to `2.3.12`.
- Downgrade `supabase` version to `2.6.1`.
- Add `ktor-client-serialization` to libraries if needed (though `ktor-serialization-kotlinx-json` is usually enough in 2.x too).

#### [MODIFY] [core/ai/build.gradle.kts](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/core/ai/build.gradle.kts)
- Ensure test dependencies use the correct Ktor version from the BOM or version catalog.

### Authentication Layer

#### [MODIFY] [AuthRepository.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/core/auth/src/main/java/com/triplane/core/auth/AuthRepository.kt)
- Update Supabase Auth imports and status handling if there are minor API differences between Supabase 2.x and 3.x.

## Verification Plan

### Automated Tests
- Run Gradle sync to ensure all dependencies resolve correctly.
- Run unit tests in `:core:ai` and `:core:auth` to verify basic functionality.
  - `gradlew :core:ai:test`
  - `gradlew :core:auth:test`

### Manual Verification
- Deploy the app to the device.
- Perform sign-in.
- Verify that `HomeViewModel` is instantiated without crashing and the Home screen appears.
