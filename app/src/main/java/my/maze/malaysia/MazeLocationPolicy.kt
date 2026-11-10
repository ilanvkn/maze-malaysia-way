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

/** A bounded foreground check, never a smoothed or road-snapped position. */
internal data class FixAssessment(
    val fix: FixCandidate, val samples: Int, val spreadMeters: Double,
    val uncertaintyMeters: Float, val consistent: Boolean, val simulated: Boolean
)
internal class FixWindow(private val precise: Boolean, private val startedNanos: Long) {
    private val samples = mutableListOf<FixCandidate>()
    private val lastProviderTime = mutableMapOf<String, Long>()
    fun offer(fix: FixCandidate, nowNanos: Long): Boolean {
        if (fix.provider !in preferredProviders(precise, setOf("gps", "network")) ||
            !usableFix(fix, nowNanos, precise) || fix.elapsedNanos < startedNanos ||
            nowNanos - fix.elapsedNanos > 5_000_000_000L) return false
        val last = lastProviderTime[fix.provider]
        if (last != null && fix.elapsedNanos - last < 500_000_000L) return false
        lastProviderTime[fix.provider] = fix.elapsedNanos
        samples.add(fix); samples.sortBy { it.elapsedNanos }
        samples.removeAll { nowNanos - it.elapsedNanos > 10_000_000_000L }
        while (samples.size > 8) samples.removeAt(0)
        return true
    }
    fun assessment(nowNanos: Long): FixAssessment? {
        val recent = samples.filter { nowNanos >= it.elapsedNanos && nowNanos - it.elapsedNanos <= 10_000_000_000L }
        val latest = recent.lastOrNull() ?: return null
        if (nowNanos - latest.elapsedNanos > 5_000_000_000L) return null
        var spread = 0.0
        for (a in recent) for (b in recent) spread = maxOf(spread, groundDistance(a.point, b.point))
        // Enclose each retained reported radius around the latest real sample. Do not average
        // points across a median or reduce accuracy by sqrt(n): these errors may be correlated.
        val envelope = recent.maxOf { groundDistance(latest.point, it.point) + it.accuracyMeters!! }
        if (!envelope.isFinite() || envelope > 5000) return null
        val consistent = recent.size >= 3 && latest.elapsedNanos - recent.first().elapsedNanos >= 2_000_000_000L &&
            recent.map { it.provider }.distinct().size == 1 &&
            spread <= maxOf(12.0, recent.minOf { it.accuracyMeters!! }.toDouble())
        return FixAssessment(latest, recent.size, spread, kotlin.math.ceil(envelope).toFloat(), consistent, recent.any { it.mock })
    }
    fun ready(nowNanos: Long): Boolean = assessment(nowNanos)?.let {
        precise && it.consistent && it.uncertaintyMeters <= 50f
    } ?: false
}
internal fun estimateDescription(estimate: FixAssessment, precise: Boolean): String = buildString {
    fun meters(value: Double) = kotlin.math.ceil(value).toInt()
    if (!precise) append("Approximate location · ")
    append("Location estimate ±${meters(estimate.uncertaintyMeters.toDouble())} m")
    append(" · reported ±${meters(estimate.fix.accuracyMeters!!.toDouble())} m")
    append(" · observed spread ${meters(estimate.spreadMeters)} m · ${estimate.samples} samples")
    append(if (estimate.consistent) " · consistent samples" else " · road uncertain")
    if (estimate.simulated) append(" · simulated location")
}

/** A foreground position is a snapshot, not live navigation. Provisional two-minute review budget. */
internal fun expiredLocationSnapshot(elapsedNanos:Long?,nowNanos:Long):Boolean = elapsedNanos==null || elapsedNanos<=0 || elapsedNanos>nowNanos || nowNanos-elapsedNanos>120_000_000_000L
