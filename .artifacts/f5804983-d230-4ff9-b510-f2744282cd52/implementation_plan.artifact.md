# Redesign Login Screen for Multi-step Dynamic Flow

Redesign the login experience to be more consistent with the Ramble design system, more dynamic with animations, and split into several steps to optimize screen real estate.

## Proposed Changes

### [feature:home]

#### [MODIFY] [LoginScreen.kt](file:///C:/Users/gaeta/AndroidStudioProjects/triplane2/triplane/feature/home/src/main/java/com/ramble/feature/home/LoginScreen.kt)
- Introduce a `LoginStep` enum (`WELCOME`, `EMAIL`, `DETAILS`) to manage the multi-step flow.
- Wrap the main content in a `RambleCard` for a more consistent and layered UI.
- Use `AnimatedContent` for smooth, horizontal transitions between steps.
- **Step 1 (WELCOME)**: Clear branding with logo, slogan, "Continue with Google" primary action, and "Continue with Email" alternative.
- **Step 2 (EMAIL)**: Focused email entry with validation.
- **Step 3 (DETAILS)**: Password entry for existing users; First Name, Last Name, and Password for new users.
- Add a progress indicator (dots) to show the user's progress through the steps.
- Add a "Back" button to allow users to navigate between steps easily.
- Integrate `RambleButton` from `core:designsystem`.
- Improve error handling UI with better positioning and animation.

## Verification Plan

### Automated Tests
- N/A (UI changes mainly, existing auth logic is preserved).

### Manual Verification
1.  Launch the app and navigate to the Login screen.
2.  Verify the initial "Welcome" step looks consistent with the Ramble brand.
3.  Click "Continue with Google" and verify the flow still works.
4.  Click "Continue with Email" and verify the smooth transition to the Email step.
5.  Enter a valid email and proceed to the "Details" step.
6.  Verify that the "Back" button correctly navigates back to the previous step.
7.  Complete the login/signup process and verify successful authentication.
