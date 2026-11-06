package my.maze.malaysia

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class MazeAStarTest {
    private fun search(weight: Double = 1.0, maxStates: Int = 100, clock: () -> Long = { 0 }, checkpoint: () -> Unit = {}): SearchResult<String>? {
        // Fast-looking direct branch costs 11; the true best path costs 4.
        val graph = mapOf("s" to listOf(SearchArc("a", 1.0, "sa"), SearchArc("b", 2.0, "sb")),
            "a" to listOf(SearchArc("g", 10.0, "ag")), "b" to listOf(SearchArc("g", 2.0, "bg")))
        return boundedAStar("s", {it == "g"}, {graph[it].orEmpty()},
            {if (it == "b") 2.0 else 0.0}, weight, maxStates, 100, 100, clock, checkpoint)
    }
    @Test fun admissibleAStarFindsBestWhileWeightedPreviewMayDetour() {
        assertEquals(listOf("sb", "bg"), search()!!.edges)
        assertEquals(4.0, search()!!.seconds, 0.0)
        assertEquals(listOf("sa", "ag"), search(8.0)!!.edges)
    }
    @Test fun budgetsAndCancellationAreDistinctFromDisconnected() {
        assertThrows(SearchBudgetExceeded::class.java) { search(maxStates = 1) }
        assertThrows(SearchBudgetExceeded::class.java) { search(clock = {100}) }
        assertThrows(CancellationException::class.java) { search(checkpoint = {throw CancellationException()}) }
        assertNull(boundedAStar<Int, Int>(0, {it == 9}, {emptyList()}, {0.0}, 1.0, 100, 100, 100, {0}))
    }
    @Test fun turnStateMustNotCollapseTwoApproachesToOneNode() {
        data class State(val node: Int, val approach: Int)
        val s = State(0, 0); val wrong = State(1, 10); val right = State(1, 20); val g = State(2, 30)
        val result = boundedAStar(s, {it.node == 2}, {when(it) {
            s -> listOf(SearchArc(wrong, 1.0, "wrong"), SearchArc(right, 2.0, "right"))
            right -> listOf(SearchArc(g, 1.0, "legal turn"))
            else -> emptyList()
        }}, {0.0}, 1.0, 20, 100, 100, {0})!!
        assertEquals(listOf("right", "legal turn"), result.edges)
    }
    @Test fun rejectsInvalidRoadCostsAndBoundsDiscovery() {
        for (bad in listOf(-1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertThrows(IllegalArgumentException::class.java) {
                boundedAStar(0, {it == 2}, {listOf(SearchArc(1, bad, 1))}, {0.0}, 1.0, 10, 10, 100, {0})
            }
        assertThrows(SearchBudgetExceeded::class.java) {
            boundedAStar(0, {it == 2}, {listOf(SearchArc(1, 1.0, 1))}, {0.0}, 1.0, 10, 1, 100, {0})
        }
    }
    @Test fun cacheEvictsLeastRecentlyUsedAndDoesNotGrow() {
        val cache = RouteCache<Int, String>(2)
        cache[1] = "one"; cache[2] = "two"; assertEquals("one", cache[1]); cache[3] = "three"
        assertEquals(2, cache.size); assertFalse(cache.containsKey(2)); assertTrue(cache.containsKey(1))
    }
    @Test fun zeroHeuristicAgreesWithIndependentAllPairsBaseline() {
        val random = java.util.Random(721L)
        repeat(30) {
            val n = 8
            val costs = Array(n) { a -> DoubleArray(n) { b -> if (a == b) 0.0 else Double.POSITIVE_INFINITY } }
            val graph = Array(n) { mutableListOf<SearchArc<Int, Pair<Int, Int>>>() }
            for (a in 0 until n) for (b in 0 until n) if (a != b && random.nextBoolean()) {
                val cost = 1.0 + random.nextInt(20)
                costs[a][b] = cost
                graph[a].add(SearchArc(b, cost, a to b))
            }
            // Floyd–Warshall is an independent baseline, with no priority queue or heuristic.
            for (k in 0 until n) for (a in 0 until n) for (b in 0 until n)
                costs[a][b] = minOf(costs[a][b], costs[a][k] + costs[k][b])
            val route = boundedAStar(0, {it == n - 1}, {graph[it]}, {0.0}, 1.0, 100, 100, 100, {0})
            if (costs[0][n-1].isInfinite()) assertNull(route)
            else assertEquals(costs[0][n-1], route!!.seconds, 0.0)
        }
    }

}
