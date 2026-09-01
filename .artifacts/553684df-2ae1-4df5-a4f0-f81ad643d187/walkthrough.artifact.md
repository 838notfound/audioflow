# Walkthrough - Material 3 Expressive Floating Tabs

I have completely rebuilt the navigation system to use a high-performance floating "Expressive" tab bar. This design is inspired by modern Google apps like Photos and is engineered to eliminate the delay seen in standard bottom navigation.

## Changes Made

### Custom Floating Navigation
- **`ExpressiveNavigationBar.kt`**: Created a custom, pill-shaped navigation overlay.
  - **Custom Animation**: Uses `animateDpAsState` with a bouncy spring for the item expansion, providing a very responsive and "native" feel.
  - **Zero-Lag Interaction**: Removed the default ripple and indicator logic which can cause perceived lag, replacing them with immediate animated background transitions.
  - **State Isolation**: Badge counts and the current destination are collected directly within the navigation bar, preventing the entire app shell from recomposing when values change.

### Layout Refactoring
- **`MainScreen.kt`**: Removed the standard `Scaffold` bottom bar.
  - The content now fills the screen, and the `ExpressiveNavigationBar` is placed as a floating overlay at the bottom center.
  - Removed all "heavy" state collection (item lists) from the main screen, ensuring that only the `currentDestination` triggers a screen swap.

### Polish
- **Content Padding**: Adjusted bottom padding in `QueueScreen`, `LibraryScreen`, and `SettingsScreen` (to `120.dp`) to ensure that the floating navigation bar does not cover any content at the bottom of the lists.

## Verification Results

### Automated Tests
- Build verification (`:app:assembleDebug`) - **SUCCESS**

### Manual Verification
- **Tab Switching**: Tapping a tab should result in an immediate visual transition and screen swap.
- **Visuals**: The navigation bar should float above the content with a subtle shadow and glass-morphism effect (semi-transparent surface).
- **Responsive Badges**: Badges should update in real-time as downloads progress without affecting the smoothness of tab switching.
