package my.maze.malaysia

import java.util.PriorityQueue

enum class RouteSearchMode(val weight: Double, val label: String) {
    QUICK(8.0, "Quick preview"), CAREFUL(1.0, "Careful A* · may take longer")
}
internal data class SearchArc<K, E>(val to: K, val seconds: Double, val edge: E)
internal data class SearchResult<E>(val edges: List<E>, val seconds: Double, val expanded: Int)
internal class SearchBudgetExceeded : IllegalStateException("Route search reached its time or state budget. Try a shorter trip or Quick preview.")

/** Turn state belongs in K; a node-only visited set is incorrect for turn restrictions.
 * Weight 1 is optimal on this graph only when h is a lower bound and the search finishes.
 * Weight >1 trades that guarantee for a bounded preview. No traffic claims follow.
 */
internal fun <K, E> boundedAStar(
    start: K,
    isGoal: (K) -> Boolean,
    outgoing: (K) -> List<SearchArc<K, E>>,
    heuristic: (K) -> Double,
    weight: Double,
    maxExpanded: Int,
    maxDiscovered: Int,
    deadlineMillis: Long,
    nowMillis: () -> Long,
    checkpoint: () -> Unit = {}
): SearchResult<E>? {
    require(weight.isFinite() && weight >= 1 && maxExpanded > 0 && maxDiscovered > 0)
    data class Entry<K>(val key: K, val cost: Double, val score: Double, val order: Long)
    val frontier = PriorityQueue<Entry<K>>(compareBy<Entry<K>> { it.score }.thenBy { it.order })
    val costs = hashMapOf(start to 0.0)
    val previous = HashMap<K, Pair<K, E>>()
    var sequence = 0L
    fun estimate(key: K): Double = heuristic(key).also { require(it.isFinite() && it >= 0) }
    frontier.add(Entry(start, 0.0, weight * estimate(start), sequence++))
    var expanded = 0
    while (frontier.isNotEmpty()) {
        checkpoint()
        if (nowMillis() >= deadlineMillis) throw SearchBudgetExceeded()
        val current = frontier.remove()
        if (current.cost != costs[current.key]) continue
        if (isGoal(current.key)) {
            val path = mutableListOf<E>()
            var cursor = current.key
            while (cursor != start) {
                val (parent, edge) = previous.getValue(cursor)
                path.add(edge)
                cursor = parent
            }
            path.reverse()
            return SearchResult(path, current.cost, expanded)
        }
        if (expanded >= maxExpanded) throw SearchBudgetExceeded()
        expanded++
        for (arc in outgoing(current.key)) {
            checkpoint()
            require(arc.seconds.isFinite() && arc.seconds > 0) { "Invalid road cost" }
            val cost = current.cost + arc.seconds
            if (cost < (costs[arc.to] ?: Double.POSITIVE_INFINITY)) {
                if (arc.to !in costs && costs.size >= maxDiscovered) throw SearchBudgetExceeded()
                // Bound queued stale entries as well as discovered states.
                if (frontier.size >= maxDiscovered * 2L) throw SearchBudgetExceeded()
                costs[arc.to] = cost
                previous[arc.to] = current.key to arc.edge
                frontier.add(Entry(arc.to, cost, cost + weight * estimate(arc.to), sequence++))
            }
        }
    }
    return null
}

/** Per-request bounded cache; avoids repeating SQLite reads for different turn states. */
internal class RouteCache<K, V>(private val limit: Int) : LinkedHashMap<K, V>(limit, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > limit
}
