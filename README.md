# GPS Trip Computer

[![CI](https://github.com/gitLRD/gps-trip-computer-app/actions/workflows/ci.yml/badge.svg)](https://github.com/gitLRD/gps-trip-computer-app/actions/workflows/ci.yml)

Two independent trip meters and a live speed readout, using nothing but the phone's GPS
receiver. Built for **UK 12-car road rallies** — a pair of trip meters on the dashboard
without bolting anything permanent into the car.

No account, no network, no analytics. The app has no internet permission at all.

## Using it

Each trip records distance, average speed, elapsed time and maximum speed, and either can
be reset on its own by tapping it — so one can run for the whole event while the other
covers a single section between junctions. Distance leads the card, because that is what
gets read off against the roadbook.

Tracking is off until you switch it on, and stays on across restarts until you switch it
off. While it is running a notification shows distance and speed, and tracking continues
with the app in the background — checking a map mid-rally will not stop the meters.

### Average speed

Average speed is derived from distance over time, not by averaging the individual GPS speed
readings: a sample mean is skewed by however often the receiver happens to report, and
cannot account for time spent stopped.

The **Include stopped time** setting chooses which time to divide by:

| Setting | Divides distance by | Use for |
| --- | --- | --- |
| On (default) | Total time since the trip was reset | Overall average against a schedule; a halt at a time control pulls it down |
| Off | Only time spent above 0.5 m/s | Moving average, ignoring stops |

### Distance accuracy

Three things guard the distance figure, since that is the number that matters most:

* Fixes reporting worse than 25 m horizontal accuracy are discarded outright. Under trees
  and in cuttings a receiver will happily emit fixes tens of metres out.
* Distance only accumulates while the receiver reports genuine movement, so a stationary
  GPS jittering by a few metres does not clock up miles that were never driven.
* When movement is below that threshold the measuring anchor is **held** rather than
  advanced, so a slow crawl looking for a junction is added once you are moving again
  instead of being quietly dropped.

### At night

A third **Night** theme renders red on black, which preserves dark adaptation far better
than a bright screen, and an in-app brightness slider dims the display below what the
system would normally allow without leaving the app.

### Screen sizes and foldables

The layout is chosen from the width of the window, not from the device, so it follows
folding, rotation and multi-window resizing alike:

| Window width | Layout |
| --- | --- |
| Under 600dp — handset portrait, or a folded cover screen | Trips stacked, speed readout beneath |
| 600dp and over — unfolded inner screen, tablet, landscape | Trips in a left column, speed readout alongside |

Trips live in the Application and are written to storage as you go, so neither unfolding
the device nor Android reclaiming the process mid-event loses the numbers.

## Building

Needs a JDK 17 or 21. It will **not** build with the JDK 25 bundled in recent Android
Studio releases; set the Gradle JDK in Studio, or from the command line:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew assembleDebug
```

## Tests

```sh
./gradlew testDebugUnitTest          # 63 tests, no device needed
./gradlew connectedDebugAndroidTest  # 27 tests, needs a device or emulator
./gradlew verifyRoborazziDebug       # screenshot comparison
./gradlew lintDebug
```

The logic that matters is deliberately kept free of Android types so it can be tested on
the JVM in milliseconds:

| Class | Covers |
| --- | --- |
| `Trip` | Average speed from distance over time, moving vs elapsed, maximum speed |
| `Units` | Metric/imperial conversion, fallback for unknown settings |
| `TrackingState` | Accuracy filter, movement gate, anchor holding, stale-fix timeout, per-trip reset |
| `TripCodec` | Persisting trips, and surviving corrupt stored data |
| `ScreenLayout` | The width breakpoint |
| `Formatting` | Elapsed-time display |

`TripTracker` is a thin adapter supplying real locations and a real clock to
`TrackingState`, so nothing interesting is stranded behind the platform APIs. Instrumented
tests cover settings persistence, what the cards render, and that trip data survives a
configuration change. Screenshot goldens in `app/src/test/screenshots` run on the JVM via
Robolectric — re-record them with `./gradlew recordRoborazziDebug`. They can be recorded on
any platform: the comparison allows a small per-pixel colour tolerance, which absorbs the
way macOS and Linux rasterise antialiased edges differently. See `ScreenshotOptions.kt` for
why that is a colour tolerance and not a pixel-count budget.

CI runs the build, unit tests, screenshots and lint on every push, and the instrumented
tests on emulators at **API 24 and 34** — the minimum and target. API 24 is there on
purpose: `LocationListener`'s status callbacks only gained default implementations in API
30, so removing those overrides would throw `AbstractMethodError` on older devices and
nowhere else.

## Installing

The app is published on **F-Droid**, which builds it from source and signs it itself — so
what you install is provably built from the code in this repository, and no signing key of
mine is involved.

To build it yourself, `./gradlew assembleDebug` produces a sideloadable APK.
`./gradlew assembleRelease` deliberately produces an *unsigned* one, because F-Droid holds
the release signature. See [RELEASING.md](RELEASING.md).

## Licence

Released under the GNU General Public License v3.0 or later. See [LICENSE](LICENSE).

## Icon

The app icon is a Japanese Spitz in a pair of old-fashioned racing goggles, drawn as
vector art and covered by the same GPL-3.0-or-later licence as the rest of the project.

It is generated rather than hand-drawn: `misc/spitz_logo.py` emits `misc/logo_spitz.svg`
and the two masked variants the launcher needs. The fur is the reason — it takes a couple
of dozen jittered points to read as fluff rather than as a cog wheel, and those are far
easier to retune as parameters than as path data. The jitter is seeded, so regenerating
produces byte-identical output instead of a diff every time.

To regenerate after changing it:

```sh
python3 misc/spitz_logo.py misc/logo_spitz.svg square
```

then re-render the mipmaps at 108dp (foreground) and 48dp (legacy) across all five
densities. The foreground is scaled to 82% so a circular launcher mask crops a little ruff
rather than taking the ear tips off.

### Previous icon

The earlier icon was assembled from three icons from the Noun Project, used under CC BY.
It is no longer shipped, but the originals remain in `misc/logo_assets/` and the
attribution stands for as long as they do:

* Odometer — P Thanga Vignesh
* Satellite — Eugene Dobrik
* Speedometer — Artdabana@Design
