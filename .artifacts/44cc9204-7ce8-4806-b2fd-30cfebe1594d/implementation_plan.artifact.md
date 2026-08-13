# 16 KB Page Size Compatibility Fix

This plan addresses the "LOAD segments not aligned at 16 KB boundaries" issue by upgrading native dependencies and configuring the Android Gradle Plugin to use legacy packaging (compression) for native libraries.

## Proposed Changes

### [Component Name] Gradle Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/gradle/libs.versions.toml)
- Update MapLibre SDK version from `11.0.0` to `13.4.1`. Newer versions are more likely to be pre-compiled with 16 KB alignment.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/app/build.gradle.kts)
- Add the `packaging` block to force native library compression using `useLegacyPackaging = true`. This ensures that even if a third-party library has 4 KB aligned ELF segments, the Android package manager will extract and align them correctly at install time on a 16 KB device.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the project still builds correctly.
- (Manual) Verify the APK alignment if `zipalign` is available: `zipalign -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk`.

### Manual Verification
- The user can verify that the notification in Android Studio or Play Console disappears after these changes.
