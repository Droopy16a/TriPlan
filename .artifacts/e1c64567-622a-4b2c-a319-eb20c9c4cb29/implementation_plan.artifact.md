# Apply New App Color Palette

This plan outlines the steps to update the Ramble app's color palette to match the new branding guidelines provided. The new palette consists of two greens, a dark navy, and a light gray.

## User Review Required

> [!IMPORTANT]
> The "Premium Travel Red" branding will be completely replaced by the new Green/Navy palette. This will significantly change the visual identity of the app.

## Proposed Changes

### Core Design System (`:core:designsystem`)

We will update the color definitions and the Material 3 theme to use the new palette.

#### [MODIFY] [Color.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/core/designsystem/src/main/java/com/ramble/core/designsystem/theme/Color.kt)
- Define new color constants:
    - `RambleGreen`: `#16C47F` (Primary)
    - `RambleGreenDark`: `#0FA36B` (Secondary)
    - `RambleNavy`: `#0F172A` (Background/Surface for dark theme)
    - `RambleLight`: `#F2F4F7` (Background for light theme)
- Remove or deprecate old branding colors like `TravelRed`, `WarmCoral`, etc.

#### [MODIFY] [Theme.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/core/designsystem/src/main/java/com/ramble/core/designsystem/theme/Theme.kt)
- Update `LightColors` and `DarkColors` color schemes to use the new constants.
- **Light Color Scheme**:
    - `primary`: `RambleGreen`
    - `secondary`: `RambleGreenDark`
    - `background`: `RambleLight`
    - `onBackground`: `RambleNavy`
    - `surface`: `Color.White`
    - `onSurface`: `RambleNavy`
- **Dark Color Scheme**:
    - `primary`: `RambleGreen`
    - `background`: `RambleNavy`
    - `onBackground`: `RambleLight`
    - `surface`: `RambleNavy` (possibly a slightly lighter shade for depth)
    - `onSurface`: `RambleLight`

## Verification Plan

### Automated Tests
- Run a build to ensure no broken references if old colors were removed.
- Run `gradle :app:assembleDebug` to verify compilation.

### Manual Verification
- Deploy the app to an emulator/device.
- Verify the new colors are applied across different screens (Home, Trip planning, etc.).
- Check both Light and Dark modes.
