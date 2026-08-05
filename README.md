# Android Lifecycle Diagnostics

A small Android library that watches Activity and Fragment lifecycles at runtime and prints plain
English findings to Logcat. It is written for people who are still learning the Android lifecycle:
there is no dashboard, no heap dump to read and no configuration to get wrong. You add the module,
run the app, and read the log.

[![JitPack](https://jitpack.io/v/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics.svg)](https://jitpack.io/#Ariel-Yitzhaki/Android-LifeCycle-Diagnostics)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![minSdk](https://img.shields.io/badge/minSdk-29-blue)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

## Contents

- [What it does](#what-it-does)
- [Requirements](#requirements)
- [Installation](#installation)
- [Reading the output](#reading-the-output)
- [The four features](#the-four-features)
- [Tuning the thresholds](#tuning-the-thresholds)
- [Turning it off or picking features](#turning-it-off-or-picking-features)
- [Sample apps](#sample-apps)
- [How it starts itself](#how-it-starts-itself)
- [Limitations](#limitations)
- [Project structure](#project-structure)
- [Building](#building)
- [License](#license)

## What it does

Four independent features run at the same time, each printing under its own Logcat tag:

| Feature | Tag | Answers |
| --- | --- | --- |
| Lifecycle timing | `LifecycleDiagnostics` | How long did my own code inside `onCreate`, `onStart`, `onResume`, `onPause`, `onStop` and `onDestroy` take? |
| Callback validation | `CallbackValidation` | Did the callbacks arrive in an order the lifecycle actually takes, and did they balance? |
| Leak detection | `LeakDetection` | Is a screen still in memory several seconds after it was destroyed? |
| Main-thread blocking | `MainThreadBlocking` | What is holding up the main thread, and which screens are dropping frames? |

Everything is reported as a single greppable Logcat line. Ordinary information is logged at debug
level, findings at warn level.

## Requirements

- `minSdk` 29 or higher
- `compileSdk` 36
- Android Gradle Plugin 8.13.2, Gradle 8.13, Kotlin 2.2.21
- The library itself compiles against Java 11; the sample apps use Java 17

The library brings three AndroidX dependencies with it: `core-ktx`, `fragment-ktx` (for
`FragmentManager.FragmentLifecycleCallbacks`) and `androidx.metrics:metrics-performance` (JankStats,
used to count dropped frames).

## Installation

Released through [JitPack](https://jitpack.io/#Ariel-Yitzhaki/Android-LifeCycle-Diagnostics).

1. Add the JitPack repository in your `settings.gradle.kts`:

   ```kotlin
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven { url = uri("https://jitpack.io") }
       }
   }
   ```

2. Add the dependency in your app module's `build.gradle.kts`:

   ```kotlin
   dependencies {
       debugImplementation("com.github.Ariel-Yitzhaki.Android-LifeCycle-Diagnostics:Diagnostics:1.0.0")
   }
   ```

   Replace `1.0.0` with the release you want; the badge above shows the newest one. Any tag, branch
   name or commit hash works as a version, so `main-SNAPSHOT` tracks the tip of the branch.

   `debugImplementation` is the recommended configuration. The library asks for a garbage collection
   after every screen the user leaves and times every message the main thread runs, which is real
   work you do not want in a release build. The sample apps in this repository use plain
   `implementation` so that release builds of the samples still log.

3. Run the app. There is no third step: no `Application` subclass, no `install()` call, no
   annotation. The library declares its own `ContentProvider` in its manifest, that manifest is
   merged into yours, and the provider starts the four features before your `Application.onCreate`
   runs. See [How it starts itself](#how-it-starts-itself).

Groovy users, the same two additions:

```groovy
// settings.gradle
maven { url 'https://jitpack.io' }

// build.gradle
debugImplementation 'com.github.Ariel-Yitzhaki.Android-LifeCycle-Diagnostics:Diagnostics:1.0.0'
```

### Building against the source instead

To modify the library while using it, include it as a module rather than an artifact: copy the
`Diagnostics` directory into your project, add `include(":Diagnostics")` to `settings.gradle.kts`,
and depend on `debugImplementation(project(":Diagnostics"))`. That is what the two sample apps in
this repository do.

## Reading the output

Filter Logcat down to the four tags:

```bash
adb logcat -s LifecycleDiagnostics CallbackValidation LeakDetection MainThreadBlocking
```

In Android Studio, the equivalent filter in the Logcat window is:

```
tag:LifecycleDiagnostics | tag:CallbackValidation | tag:LeakDetection | tag:MainThreadBlocking
```

## The four features

### 1. Lifecycle timing

Times the code you wrote inside each of the six lifecycle callbacks. Activity measurements use the
`onActivityPreX` / `onActivityPostX` pairs on `Application.ActivityLifecycleCallbacks`, so they
bracket exactly one callback. Anything over 50 ms is marked slow.

```
D/LifecycleDiagnostics: HomeActivity.onCreate took 8.41 ms  [first time seen]
W/LifecycleDiagnostics: SlowCreateActivity.onCreate took 412.35 ms  SLOW (over 50 ms)  [first time seen]
D/LifecycleDiagnostics: HomeActivity.onDestroy took 0.32 ms  [configuration change]
```

Tags on the line:

- `[first time seen]` marks the first measurement recorded for that screen class in this process,
  which is usually the most expensive one because of class loading and inflation.
- `[configuration change]` means the screen was being replaced by a rotation or similar, not by
  normal navigation.
- `[approx: measured between callbacks, not around one]` appears on Fragment measurements. Only
  `Fragment.onCreate` has a real before/after pair; the other five callbacks are reported as the gap
  since the previous callback and are prefixed with `~`.

An Activity's `onCreate` measurement includes the `onCreate` of any fragments it restores, because
that work happens inside `super.onCreate()`.

### 2. Callback validation

Keeps a small state machine per live Activity and Fragment instance and reports five kinds of
problem:

- A callback that does not follow the state the component was already in.
- A component destroyed without ever reaching resumed.
- A component destroyed with unbalanced `onStart` and `onStop` counts.
- An Activity class destroyed and recreated more than 3 times in 10 seconds, not counting
  configuration changes, which usually means a restart loop or a redirect that bounces back.
- A Fragment destroyed while one of its views never received `onDestroyView`.

```
W/CallbackValidation: FragmentViewLeakFragment@8b21c4 (Fragment) was destroyed with 2 view(s) created but only 1 destroyed ...
W/CallbackValidation: RelaunchSelfActivity was destroyed and recreated 4 times in the last 10 seconds ...
W/CallbackValidation: SlowCreateActivity@3f2a1b (Activity) was destroyed without ever reaching resumed
```

The feature also switches on the three StrictMode VM checks that catch things left behind, and
receives the violations inside the process through `penaltyListener` so they can be printed with the
screen that was in the foreground at the time:

```
D/CallbackValidation: StrictMode VM checks are on: leaked registration objects, leaked closable objects, Activity leaks
W/CallbackValidation: StrictMode IntentReceiverLeakedViolation: ... best guess at where this came from: ...
```

Components whose `onCreate` happened before the library was installed are excluded from the
end-of-life checks, so the library never reports on its own blind spot.

### 3. Leak detection

When an Activity, Fragment or Fragment view is destroyed, it is held only through a `WeakReference`.
Five seconds later, on a background thread, the library requests a garbage collection, waits 100 ms
for the runtime to clear its weak references, and checks whether the component is still reachable.

A single retained instance is nearly always noise, so nothing is printed until a class shows a
pattern: at least 3 destructions of that class, with at least half of them retained. One finding per
class per session.

```
W/LeakDetection: ActivityLeakActivity (Activity) was still in memory 3 of 4 times it was destroyed this session ...
```

The finding lists the three usual causes rather than guessing: a static or companion object field
still pointing at the screen, a listener or receiver registered and never unregistered, or an inner
class, `Handler` or `Runnable` that outlived the screen.

Activities being replaced for a configuration change are skipped, because the framework legitimately
holds the old instance for a moment. Fragments are not skipped, since nothing is handed from an old
fragment to its replacement.

### 4. Main-thread blocking

Three detectors, all reporting under the same tag.

**Slow messages.** The library installs a `Printer` on the main `Looper`, which is called around
every message the main thread runs. A countdown runs on a background thread, and if a message is
still running after 200 ms the background thread captures the main thread's stack and prints the top
8 frames:

```
W/MainThreadBlocking: the main thread has been busy with one message for 213 ms (over the 200 ms limit) with SlowCreateActivity in the foreground ...
```

The stack is a snapshot taken at the 200 ms mark, not a recording, so it can show code that ran
after the slow part.

**Dropped frames.** JankStats counts the frames each Activity draws between `onStart` and `onStop`.
When a visit ends having dropped more than 5 per cent of its frames, one line is printed:

```
W/MainThreadBlocking: 18.4% of JankListActivity's frames were dropped while it was on screen (37 of 201, over the 5.0% limit) ...
```

**StrictMode thread violations.** Disk reads, disk writes and network on the main thread are caught
at the moment they happen, and named together with the foreground screen:

```
D/MainThreadBlocking: StrictMode main-thread checks are on: disk reads, disk writes, network
W/MainThreadBlocking: StrictMode DiskReadViolation on the main thread with MainThreadDiskReadActivity in the foreground: ...
```

## Tuning the thresholds

Every tunable value lives in one `object` per feature. Edit the constant and rebuild.

| Constant | File | Default |
| --- | --- | --- |
| `SLOW_CALLBACK_THRESHOLD_MS` | `lifecycle/DiagnosticsConstants.kt` | 50 |
| `RECREATE_LIMIT` | `callbacks/ValidationConstants.kt` | 3 |
| `RECREATE_WINDOW_MS` | `callbacks/ValidationConstants.kt` | 10000 |
| `WATCH_DELAY_MS` | `leaks/LeakConstants.kt` | 5000 |
| `GC_SETTLE_MS` | `leaks/LeakConstants.kt` | 100 |
| `MIN_DESTROY_COUNT` | `leaks/LeakConstants.kt` | 3 |
| `SLOW_MESSAGE_THRESHOLD_MS` | `blocking/BlockingConstants.kt` | 200 |
| `STACK_FRAMES_LOGGED` | `blocking/BlockingConstants.kt` | 8 |
| `JANK_PERCENT_THRESHOLD` | `blocking/BlockingConstants.kt` | 5.0 |

The Logcat tag each feature prints under is in the same file, as `LOG_TAG`.

## Turning it off or picking features

The auto-start provider is all or nothing. To choose features yourself, remove the provider from the
merged manifest in your own `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application>
        <provider
            android:name="com.ariel.diagnostics.DiagnosticsInitProvider"
            android:authorities="${applicationId}.diagnostics-init"
            tools:node="remove" />
    </application>

</manifest>
```

Then install only what you want from `Application.onCreate`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LifecycleDiagnostics.install(this)
        CallbackValidation.install(this)
        LeakDetection.install(this)
        MainThreadBlocking.install(this)
    }
}
```

Every `install()` is idempotent, so calling it twice is harmless.

The provider only runs in the default process. If your app declares a component with
`android:process`, call `install()` by hand from `Application.onCreate` for that process, the way
both sample apps do:

```kotlin
if (getProcessName() != packageName) {
    LifecycleDiagnostics.install(this)
    CallbackValidation.install(this)
    LeakDetection.install(this)
}
```

## Sample apps

Two runnable apps in this repository plant the exact faults the library is meant to catch. Almost
every screen comes as a FAULT / CONTROL pair: the same work, done wrong and then done right, so the
difference in Logcat is the whole lesson.

- `sample-views` - 19 Activity and Fragment screens using Views, view binding and RecyclerView.
- `sample-compose` - 19 routes in a single Activity using Compose and Navigation, including a
  recomposition churn pair that has no equivalent in the Views app.

| Screen or route | What it plants |
| --- | --- |
| `slow-create` | 400 ms of real CPU work inline in `onCreate` or first composition |
| `slow-resume` | The same 400 ms on every resume |
| `main-thread-disk-read` | A 4 MiB file read synchronously on the main thread |
| `leaky-activity` | The Activity stored in a companion object field, never cleared |
| `FragmentViewLeakFragment` | A view binding never nulled in `onDestroyView` (Views app only) |
| `leaky-viewmodel` | A ViewModel registered with a process-lifetime singleton, never unregistered |
| `unregistered-receiver` | A `BroadcastReceiver` registered in `onStart` and never unregistered |
| `jank-list` | 12 ms of blocking work per row inside the bind, on a 400 row list |
| `recomposition-churn` | A 60 Hz ticker read at the top of the tree, unstable parameters, nothing remembered (Compose app only) |
| `relaunch-self` | Finishes and relaunches itself 5 times, to drive a restart loop |
| `start-for-result` | A clean round trip to a second Activity and back |
| `secondary-process` | A screen declared with `android:process=":secondary"` |

The work is real, not simulated: `BusyWork` spins against the wall clock so a screen advertised as
400 ms costs about 400 ms on a fast phone and a slow emulator alike, and `SampleFiles` writes a real
4 MiB blob on first launch and reads it back in unbuffered 512 byte chunks.

Install and run them with:

```bash
./gradlew :sample-views:installDebug
./gradlew :sample-compose:installDebug
```

## How it starts itself

`DiagnosticsInitProvider` is a `ContentProvider` declared in the library's own manifest. Manifest
merging copies that declaration into every app that depends on the library, and Android creates
content providers after the `Application` object is constructed but before `Application.onCreate`.
That is the earliest a library can run code without any help from the app, and before any Activity
can exist.

Two details in the declaration matter:

- `android:authorities="${applicationId}.diagnostics-init"` is built from the consuming app's own id.
  Authorities must be unique across every app on the device, so a hardcoded name would stop two apps
  that both use the library from being installed side by side.
- `android:initOrder="100"` puts this provider ahead of other libraries' providers, so their setup
  work is measured rather than missed.

The two StrictMode policies are a deliberate exception. They are installed from
`onActivityPreCreated`, not from `install()`, because apps commonly call `setThreadPolicy` or
`setVmPolicy` in `Application.onCreate`, and those calls replace the whole policy. Installing later
means the library's checks survive. Both policies are seeded from whatever policy is already in
force, so nothing the app asked for is switched off.

## Limitations

- **Debug builds only.** The main-thread feature is called twice for every message the main thread
  runs, the leak feature requests a garbage collection a few seconds after every screen the user
  leaves, and capturing another thread's stack briefly pauses it.
- **One `Looper` printer.** A `Looper` holds a single message printer and offers no getter for it.
  Anything that calls `setMessageLogging` after the library does silently switches slow-message
  detection off. The library prints a debug line at startup saying it took the slot.
- **One StrictMode listener per policy.** A StrictMode policy holds a single `penaltyListener`, so
  the library replaces one the app may have set.
- **Default process only** unless you call `install()` yourself for other processes.
- **`System.gc()` is a request, not a command.** A "retained" answer can be a false alarm, which is
  why leak findings require a repeating pattern before they are printed.
- **Leaks are reported, not explained.** The library can tell you a screen is being held, but not by
  what. For the reference chain, reach for a heap dump.
- **Fragment timings other than `onCreate` are approximate**, since the framework gives no before
  half for them.
- **No API for reading findings programmatically.** Everything goes to Logcat.

## Project structure

```
Diagnostics/                      the library
  src/main/AndroidManifest.xml    declares the auto-start provider
  src/main/java/com/ariel/diagnostics/
    DiagnosticsInitProvider.kt    starts all four features
    lifecycle/                    feature 1, callback timing
    callbacks/                    feature 2, callback validation
    leaks/                        feature 3, retained screen detection
    blocking/                     feature 4, main-thread blocking and jank
sample-views/                     sample app built with Views and Fragments
sample-compose/                   sample app built with Compose and Navigation
jitpack.yml                       JDK and build command used for JitPack releases
```

Each feature package follows the same shape: an `object` with `install()`, one
`ActivityLifecycleCallbacks` implementation, one `FragmentLifecycleCallbacks` implementation where
relevant, plain data holders, a constants file, and a single logger class that is the only place the
feature touches `Log`.

## Building

```bash
./gradlew :Diagnostics:assembleDebug
./gradlew :sample-views:assembleDebug :sample-compose:assembleDebug
```

On Windows, use `gradlew.bat`. An `.aar` for the library is produced at
`Diagnostics/build/outputs/aar/`.

To try a release locally before tagging it, publish to your own Maven repository and consume it with
`mavenLocal()`:

```bash
./gradlew :Diagnostics:publishToMavenLocal -Pgroup=com.github.Ariel-Yitzhaki.Android-LifeCycle-Diagnostics -Pversion=1.0.0-local
```

That is the same command JitPack runs, with the group and version it would supply. Releases
themselves need nothing but a pushed tag: `jitpack.yml` pins JDK 17 and limits the build to
`:Diagnostics`, and the tag becomes the version, so no version number is hardcoded anywhere in the
build files.

## License

MIT. See [LICENSE](LICENSE).
