# Fix Back Navigation to Prevent Quitting the App

The goal is to intercept the system back button (navbar back button) so that it navigates back to the Home screen instead of quitting the app, unless the user is already on the Home screen.

## Proposed Changes

### [MainActivity](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/app/src/main/java/com/example/triplane/MainActivity.kt)
- Add `BackHandler` to intercept back button when `currentScreen` is not "home".
- Pass an `onBack` callback to `PlannerScreen` to allow it to request navigation back to "home".
- Pass a `isActive` flag to `PlannerScreen` so it can enable/disable its internal `BackHandler`.

### [PlannerScreen](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/triplane/feature/home/PlannerScreen.kt)
- Add `onBack` parameter to `PlannerScreen`.
- Add `isActive` parameter to `PlannerScreen`.
- Implement `BackHandler` inside `PlannerScreen`:
    - If `isSearchFormExpanded` is true, collapse the form.
    - Otherwise, call `onBack()`.
    - Only enable this `BackHandler` if `isActive` is true.

### [TripWorkspaceScreen](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/trip/src/main/java/com/triplane/feature/trip/TripWorkspaceScreen.kt)
- Implement `BackHandler` inside `TripWorkspaceScreen`:
    - If the bottom sheet is expanded, partially expand (collapse) it.
    - Otherwise, call `onBackClick()`.
    - This `BackHandler` will automatically be enabled/disabled as the screen is added/removed from composition via `AnimatedVisibility`.

## Verification Plan

### Manual Verification
1. Launch the app (Home screen).
2. Press back button -> App should quit (expected).
3. Open a Trip (TripWorkspaceScreen).
4. Press back button -> App should go back to Home screen (not quit).
5. Open Trip, expand bottom sheet.
6. Press back button -> Bottom sheet should collapse.
7. Press back button again -> App should go back to Home screen.
8. Switch to Planner tab.
9. Press back button -> App should go back to Home screen.
10. Switch to Planner tab, expand search form.
11. Press back button -> Search form should collapse.
12. Press back button again -> App should go back to Home screen.
