# Balloon Live Wallpaper V2

Native Android live wallpaper: a cartoon green balloon moves over a black background behind the launcher icons.

## What V2 does

- Android `WallpaperService` implementation.
- Black wallpaper background.
- Cartoon green balloon drawn entirely with Android Canvas; no image download is required.
- Smooth frame-based animation, targeting about 60 FPS while visible.
- Physics-style motion using position, velocity, buoyancy, gravity, air drag, changing wind and boundary collisions.
- The balloon leans slightly with horizontal velocity and its strings sway with movement.
- The wallpaper does **not** enable touch events, so it does not deliberately consume launcher taps/swipes.
- A small launcher activity opens Android's live-wallpaper selection UI.

## Project structure

```text
BalloonLiveWallpaperV2/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/balloonwallpaper/
│       │   ├── MainActivity.kt
│       │   └── BalloonWallpaperService.kt
│       └── res/
│           ├── drawable/balloon_preview.xml
│           ├── values/colors.xml
│           ├── values/strings.xml
│           ├── values/styles.xml
│           └── xml/balloon_wallpaper.xml
├── build.gradle
├── gradle.properties
├── gradle/wrapper/
├── gradlew
├── gradlew.bat
├── codemagic.yaml
└── settings.gradle
```

## Build with Codemagic

1. Put this project in a GitHub repository.
2. Connect the repository to Codemagic.
3. Select the `android-workflow` from `codemagic.yaml`.
4. For a first test, use a debug build or configure a signing key for the release workflow.
5. Download the resulting APK and install it on an Android phone.
6. Open **Balloon Live Wallpaper** and press **Set Balloon Wallpaper**.

## Signing

The release workflow expects a Codemagic Android signing configuration named `keystore_reference`. If you have not configured signing in Codemagic yet, change the workflow to a debug build for initial testing, or add your own Codemagic keystore reference.

## Why the launcher remains usable

The service is a wallpaper rather than an overlay window. It draws into Android's wallpaper surface. The launcher is a separate layer above it. The service also deliberately does not call `setTouchEventsEnabled(true)`, so it has no need to receive home-screen touch events.

## Physics model

The balloon is not moved by simply adding a fixed number of pixels every frame. Each frame calculates elapsed time and updates velocity and position. The simulation includes:

- upward buoyancy
- downward gravity
- velocity-dependent air drag
- smoothly varying horizontal wind
- low-frequency turbulence
- energy loss when hitting screen boundaries
- velocity-dependent tilt and string sway

This is intentionally a lightweight approximation rather than a full fluid-dynamics model; it is appropriate for a live wallpaper and keeps CPU/GPU work modest.

## Build wrapper note

`gradlew` is included and pins Gradle 8.10.2. In an environment that already provides Gradle it uses that installation; otherwise the script downloads the pinned Gradle distribution into the normal Gradle cache. Android Studio can also import the project directly.
