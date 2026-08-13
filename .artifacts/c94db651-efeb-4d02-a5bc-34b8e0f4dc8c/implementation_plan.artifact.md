# Dynamic SearchBar Expansion on Home Screen

Enable the "What's Next" search bar to dynamically expand when the user over-scrolls at the top of the Home Screen, providing a clear interaction for starting a search.

## User Review Required

> [!IMPORTANT]
> The expansion is triggered by pulling down (scrolling up) when already at the top of the list. We need to decide how "expanded" it should get and if it should trigger an actual text input focus.

## Proposed Changes

### feature:home

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/feature/home/src/main/java/com/triplane/feature/home/HomeScreen.kt)

- Add a `searchBarExpansion` state (float from 0 to 1).
- Enhance `NestedScrollConnection` to update `searchBarExpansion` when over-scrolling at the top.
- Modify `SearchBar` to accept an `expansion` parameter and adjust its height/padding/shadow accordingly.
- Integrate the expansion state into the `HomeScreen` layout.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to the Home Screen.
- Scroll to the very top.
- Pull down (scroll up) and verify that the SearchBar expands smoothly.
- Release the scroll and verify it snaps back or stays expanded if it reaches a threshold (or just snaps back if it's purely for visual feedback before opening a search UI).
