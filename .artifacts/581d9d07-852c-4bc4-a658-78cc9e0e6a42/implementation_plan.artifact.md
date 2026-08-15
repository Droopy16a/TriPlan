# Fix Plan Trip Form Scroll Issues

The user reported two issues when interacting with the Plan Trip form:
1. **Scroll Persistence:** The form remembers its scroll position when reopened.
2. **Form Hiding:** The search bar (collapsed form) hides when the form is closed if the user had scrolled down within the form.

## Proposed Changes

### [feature:home](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/triplane/feature/home)

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/triplane/feature/home/HomeScreen.kt)

- **Reset `isScrollingDown`:** In the `LaunchedEffect(isSearchFormExpanded)`, reset `isScrollingDown` to `false` when the form is collapsed. This prevents the search bar from being hidden immediately after closing the form.
- **Form Scroll Reset:** In the `SearchBar` component, ensure the `ScrollState` is reset to 0 whenever the form is expanded. While `AnimatedContent` should theoretically handle this, explicitly resetting it ensures consistent behavior if the component is reused or during transitions.

## Verification Plan

### Manual Verification
1. Open the Plan Trip form.
2. Scroll down to the bottom of the form.
3. Close the form using the "Close" button.
4. Verify that the search bar is visible (not hidden due to the scroll offset).
5. Reopen the Plan Trip form.
6. Verify that the form is scrolled back to the top.
