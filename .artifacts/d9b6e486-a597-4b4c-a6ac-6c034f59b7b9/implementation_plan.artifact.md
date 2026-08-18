# Request Notification Permission

The app currently requests location permission but fails to request notification permission on Android 13+ (API 33+). This is needed because the app sends notifications during the trip planning process.

## User Review Required

> [!IMPORTANT]
> The app will now request both Location and Notification permissions when the search form is opened or when accessing features that require location (like the map). On Android 13+, these will appear in the system permission dialog.

## Proposed Changes

### feature:home

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/ramble/feature/home/HomeScreen.kt)
- Update `locationPermissionLauncher` to specifically check for location permissions in its callback.
- Update the `LaunchedEffect` that triggers the permission request to include `POST_NOTIFICATIONS` on API 33+.

#### [MODIFY] [PlannerScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/ramble/feature/home/PlannerScreen.kt)
- Add `android.os.Build` import.
- Update `permissionLauncher` to specifically check for location permissions in its callback.
- Update the map style initialization to include `POST_NOTIFICATIONS` in the permission request if needed.

### feature:trip

#### [MODIFY] [TripWorkspaceScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/trip/src/main/java/com/ramble/feature/trip/TripWorkspaceScreen.kt)
- Add `android.os.Build` import.
- Update `permissionLauncher` to specifically check for location permissions in its callback.
- Update the map initialization to include `POST_NOTIFICATIONS` in the permission request if needed.

## Verification Plan

### Automated Tests
- N/A (UI permission flow is hard to test automatically without complex Robolectric/Espresso setup).

### Manual Verification
- Deploy the app to an Android 13+ device or emulator.
- Open the search form on the Home screen.
- Verify that both location and notification permission dialogs appear (or one dialog with two pages).
- Navigate to the Planner or Trip Workspace and verify that location is still correctly enabled even if notification permission is denied (and vice-versa).
