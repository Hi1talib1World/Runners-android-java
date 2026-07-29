# Implementation Plan - Update Firebase Configuration and Fix Features

This plan covers updating the Firebase configuration file, integrating the Firebase BoM, and completing the previously planned bug fixes and testing suite.

## Proposed Changes

### [Component Name] Firebase Configuration & SDK Integration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/gradle/libs.versions.toml)
- Update `google-services` plugin version to `4.5.0`.
- Add `firebaseBom` version (e.g., `33.2.0`).
- Add `firebase-bom` library definition.
- Add `firebase-analytics` library definition.

#### [MODIFY] [app/build.gradle](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/build.gradle)
- Use `libs.firebase.bom` as a platform dependency.
- Remove explicit versions from Firebase dependencies (if they are managed by BoM in `libs.versions.toml` or by the `implementation platform` call).
- Add `implementation(libs.firebase.analytics)`.

#### [MODIFY] [google-services.json](file:///C:/Users/hicha/Documents/GitHub/Runners-android-java/app/google-services.json)
- Replace placeholder Firebase configuration with the provided `google-services.json` for project `running-25e33`.

### [Component Name] Bug Fixes (Continuing)
... (rest of the plan)
