# GPS Trip Computer

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

## Building

The project needs a JDK 17 or 21. It will **not** build with the JDK 25 bundled
in recent Android Studio releases — AGP 8.9 cannot parse that version and fails
with a bare `* What went wrong: 25.0.2`. Set the Gradle JDK in Android Studio, or
from the command line:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew assembleDebug
```

Tests:

```sh
./gradlew testDebugUnitTest        # trip and unit-conversion logic, no device needed
./gradlew connectedDebugAndroidTest # settings persistence, needs a device or emulator
```

## Licence

Released under the GNU General Public License v3.0 or later. See [LICENSE](LICENSE).

## Icon attribution

The app icon is assembled from three icons from the Noun Project, used under
CC BY. The originals are in `misc/logo_assets/`:

* Odometer — P Thanga Vignesh
* Satellite — Eugene Dobrik
* Speedometer — Artdabana@Design
