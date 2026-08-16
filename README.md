# GPS Trip Computer

[![CI](https://github.com/gitLRD/gps-trip-computer-app/actions/workflows/ci.yml/badge.svg)](https://github.com/gitLRD/gps-trip-computer-app/actions/workflows/ci.yml)

A small Android trip computer. It shows a live speed readout and two independent
trip computers, each recording distance and average speed, using nothing but the
device's GPS receiver.

No account, no network, no analytics. The app has no internet permission and does
not request background location.

## Average speed

Average speed is derived from distance over time, not by averaging the individual
GPS speed readings — a sample mean is skewed by however often the receiver happens
to report, and cannot account for time spent stopped.

The **Include stopped time** setting chooses which time to divide by:

| Setting | Divides distance by | Use for |
| --- | --- | --- |
| On (default) | Total time since the trip was reset | Overall journey average; a stop at a checkpoint pulls it down |
| Off | Only time spent above 0.5 m/s | Moving average, ignoring stops |

Distance only accumulates while the receiver reports genuine movement, so a
stationary GPS jittering by a few metres does not clock up phantom distance.

## Screen sizes and foldables

The layout is chosen from the width of the window it is given, not from the device, so it
follows folding, rotation and multi-window resizing alike:

| Window width | Layout |
| --- | --- |
| Under 600dp — handset portrait, or a folded cover screen | Trips stacked, speed readout beneath |
| 600dp and over — unfolded inner screen, tablet, landscape | Trips in a left column, speed readout alongside |

Trip data lives in a `ViewModel`, so unfolding the device — which is a configuration change
like any other — does not restart tracking or discard the journey so far.

## Building

The project needs a JDK 17 or 21. It will **not** build with the JDK 25 bundled
in recent Android Studio releases — AGP 8.9 cannot parse that version and fails
with a bare `* What went wrong: 25.0.2`. Set the Gradle JDK in Android Studio, or
from the command line:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew assembleDebug
```

## Tests

```sh
./gradlew testDebugUnitTest          # 31 tests, no device needed
./gradlew connectedDebugAndroidTest  # 15 tests, needs a device or emulator
./gradlew lintDebug
```

The logic that matters is deliberately kept free of Android types so it can be tested on
the JVM in milliseconds:

| Class | Covers |
| --- | --- |
| `Trip` | Average speed from distance over time, moving vs elapsed |
| `Units` | Metric/imperial conversion, fallback for unknown settings |
| `TrackingState` | Movement gate, stale-fix timeout, speed fallback, per-trip reset |

`TripTracker` is a thin adapter that supplies real locations and a real clock to
`TrackingState`, so nothing interesting is stranded behind the platform APIs. The
instrumented tests cover settings persistence, what the cards render, and that trip data
survives a configuration change.

CI runs the build, unit tests and lint on every push, and the instrumented tests on
emulators at **API 24 and 34** — the minimum and target. API 24 is there on purpose:
`LocationListener`'s status callbacks only gained default implementations in API 30, so
removing those overrides would throw `AbstractMethodError` on older devices and nowhere
else.

## Licence

Released under the GNU General Public License v3.0 or later. See [LICENSE](LICENSE).

## Icon attribution

The app icon is assembled from three icons from the Noun Project, used under
CC BY. The originals are in `misc/logo_assets/`:

* Odometer — P Thanga Vignesh
* Satellite — Eugene Dobrik
* Speedometer — Artdabana@Design
