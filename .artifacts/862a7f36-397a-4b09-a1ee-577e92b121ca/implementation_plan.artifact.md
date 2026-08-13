# Add slide-out transition when leaving Trip Workspace

The goal is to implement a "slide to the right" transition when the user clicks the back arrow in the trip page, returning to the home screen.

## User Review Required

> [!IMPORTANT]
> The transition will be implemented in `MainActivity` using Jetpack Compose's `AnimatedVisibility` to coordinate the exit of the trip screen and the entrance of the home screen.

## Proposed Changes

### [Component Name] :app

#### [MODIFY] [MainActivity.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/app/src/main/java/com/example/triplane/MainActivity.kt)
- Wrap the `TripWorkspaceScreen` and `HomeScreen` in `AnimatedVisibility` components.
- Configure `slideOutHorizontally` for the trip screen to slide it to the right when dismissed.
- Configure `slideInHorizontally` for the home screen to slide it in from the left when returning.
- Ensure the "expanding" state transition remains seamless.

## Verification Plan

### Manual Verification
- Deploy the app to a device or emulator.
- Navigate from the Home screen to a Trip (click on a trip card).
- Verify the expansion animation still works correctly.
- Click the back arrow in the Trip Workspace.
- Verify that the Trip Workspace slides out to the right and the Home screen slides in from the left.
