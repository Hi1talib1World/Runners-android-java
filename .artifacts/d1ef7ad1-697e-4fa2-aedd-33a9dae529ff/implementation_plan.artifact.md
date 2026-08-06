# Implementation Plan - Refactor to Interfaces and Use Fakes in Tests

This plan describes how to refactor the app's dependencies to avoid using any mocking frameworks (like MockK) in unit tests, as per the user's request.

## User Review Required

> [!IMPORTANT]
> - Several core classes (`RunRepository`, `WorkoutRepository`, `SettingsRepository`, `BillingManager`, and `HealthConnectManager`) will be refactored into interfaces.
> - Hilt modules will be updated to use `@Binds` for these interfaces.
> - Testing will now rely on **Fakes** (manual implementations of interfaces) instead of mocks.

## Proposed Changes

### [Component Name] Dependency Refactoring

#### [MODIFY] [RunRepository.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/data/repository/RunRepository.kt)
- Convert `RunRepository` to an `interface`.
- Rename the original class to `DefaultRunRepository`.

#### [MODIFY] [WorkoutRepository.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/data/repository/WorkoutRepository.kt)
- Convert `WorkoutRepository` to an `interface`.
- Rename the original class to `DefaultWorkoutRepository`.

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/features/settings/SettingsRepository.kt)
- Convert `SettingsRepository` to an `interface`.
- Rename the original class to `DefaultSettingsRepository`.

#### [MODIFY] [BillingManager.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/features/subscription/BillingManager.kt)
- Convert `BillingManager` to an `interface`.
- Rename the original class to `DefaultBillingManager`.

#### [MODIFY] [HealthConnectManager.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/core/health/HealthConnectManager.kt)
- Convert `HealthConnectManager` to an `interface`.
- Rename the original class to `DefaultHealthConnectManager`.

### [Component Name] Hilt Configuration

#### [NEW] [RepositoryModule.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/main/java/com/denzo/runners/core/di/RepositoryModule.kt)
- Add `@Binds` methods to link the new interfaces to their default implementations.

### [Component Name] Unit Testing (Fakes)

#### [NEW] [FakeRunRepository.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/fakes/FakeRunRepository.kt)
#### [NEW] [FakeWorkoutRepository.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/fakes/FakeWorkoutRepository.kt)
#### [NEW] [FakeSettingsRepository.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/fakes/FakeSettingsRepository.kt)
#### [NEW] [FakeBillingManager.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/fakes/FakeBillingManager.kt)
#### [NEW] [FakeHealthConnectManager.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/fakes/FakeHealthConnectManager.kt)

#### [MODIFY] [HomeViewModelTest.kt](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/src/test/java/com/denzo/runners/features/home/HomeViewModelTest.kt)
- Replace all `mockk` usages with the new Fakes.

## Verification Plan

### Automated Tests
- Run all unit tests to ensure they pass without any mocking framework:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- Verify that the app still builds and runs correctly with Hilt.
