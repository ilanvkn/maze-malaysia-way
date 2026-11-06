package my.maze.malaysia

import org.junit.Assert.*
import org.junit.Test

class MazeLocationPolicyTest {
    private val now = 100_000_000_000L
    private val good = FixCandidate(landmarks[0].point, 20f, now - 2_000_000_000L, "gps")
    @Test fun approximatePermissionNeverSelectsGps() {
        assertEquals(listOf("network"), preferredProviders(false, setOf("gps", "network")))
        assertTrue(preferredProviders(false, setOf("gps")).isEmpty())
        assertEquals(listOf("gps", "network"), preferredProviders(true, setOf("network", "gps")))
        assertTrue(preferredProviders(true, emptySet()).isEmpty())
    }
    @Test fun rejectsStaleFutureMissingAndNonfiniteAccuracy() {
        assertTrue(usableFix(good, now, true))
        assertFalse(usableFix(good.copy(elapsedNanos = now - 30_000_000_001L), now, true))
        assertFalse(usableFix(good.copy(elapsedNanos = now + 1), now, true))
        assertFalse(usableFix(good.copy(elapsedNanos = 0), now, true))
        for (bad in listOf(null, Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 251f))
            assertFalse(usableFix(good.copy(accuracyMeters = bad), now, true))
    }
    @Test fun coarseQualityIsExplicitAndOutOfRegionFixIsRejected() {
        assertTrue(usableFix(good.copy(accuracyMeters = 3000f), now, false))
        assertFalse(usableFix(good.copy(accuracyMeters = 5001f), now, false))
        assertFalse(usableFix(good.copy(point = GeoPoint(51.5, -.1)), now, true))
        assertTrue(fixDescription(good, true).contains("±20 m"))
        assertTrue(fixDescription(good.copy(mock = true), false).contains("simulated"))
        assertTrue(fixDescription(good, false).startsWith("Approximate"))
    }
}
