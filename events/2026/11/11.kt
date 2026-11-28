import kotlin.math.min

fun main() {
    println(
        part1(
            """
            portals: 10

            5 10
            2 7
            """.trimIndent()
        )
    )
    println(
        part2(
            """
            portals: 10

            5 10
            2 7
            """.trimIndent()
        )
    )
    println(
        part3(
            """
            portals: 32

            1 10
            20 30
            15 29
            13 10
            """.trimIndent()
        )
    )
    println(
        part3(
            """
            portals: 64

            1 40
            3 35
            5 42
            7 50
            """.trimIndent()
        )
    )
}

private class PortalRing(notes: String, reach: Int) {
    private val lines = notes.lineSequence().filter { it.isNotBlank() }.iterator()
    val size = lines.next().removePrefix("portals: ").toInt()
    val distances = Array(size) { i ->
        IntArray(size) { j ->
            val gap = kotlin.math.abs(i - j)
            (min(gap, size - gap) + reach - 1) / reach
        }
    }
    private val destinations = IntArray(size)
    val listedGain = lines.asSequence().sumOf { line ->
        val (u, v) = line.split(' ')
        shortcutGain(distances, u.toInt() - 1, v.toInt() - 1, apply = true)
    }

    fun shortcutGain(d: Array<IntArray>, u: Int, v: Int, apply: Boolean): Int {
        if (d[u][v] <= 1) return 0
        val fromU = d[u]
        val fromV = d[v]
        // A shorter route via u -> v must beat reaching v the old way at its
        // start, and beat leaving u the old way at its destination.
        var count = 0
        for (j in d.indices) {
            if (fromV[j] + 1 < fromU[j]) destinations[count++] = j
        }
        var gain = 0
        for (i in d.indices) {
            val via = fromU[i] + 1
            if (via >= fromV[i]) continue
            for (k in 0 until count) {
                val j = destinations[k]
                val shorter = via + fromV[j]
                if (shorter < d[i][j]) {
                    gain += d[i][j] - shorter
                    if (apply) {
                        d[i][j] = shorter
                        d[j][i] = shorter
                    }
                }
            }
        }
        return 2 * gain // Strain counts both directions.
    }
}

fun part1(notes: String): Int = PortalRing(notes, 1).listedGain

fun part2(notes: String): Int {
    val ring = PortalRing(notes, 3)
    var best = 0
    for (u in 0 until ring.size) {
        for (v in u + 1 until ring.size) {
            best = maxOf(best, ring.shortcutGain(ring.distances, u, v, apply = false))
        }
    }
    return ring.listedGain + best
}

fun part3(notes: String): Int {
    val ring = PortalRing(notes, 3)
    val spacing = ring.size / 8
    val used = BooleanArray(8)
    val scratch = Array(3) { Array(ring.size) { IntArray(ring.size) } }
    var best = -1

    fun pair(offset: Int, depth: Int, distances: Array<IntArray>, gain: Int) {
        // Fixing the first free crystal enumerates each matching exactly once.
        val a = used.indexOf(false)
        val u = offset + a * spacing
        used[a] = true
        for (b in a + 1 until 8) {
            val v = offset + b * spacing
            if (used[b] || distances[u][v] == 1) continue
            if (depth == 3) {
                best = maxOf(best, gain + ring.shortcutGain(distances, u, v, apply = false))
            } else {
                val next = scratch[depth]
                for (i in distances.indices) distances[i].copyInto(next[i])
                val added = ring.shortcutGain(next, u, v, apply = true)
                used[b] = true
                pair(offset, depth + 1, next, gain + added)
                used[b] = false
            }
        }
        used[a] = false
    }

    for (offset in 0 until spacing) pair(offset, 0, ring.distances, 0)
    check(best >= 0) { "No placement creates four new connections" }
    return ring.listedGain + best
}
