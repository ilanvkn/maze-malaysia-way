package my.maze.malaysia

/** Pure policy: monotonic fix age, permission-aware quality, no wall-clock assumptions. */
internal data class FixCandidate(
    val point: GeoPoint,
    val accuracyMeters: Float?,
    val elapsedNanos: Long,
    val provider: String,
    val mock: Boolean = false
)
internal fun usableFix(fix: FixCandidate, nowNanos: Long, precise: Boolean): Boolean {
    val accuracy = fix.accuracyMeters ?: return false
    if (!accuracy.isFinite() || accuracy <= 0 || accuracy > if (precise) 250f else 5000f) return false
    if (fix.elapsedNanos <= 0 || fix.elapsedNanos > nowNanos) return false
    return nowNanos - fix.elapsedNanos <= 30_000_000_000L && inMalaysiaRegion(fix.point)
}
internal fun preferredProviders(precise: Boolean, enabled: Set<String>): List<String> =
    (if (precise) listOf("gps", "network") else listOf("network")) .filter { it in enabled }

internal fun fixDescription(fix: FixCandidate, precise: Boolean): String = buildString {
    append(if (precise) "Reported accuracy ±" else "Approximate location · reported accuracy ±")
    append(kotlin.math.ceil(fix.accuracyMeters!!.toDouble()).toInt())
    append(" m · one foreground fix")
    if (fix.mock) append(" · simulated location")
}
