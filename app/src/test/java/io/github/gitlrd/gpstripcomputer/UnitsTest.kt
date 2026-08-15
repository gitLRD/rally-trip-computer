package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `metres convert to kilometres`() {
        assertEquals(1.0, metresTo(1_000.0, DistanceUnit.KILOMETRES), 1e-9)
        assertEquals(0.0, metresTo(0.0, DistanceUnit.KILOMETRES), 1e-9)
    }

    @Test
    fun `metres convert to miles`() {
        assertEquals(1.0, metresTo(1_609.344, DistanceUnit.MILES), 1e-9)
        // 10 km is a shade over 6.21 miles.
        assertEquals(6.213712, metresTo(10_000.0, DistanceUnit.MILES), 1e-6)
    }

    @Test
    fun `metres per second convert to kilometres per hour`() {
        assertEquals(3.6, metresPerSecondTo(1.0, SpeedUnit.KILOMETRES_PER_HOUR), 1e-9)
        assertEquals(100.0, metresPerSecondTo(27.777778, SpeedUnit.KILOMETRES_PER_HOUR), 1e-5)
    }

    @Test
    fun `metres per second convert to miles per hour`() {
        assertEquals(2.236936, metresPerSecondTo(1.0, SpeedUnit.MILES_PER_HOUR), 1e-6)
        // 100 km/h is about 62.14 mph.
        assertEquals(62.137119, metresPerSecondTo(27.777778, SpeedUnit.MILES_PER_HOUR), 1e-5)
    }

    @Test
    fun `unit system maps to matching speed and distance units`() {
        assertEquals(SpeedUnit.KILOMETRES_PER_HOUR, UnitSystem.METRIC.speedUnit)
        assertEquals(DistanceUnit.KILOMETRES, UnitSystem.METRIC.distanceUnit)
        assertEquals(SpeedUnit.MILES_PER_HOUR, UnitSystem.IMPERIAL.speedUnit)
        assertEquals(DistanceUnit.MILES, UnitSystem.IMPERIAL.distanceUnit)
    }

    @Test
    fun `unknown or missing unit keys fall back to metric`() {
        assertEquals(UnitSystem.METRIC, UnitSystem.fromKey("metric"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromKey("imperial"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromKey(null))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromKey("nonsense"))
    }
}
