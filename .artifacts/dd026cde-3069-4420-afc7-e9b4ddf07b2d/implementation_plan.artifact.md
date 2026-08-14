# Replace Edit Trip button with a Burger Menu

The goal is to replace the current "Edit Trip" `OutlinedButton` in the `TripSheetContent` with a burger menu (`IconButton` with `Icons.Default.Menu`) that opens a `DropdownMenu` containing the "Edit Trip" action.

## Proposed Changes

### Trip Feature

#### [MODIFY] [TripWorkspaceScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/trip/src/main/java/com/triplane/feature/trip/TripWorkspaceScreen.kt)

- Add a state variable `showMenu` to track the visibility of the dropdown menu.
- Replace the `OutlinedButton` with a `Box` containing an `IconButton` and a `DropdownMenu`.
- Add `Icons.Default.Menu` to the imports if necessary (or just use `Icons.Default.Menu`).

## Verification Plan

### Manual Verification
- Deploy the app to a device or emulator.
- Navigate to the trip workspace screen.
- Verify that the "Edit Trip" button is gone and replaced by a burger menu icon.
- Tap the burger menu icon and verify that a dropdown menu appears.
- Verify that "Edit Trip" is an option in the dropdown menu.
