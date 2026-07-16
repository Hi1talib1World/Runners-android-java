# Implementation Plan - Fix Features and Add Tests

This plan addresses several identified bugs in the Runners app and adds a comprehensive testing suite to prevent regressions.

## User Review Required

> [!IMPORTANT]
> - `LocationService` will now be explicitly stopped when a run is saved or discarded.
> - `UnitConverter.formatPace` output format will be changed for consistency (using `m'ss''` format).

## Proposed Changes

### [Component Name] Bug Fixes

#### [MODIFY] [UnitConverter.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/core/utils/UnitConverter.kt)
- Fix `formatPace` to use a consistent `m'ss''` format and ensure unit strings are correct.
- Update unit conversion logic if needed.

#### [MODIFY] [TrackingManager.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/services/TrackingManager.kt)
- Improve auto-pause logic to ensure duration doesn't jump on resume.

#### [MODIFY] [LocationService.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/services/LocationService.kt)
- Reset `runDurationSeconds` when a new run starts in `onStartCommand`.
- Only increment `runDurationSeconds` when `isTracking` is true.

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/features/home/HomeViewModel.kt)
- Add `UiEvent.RunDiscarded`.
- Fix `sendCheer` to emit a success event instead of an error.
- Prevent multiple concurrent `joinSession` calls using a loading check.

#### [MODIFY] [HomeFragment.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/features/home/HomeFragment.kt)
- Stop `LocationService` when `RunSaved` or `RunDiscarded` event is received.
- Add handling for the new cheer success event.

### [Component Name] Testing Suite

#### [MODIFY] [libs.versions.toml](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/gradle/libs.versions.toml)
- Add testing dependencies: `mockk`, `turbine`, `kotlinx-coroutines-test`, `androidx.core:core-testing`.

#### [MODIFY] [build.gradle](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/build.gradle)
- Include new testing dependencies.

#### [MODIFY] [UnitConverterTest.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/core/utils/UnitConverterTest.kt)
- Update tests to match fixed `formatPace` logic.

#### [NEW] [HomeViewModelTest.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/features/home/HomeViewModelTest.kt)
- Test UI state updates, goal selections, and event emissions.

#### [NEW] [TrackingManagerTest.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/services/TrackingManagerTest.kt)
- Test distance calculation, auto-pause behavior, and heart rate zone logic.

## Verification Plan

### Automated Tests
- Run all unit tests:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

### Manual Verification
- Deploy to device/emulator.
- Start a run, wait for auto-pause, verify duration stops.
- Move again, verify duration resumes without jumping.
- Save run, verify notification and service disappear.
- Send a cheer to a mock athlete, verify success message color.
