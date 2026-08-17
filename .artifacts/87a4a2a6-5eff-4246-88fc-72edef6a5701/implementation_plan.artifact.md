# Fix Owner View for Expenses and Members Tabs

The current implementation treats all users as owners in the `TripWorkspaceScreen`. Specifically, the "Members" tab always shows the first member (usually the owner) as the primary user ("Me"), and the "Expenses" tab defaults to the owner as the payer. Additionally, administrative actions like "Edit Trip" and "Delete Trip" are visible to everyone.

## User Review Required

> [!IMPORTANT]
> - "Edit Trip" and "Delete Trip" will now be restricted to the trip owner.
> - Members will have a "Leave Trip" option instead of "Delete Trip".
> - The "Expenses" and "Members" tabs will now correctly identify the current user and show their personal balance and set them as the default payer for new expenses.

## Proposed Changes

### [Component Name] :core:ai

#### [MODIFY] [TripRepository.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/core/ai/src/main/java/com/ramble/core/ai/TripRepository.kt)
- Update `save` method to avoid overwriting `userId` if it's already present (prevent participants from becoming owners).
- Update `membersForTrip` to ensure the current user is included and prioritized in the list.
- Update `buildDisplayMembers` to move the current user to the top of the list when `includeCurrentUser` is true.
- Add `leaveTrip` method to allow members to remove themselves from a trip.

### [Component Name] :feature:trip

#### [MODIFY] [TripWorkspaceScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/trip/src/main/java/com/ramble/feature/trip/TripWorkspaceScreen.kt)
- Identify `currentUserId` and determine `isOwner`.
- Conditionally show "Edit Trip" and "Delete Trip" menu items based on `isOwner`.
- Add "Leave Trip" menu item for non-owners.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device is preferred for UI logic).

### Manual Verification
1. Log in as User A and create a trip.
2. Invite User B to the trip.
3. Log in as User B and join the trip.
4. Verify that User B sees themselves as "Me" (or at the top) in the Members tab.
5. Verify that when User B adds an expense, they are the default payer.
6. Verify that User B does NOT see "Edit Trip" and sees "Leave Trip" instead of "Delete Trip".
7. Verify that User A (the owner) still sees "Edit Trip" and "Delete Trip".
