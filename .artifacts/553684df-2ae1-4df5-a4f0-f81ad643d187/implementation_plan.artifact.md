# Rebuild Navigation: Material 3 Expressive Floating Tabs

The previous optimization did not fully resolve the jank. This plan replaces the standard `Scaffold` bottom bar with a custom "Expressive" floating navigation bar. This bar will be a floating pill-shaped overlay, similar to the new Google Photos, designed for high performance and immediate feedback.

## User Review Required

> [!IMPORTANT]
> - The standard `Scaffold(bottomBar = ...)` will be abandoned in favor of a `Box` layout with a floating overlay.
> - Navigation state collection will be pushed down into the navigation component itself to prevent the parent `MainScreen` from recomposing when badge counts or item lists change.

## Proposed Changes

### [UI Components]

#### [NEW] [ExpressiveNavigationBar.kt](file:///C:/Users/Parth/StudioProjects/audioflow/app/src/main/java/com/example/ui/components/ExpressiveNavigationBar.kt)
- Create a custom floating bar using a `Surface` with `CircleShape` (pill).
- Use `Row` and individual `ExpressiveNavItem` composables.
- Implement a custom selection indicator (e.g., a colored pill background) that reacts immediately.
- Collect badge states *inside* this component to isolate recomposition.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Parth/StudioProjects/audioflow/app/src/main/java/com/example/ui/screens/MainScreen.kt)
- Remove `Scaffold`'s `bottomBar` parameter.
- Wrap the content and the new `ExpressiveNavigationBar` in a `Box`.
- Remove badge state collection from `MainScreen`.
- Keep only the `currentDestination` state for screen switching.

### [Data/ViewModel Layer]

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/Parth/StudioProjects/audioflow/app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt)
- Ensure badge count flows are as light as possible (already implemented, will double-check).

---

## Verification Plan

### Automated Tests
- Build verification (`:app:assembleDebug`).

### Manual Verification
- Verify the new "floating" look.
- Confirm tab switching is "native-speed" (no delay, no grey hang).
- Ensure badges update correctly without causing jank in the navigation bar's own animations.
