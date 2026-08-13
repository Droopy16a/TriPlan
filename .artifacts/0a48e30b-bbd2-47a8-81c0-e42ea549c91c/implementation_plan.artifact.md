# City Selection on Planner Page

Implement city selection on the Planner page map. When a city is clicked, the form expands, the destination is filled, and the city is highlighted.

## User Review Required

> [!IMPORTANT]
> - The implementation relies on querying map layers (`place-city`, `place-town`) from the OpenFreeMap Liberty style.
> - To ensure selection works even when labels are hidden by collision, I will add a "hit target" layer that always exists in the map's source data.
> - I will add an `id` field to the `Properties` model in `core:location` to uniquely identify cities (using OSM IDs from the map features).

## Proposed Changes

### core:location

#### [MODIFY] [LocationModels.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/core/location/src/main/java/com/triplane/core/location/LocationModels.kt)
- Add `val osm_id: String? = null` to `Properties` for stable identification.

### feature:home

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/feature/home/src/main/java/com/triplane/feature/home/HomeViewModel.kt)
- Add `selectedCityId: StateFlow<String?>`.
- Add `selectedCityProperties: StateFlow<Properties?>`.
- Add `selectCity(properties: Properties)` function.
- Add `clearSelection()` function.

#### [MODIFY] [PlannerScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane/feature/home/src/main/java/com/triplane/feature/home/PlannerScreen.kt)
- Refactor `SearchBar` to accept an external `destination` state and `onDestinationChange`.
- Add a map click listener to `mapInstance` that:
    - Queries for features in `place-city` or `place-town` layers.
    - Calls `viewModel.selectCity()` with the feature's properties.
- Add a `GeoJsonSource` ("selection-source") and a `SymbolLayer` ("selection-layer") to the map.
- Update the selection source whenever `selectedCityProperties` changes.
- Use a custom Bitmap for the selection highlight (TripLane green + white border).
- Ensure the form expands and `destination` is set when a city is selected.

## Verification Plan

### Automated Tests
- N/A (UI and Map interaction are better verified manually in this context).

### Manual Verification
1. **Selection**: Click on a city label on the map. Verify form expands and destination is filled.
2. **Highlighting**: Verify the selected city has a green background and white border.
3. **Zoom Stability**: Select a city, zoom out until its label disappears, then zoom back in. Verify it is still highlighted and the form is still populated.
4. **Single Selection**: Select a city, then select another. Verify only the new one is highlighted.
5. **Manual Edit**: Manually edit the destination field. Verify the map highlight is cleared if the text no longer matches.
