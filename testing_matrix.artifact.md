# Runners Android: Comprehensive Testing Matrix

## 1. Functional Testing (FUN)

| ID | Feature | Description | Priority | Automated |
| :--- | :--- | :--- | :--- | :--- |
| FUN-001 | Auth: Login | Validate successful Email/Password and Google Sign-In. | P0 | Yes (E2E) |
| FUN-002 | Auth: Sign Up | Validate new account creation with validation. | P0 | Yes (E2E) |
| FUN-003 | Tracking: GPS | Verify real-time coordinate plotting on map. | P0 | Manual |
| FUN-004 | Tracking: Safety | Verify "Hold to Finish" (2s) prevents accidental stops. | P1 | Yes (UI) |
| FUN-005 | History: CRUD | Verify run saving, listing, and deletion. | P0 | Yes (Unit) |
| FUN-006 | Gear: Mileage | Verify shoe mileage increments automatically after a run. | P1 | Yes (Unit) |
| FUN-007 | Sync: Cloud | Verify background sync to cloud and Health Connect. | P1 | Manual |

## 2. UI & Design System (UI)

| ID | Feature | Description | Priority | Automated |
| :--- | :--- | :--- | :--- | :--- |
| UI-001 | Branding | Verify "Neon Volt & Dark" consistency across all screens. | P1 | Manual |
| UI-002 | Dark Mode | Verify no purple or default Material colors remain. | P0 | Manual |
| UI-003 | Feedback | Verify Snackbars appear for successes and errors. | P1 | Yes (UI) |
| UI-004 | Charts | Verify Pace, HR, and Cadence charts render correctly. | P2 | Manual |

## 3. Performance & Stability (PERF)

| ID | Metric | Target | Priority | Status |
| :--- | :--- | :--- | :--- | :--- |
| PERF-001 | Startup | Cold start to Splash < 2s. | P0 | Pending |
| PERF-002 | Memory | No OOM during long (1h+) tracking sessions. | P0 | Pending |
| PERF-003 | Battery | < 5% drain per hour of active tracking. | P1 | Pending |
| PERF-004 | Stability | 0 Fatal Exceptions (NPEs, ClassCast). | P0 | Ongoing |

## 4. Compliance & Security (SEC)

| ID | Feature | Requirement | Priority | Status |
| :--- | :--- | :--- | :--- | :--- |
| SEC-001 | Permissions | Request only necessary permissions (Location, Notifications). | P0 | Verified |
| SEC-002 | Exporting | All internal components set to `exported="false"`. | P0 | Verified |
| SEC-003 | FS Type | Foreground Service declared as `location`. | P0 | Verified |

---

# Detailed Functional Test Cases

## FUN-001: Auth Login Flow
1. **Preconditions**: App is in Logged Out state.
2. **Steps**:
   - Enter valid email/password.
   - Tap LOGIN.
3. **Expected**:
   - Progress indicator appears.
   - Successful transition to HomeFragment.
   - "Welcome" snackbar shown.

## FUN-006: Gear Auto-Tracking
1. **Preconditions**: User has an "Active" shoe with 100km mileage.
2. **Steps**:
   - Record a 5km run.
   - Finish and save run.
   - Check Gear screen.
3. **Expected**:
   - Shoe mileage is now exactly 105km.
