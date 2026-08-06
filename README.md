![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Min%20SDK%2029-3DDC84?logo=android&logoColor=white)
[![JitPack](https://jitpack.io/v/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics.svg)](https://jitpack.io/#Ariel-Yitzhaki/Android-LifeCycle-Diagnostics)
![License](https://img.shields.io/badge/License-MIT-blue)

# Android Lifecycle Diagnostics




<br>

## Description

An Android library that watches Activity and Fragment lifecycles at runtime and prints what it sees to Logcat. Four detectors run at once: callback timing, callback order, retained screens, and main thread blocking.

No dashboard, no heap dump, no setup code. Add the dependency, run the app, read the log.

Built with Kotlin, AndroidX Fragment, JankStats and StrictMode.


## Features

### Lifecycle Timing
`LifecycleDiagnostics`
- Times `onCreate`, `onStart`, `onResume`, `onPause`, `onStop` and `onDestroy` on every screen
- Activity numbers are exact, Fragment numbers are gaps between callbacks and marked `~`
- Reports what it costs a Fragment to build its view, covering `onCreateView` and `onViewCreated`
- Anything over 50 ms is flagged `SLOW`
- A Fragment's resume to pause gap is reported as time on screen, never as work
<br>
<br>

https://github.com/user-attachments/assets/45ff60c8-7953-4569-9839-b6344f6b5011

<br>

### Callback Validation
`CallbackValidation`
- Keeps a state machine per live Activity and Fragment, and reports steps the lifecycle does not take
- Catches a screen started and then destroyed without ever reaching resumed
- Catches unbalanced `onStart` and `onStop` counts
- Catches an Activity recreated more than 3 times in 10 seconds, which is a restart loop
- Catches a Fragment destroyed with a view that never got `onDestroyView`
- Switches on the StrictMode VM checks for leaked receivers, closables and Activities
<br>
<br>

https://github.com/user-attachments/assets/0a3111ae-08ec-4f7e-bc87-ca5deb58aa47

<br>

### Leak Detection
`LeakDetection`
- Holds every destroyed Activity, Fragment and Fragment view through a `WeakReference`
- Asks for a garbage collection 5 seconds later and checks what survived
- Prints nothing until a class shows a pattern of 3 destructions with at least half retained
- Counts a view its Fragment outlived apart from one that died alongside its Fragment
- The first of those is the back stack case: a binding or `findViewById` result never cleared
<br>
<br>

https://github.com/user-attachments/assets/df3eb66d-d82f-4659-b0f7-ccebb2b9460c

<br>


### Main-Thread Blocking
`MainThreadBlocking`
- Captures the main thread's stack when one message runs longer than 200 ms
- Counts dropped frames per screen with JankStats, including `DialogFragment` windows
- Reports a screen that keeps the main thread busy for most of its first 5 seconds in front
- Switches on the StrictMode thread checks for disk reads, disk writes and network
- Names the first frame from your own package on a StrictMode stack, or says there is none
<br>
<br>

https://github.com/user-attachments/assets/6f04fe97-68b2-40c4-9f09-dd04d89b21c3

<br>
<br>

## Output

Every line names the screen the user was looking at, which is the Fragment in front with its host Activity in brackets. Debug level carries ordinary information, warn level carries a problem.

```bash
adb logcat -s LifecycleDiagnostics CallbackValidation LeakDetection MainThreadBlocking
```

The same filter in the Android Studio Logcat window:

```
tag:LifecycleDiagnostics | tag:CallbackValidation | tag:LeakDetection | tag:MainThreadBlocking
```
<br>

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| Language | Kotlin |
| Platform | Android (Min SDK 29, Compile SDK 36) |
| Build | Android Gradle Plugin 8.13.2, Gradle 8.13, Java 11 |
| Lifecycle | `Application.ActivityLifecycleCallbacks`, `FragmentManager.FragmentLifecycleCallbacks` |
| Frames | AndroidX JankStats (`androidx.metrics:metrics-performance`) |
| Runtime checks | StrictMode thread and VM policies |
| Startup | `ContentProvider` merged into the host manifest |
| Distribution | JitPack |
<br>

## Architecture

```
com.ariel.diagnostics/
├── DiagnosticsInitProvider.kt   # starts all four detectors before Application.onCreate
├── StackSummary.kt              # turns a stack trace into one line
├── lifecycle/                   # callback timing
├── callbacks/                   # callback order, balance and VM StrictMode
├── leaks/                       # retained screen detection
└── blocking/                    # main thread, frames and thread StrictMode
```

- **Each package is self-contained**: an `object` with `install()`, one `ActivityLifecycleCallbacks`, one `FragmentLifecycleCallbacks`, plain data holders, a constants file and a single logger.
- **Only the logger touches `Log`**, so everything one feature prints is worded in one place.
- **Nothing holds a screen.** Names and weak references only, so watching for leaks never causes one.
- **Fragment callbacks register recursively**, so a Fragment nested inside another is measured like any other.
<br>

## Setup

There are two ways in. Clone the repository and run the sample app to see what the library reports, or add it to an app of your own.

### Option 1: Run the sample app

Nothing to configure. The sample depends on the library as a local module, so cloning and running is the whole process. There is no dependency to add, no version to pick and no key to supply.

```bash
git clone https://github.com/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics.git
```

Open the project in Android Studio and run the `sample-views` configuration, or install it from the command line:

```bash
./gradlew :sample-views:installDebug
```

Then filter Logcat to the four tags and tap through the screens. See [Sample App](#sample-app) for what each screen plants.

### Option 2: Add it to your own app

Requires Min SDK 29 or higher and Compile SDK 36.

**1. Add JitPack** to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2. Add the dependency** to your app module's `build.gradle.kts`. The badge at the top shows the newest release:

```kotlin
dependencies {
    debugImplementation("com.github.Ariel-Yitzhaki:Android-LifeCycle-Diagnostics:1.2.0")
}
```

`debugImplementation` is the one to use. The library times every main thread message and asks for a garbage collection after every screen the user leaves, which is real work you do not want shipped.

**3. Run the app** and read Logcat. No `Application` subclass, no `install()` call, no annotation.
<br>

## Thresholds

Every tunable value lives in one `object` per feature. Edit the constant and rebuild. The Logcat tag each feature prints under sits in the same file, as `LOG_TAG`.

| Constant | File | Default |
| --- | --- | --- |
| `SLOW_CALLBACK_THRESHOLD_MS` | `lifecycle/DiagnosticsConstants.kt` | 50 |
| `RECREATE_LIMIT` | `callbacks/ValidationConstants.kt` | 3 |
| `RECREATE_WINDOW_MS` | `callbacks/ValidationConstants.kt` | 10000 |
| `VIOLATION_FRAMES_LOGGED` | `callbacks/ValidationConstants.kt` | 8 |
| `WATCH_DELAY_MS` | `leaks/LeakConstants.kt` | 5000 |
| `GC_SETTLE_MS` | `leaks/LeakConstants.kt` | 100 |
| `MIN_DESTROY_COUNT` | `leaks/LeakConstants.kt` | 3 |
| `SLOW_MESSAGE_THRESHOLD_MS` | `blocking/BlockingConstants.kt` | 200 |
| `STACK_FRAMES_LOGGED` | `blocking/BlockingConstants.kt` | 8 |
| `VIOLATION_FRAMES_LOGGED` | `blocking/BlockingConstants.kt` | 8 |
| `JANK_PERCENT_THRESHOLD` | `blocking/BlockingConstants.kt` | 5.0 |
| `MIN_FRAMES_COUNTED` | `blocking/BlockingConstants.kt` | 20 |
| `SETTLE_WINDOW_MS` | `blocking/BlockingConstants.kt` | 5000 |
| `BUSY_PERCENT_THRESHOLD` | `blocking/BlockingConstants.kt` | 50.0 |
| `MIN_SETTLE_MS` | `blocking/BlockingConstants.kt` | 500 |
<br>

## Sample App

`sample-views` is a runnable app that plants the faults the library catches, across 38 Activity and Fragment screens using Views, view binding and RecyclerView. Most screens come as a FAULT and CONTROL pair: the same work done wrong, then done right, so the difference in Logcat is the whole lesson.

It groups its screens by the feature each one exercises, and every screen says on the home list what it plants and what the library should print because of it.

**1 · Slow lifecycle callbacks** - `LifecycleDiagnostics`

| Screen | What it plants |
| --- | --- |
| `SlowCreateActivity` | 400 ms of real CPU work inline in `onCreate` |
| `SlowResumeActivity` | The same 400 ms on every resume |
| `SlowViewBuildFragment` | 180 ms in `onCreateView` and 140 ms in `onViewCreated` |
| `NestedParentFragment` | A slow child fragment in a child `FragmentManager` |

**2 · Work on the main thread** - `MainThreadBlocking`

| Screen | What it plants |
| --- | --- |
| `MainThreadDiskReadActivity` | A 4 MiB file read synchronously on the main thread |
| `MainThreadDiskWriteActivity` | 2 MiB written and fsynced on the main thread |
| `MainThreadNetworkActivity` | A TCP connect on the main thread, over loopback |
| `BusySettleActivity` | 60 ms of work every 80 ms for six seconds, no single message slow |
| `JankListActivity` | 12 ms of blocking work per row on a 400 row list |
| `JankDialogActivity` | The same list inside a `DialogFragment`, in a window of its own |

**3 · Screens that never go away** - `LeakDetection`, `CallbackValidation`

| Screen | What it plants |
| --- | --- |
| `ActivityLeakActivity` | The Activity stored in a companion object field, never cleared |
| `FragmentViewLeakFragment` | A view binding never nulled in `onDestroyView` |
| `ViewCaptureFragment` | The fragment's root view handed to a process-lifetime cache |
| `ViewModelLeakFragment` | A ViewModel in a global registry, holding a callback into the fragment |
| `UnregisteredReceiverActivity` | A `BroadcastReceiver` registered in `onStart`, never unregistered |
| `ServiceBindLeakActivity` | A `ServiceConnection` bound in `onStart`, never unbound |
| `LeakedClosableActivity` | A `FileInputStream` abandoned without being closed |

**4 · Lifecycle order and shape** - `CallbackValidation`

| Screen | What it plants |
| --- | --- |
| `FinishInStartActivity` | `finish()` in `onStart`, after the view is already built |
| `StrayCallbackActivity` | A second `onStart` delivered by hand |
| `ViewWithoutContainerFragment` | A view inflated by a fragment added with no container |
| `RelaunchSelfActivity` | Finishes and relaunches itself 5 times |
| `StartForResultActivity` | A clean round trip to a second Activity and back |

**5 · A second process**

| Screen | What it plants |
| --- | --- |
| `SecondaryProcessActivity` | A screen declared with `android:process=":secondary"` |
<br>

## Limitations

- **Debug builds only.** Timing every main thread message and collecting after every screen is real work.
- **Emulator numbers overstate.** Frame timing runs against an emulated vsync and unoptimised code. StrictMode counts are the exception, since they fire per call.
- **One `Looper` printer.** Anything calling `setMessageLogging` after the library switches slow message detection off.
- **One StrictMode listener per policy.** The library replaces one the app may have set.
- **`System.gc()` is a request.** A retained answer can be wrong, which is why a repeating pattern is required.
- **Leaks are reported, not explained.** For the reference chain, take a heap dump.
- **Fragment timings other than `onCreate` are gaps**, since the framework offers no hook before them.
- **Dialogs an app builds itself are not counted.** Only Activity and `DialogFragment` windows have a lifecycle to hook.
- **Everything goes to Logcat.** There is no API for reading results programmatically.
<br>

## License

MIT. See [LICENSE](LICENSE).
