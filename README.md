# Runners - High-Performance Activity Tracker

Runners is a production-ready Android application designed for athletes who demand precision tracking, deep analytics, and a social community experience. Built with a modern, high-contrast **Neon Volt & Dark** theme, it provides a seamless and motivating environment for your daily runs.

## 🚀 Key Features

### 📍 Real-time Tracking & Safety
*   **Precision GPS**: High-frequency telemetry ensures your pace, distance, and path are tracked with professional accuracy.
*   **Live Metrics**: Real-time display of pace, distance, duration, heart rate, and cadence.
*   **Safety Finish**: A 2-second "Hold to Finish" gesture prevents accidental run terminations.
*   **Audio Coaching**: Real-time split announcements (e.g., "1 km completed") via Text-to-Speech (TTS).
*   **Run Goals**: Set targets (Free Run, 5KM, 10KM, 30 Mins) and track your progress live.

### 📊 Advanced Analytics
*   **Run Summary**: Detailed post-run insights with interactive maps and performance charts.
*   **Pace Analysis**: Visualize your intensity fluctuations over time using integrated line charts.
*   **Lifetime Dashboard**: Track your total mileage, run count, and personal bests directly on your profile.

### 🏆 Community & Motivation
*   **Interactive Leaderboards**: Compete globally or within your club/friends list. Weekly rankings are updated in real-time.
*   **Achievement System**: Unlock collectible high-contrast badges for reaching significant milestones.
*   **Social Discovery**: Find and follow other athletes or join local running clubs.
*   **Social Sharing**: Generate and share professional "Race Cards" of your runs to social media.

### ☁️ Cloud & Connectivity
*   **Firebase Integration**: Secure authentication and real-time profile management.
*   **Background Sync**: Automated cloud backups with offline resilience and "Sync Status" visibility.
*   **Cross-Device Support**: Your profile and history stay in sync across all your devices.

### ⚙️ Universal Preferences
*   **Global Unit Support**: Instantly switch between **Metric (KM)** and **Imperial (MI)** systems across the entire app.
*   **Modern Architecture**: Built with Kotlin, Hilt (Dependency Injection), Flow, and Coroutines for high performance and stability.

## 🛠 Tech Stack
*   **Language**: Kotlin
*   **Architecture**: MVVM with Clean Architecture principles
*   **Database**: Room with Flow support
*   **Networking**: Retrofit & OkHttp
*   **Authentication**: Firebase Auth & Google Sign-In
*   **Maps**: osmdroid
*   **Charts**: MPAndroidChart
*   **DI**: Dagger Hilt
*   **Background Tasks**: Coroutines & Services

## 🏁 Getting Started
1.  Clone the repository.
2.  Add your `google-services.json` to the `app/` directory.
3.  Add your Google Maps API key in `strings.xml`.
4.  Build and run using Android Studio.

---
*By Runners, for Runners.*
