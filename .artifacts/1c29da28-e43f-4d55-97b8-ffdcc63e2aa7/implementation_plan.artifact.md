# Implement Suggestions for Departure and Destination

This plan implements location suggestions for both departure and destination inputs in the search bar, similar to the Google Maps experience. It leverages the existing `LocationService` (Photon API) to fetch autocomplete suggestions as the user types.

## User Review Required

> [!NOTE]
> The implementation uses the free Photon API (OSM-based) instead of Google Places SDK to avoid API key requirements and costs, while providing a similar autocomplete experience.

## Proposed Changes

### feature:home

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/triplane/feature/home/HomeViewModel.kt)
- Add `departureSuggestions` StateFlow.
- Add `updateDepartureSuggestions(query: String)` function with debouncing.
- Update `updateDestinationSuggestions` for consistency.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/triplane/feature/home/HomeScreen.kt)
- Hoist `departureInput` state from `SearchBar` to `HomeScreen`.
- Update `SearchBar` to accept `departure` state and `departureSuggestions`.
- Implement suggestion list for the departure field.
- Ensure the suggestion dropdown appears below the currently focused field.

#### [MODIFY] [PlannerScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/triplane/feature/home/PlannerScreen.kt)
- Hoist `departureInput` state and wire it to the `SearchBar`.
- Pass `departureSuggestions` from the ViewModel to the `SearchBar`.

## Verification Plan

### Automated Tests
- N/A (UI-heavy change, manual verification preferred for animations and autocomplete feel).

### Manual Verification
1. Open the app and expand the search bar.
2. Type in the "Departure" field and verify that suggestions appear.
3. Select a suggestion and verify the field is populated.
4. Type in the "Destination" field and verify that suggestions appear.
5. Select a suggestion and verify the field is populated.
6. Verify that location permission still auto-fills the departure field.
