import java.math.BigInteger
import java.util.PriorityQueue

fun main() {
    val example = """
        #####################################################
        #.@.........#................#.....................##
        #.#########@#.##############.#.###################.##
        #.#.......#.#.##..........##.#.###..@..@......@###.##
        #.#.#####.#.#.##.########.##.#.###@###########.###.##
        #.#.#@@@#.#.#.##.##....##.##@#.###.###.....###.###.##
        #.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###.##
        S...................................................E
        #.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###@##
        #.#.#...#.#.#.##@##.@..##.##.#.###.###@....###.###.##
        #.#.#####.#.#.##.########.##.#.###.###########.###.##
        #.#.......#.#.##..@.......##.#.###.............###.##
        #.#########.#.##############.#.###################.##
        #...........#................#................@....##
        #####################################################
    """.trimIndent()
    val exactExample = """
        #####################################################
        #.@.........#................#..........@..........##
        #.#########@#.##############.#.###################.##
        #.#...@...#.#.##.....@....##.#.###..@..@......@###.##
        #.#.#####.#.#.##.########.##.#.###@###########.###.##
        #.#.#@@@#.#.#.##.##..@.##.##@#.###.###..@..###.###.##
        #.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###.##
        S...................................................E
        #.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###@##
        #.#.#.@.#.#.#.##@##.@..##.##.#.###.###@....###.###.##
        #.#.#####.#.#.##.########.##.#.###.###########.###.##
        #.#...@...#.#.##..@.......##.#.###......@......###.##
        #.#########.#.##############.#.###################.##
        #.....@.....#........@.......#................@....##
        #####################################################
    """.trimIndent()
    println(part1(example).also { check(it == 184) })
    println(part2(example).also { check(it == 5448) })
    println(part3(exactExample).also { check(it == 540320698784L) })
}

private data class Move(val to: Int, val time: Int)

// Stop 0 is S. Every other stop is a deposit paired with the direction of arrival.
private class Stops(notes: String) {
    val moves = mutableListOf<List<Move>>()
    val deposit = mutableListOf<Int>()
    val finish = mutableListOf<Int>() // time to E without collecting another unit, or -1
    val count: Int

    init {
        val rows = notes.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val stride = rows.maxOf { it.length } + 2
        val grid = CharArray((rows.size + 2) * stride) { '#' }
        rows.forEachIndexed { r, row -> row.toCharArray(grid, (r + 1) * stride + 1) }
        val offset = intArrayOf(-stride, 1, stride, -1)
        val depositId = IntArray(grid.size) { -1 }
        var ids = 0
        for (cell in grid.indices) if (grid[cell] == '@') depositId[cell] = ids++
        count = ids

        val initial = grid.size * 4 // no incoming direction at S
        val states = mutableListOf(initial)
        deposit.add(-1)
        val stopId = IntArray(initial) { -1 }
        val seen = IntArray(initial) { -1 }
        val queue = IntArray(initial + 1)
        val startCell = grid.indexOf('S')
        var stop = 0
        while (stop < states.size) {
            val edges = mutableListOf<Move>()
            var exit = -1
            var head = 0
            var tail = 1
            queue[0] = states[stop]
            var time = 0
            while (head < tail) {
                val layerEnd = tail
                time++
                while (head < layerEnd) {
                    val state = queue[head++]
                    val cell = if (state == initial) startCell else state / 4
                    for (dir in offset.indices) {
                        if (state != initial && dir == ((state % 4) xor 2)) continue
                        val next = cell + offset[dir]
                        if (grid[next] == '#') continue
                        val arrival = next * 4 + dir
                        if (seen[arrival] == stop) continue
                        seen[arrival] = stop
                        if (grid[next] == '@') {
                            if (stopId[arrival] < 0) {
                                stopId[arrival] = states.size
                                states.add(arrival)
                                deposit.add(depositId[next])
                            }
                            edges.add(Move(stopId[arrival], time))
                        } else {
                            if (grid[next] == 'E' && exit < 0) exit = time
                            queue[tail++] = arrival
                        }
                    }
                }
            }
            moves.add(edges)
            finish.add(exit)
            stop++
        }
    }
}

fun part1(notes: String): Int {
    if (notes.isBlank()) return 0
    val stops = Stops(notes)

    data class Visit(val stop: Int, val remaining: BigInteger, val time: Int)

    val all = BigInteger.ONE.shiftLeft(stops.count) - BigInteger.ONE
    val best = Array(stops.moves.size) { HashMap<BigInteger, Int>() }
    val queue = PriorityQueue(compareBy<Visit> { it.time })
    best[0][all] = 0
    queue.add(Visit(0, all, 0))
    var answer = Int.MAX_VALUE
    while (queue.isNotEmpty()) {
        val (stop, remaining, time) = queue.remove()
        if (time >= answer) break
        if (time != best[stop][remaining]) continue
        val finish = stops.finish[stop]
        if (remaining.signum() == 0 && finish >= 0) answer = minOf(answer, time + finish)
        for ((to, cost) in stops.moves[stop]) {
            val mask = remaining.clearBit(stops.deposit[to])
            val next = time + cost
            if (next < (best[to][mask] ?: answer)) {
                best[to][mask] = next
                queue.add(Visit(to, mask, next))
            }
        }
    }
    return if (answer == Int.MAX_VALUE) -1 else answer
}

private const val INF = Long.MAX_VALUE / 2

private fun collect(notes: String, target: Long, exact: Boolean): Long {
    val stops = Stops(notes)
    // time[i] is the fastest finish from stop i for the current number of units still needed.
    var time = LongArray(stops.moves.size) { i ->
        val finish = stops.finish[i]
        if (finish < 0) INF else finish.toLong()
    }
    var next = LongArray(time.size)
    fun advance(allowExtra: Boolean) {
        for (i in time.indices) {
            var best = if (allowExtra) time[i] else INF
            for (move in stops.moves[i]) best = minOf(best, time[move.to] + move.time)
            next[i] = best
        }
        val old = time
        time = next
        next = old
    }
    if (!exact) {
        // With the quota already met, the final leg may pass through more deposits.
        do {
            advance(true)
        } while (!time.contentEquals(next))
    }

    var units = 0L
    var checkpoint = time.copyOf()
    var checkpointUnits = 0L
    var skippedTime = 0L
    while (units < target) {
        advance(false)
        units++
        val pivot = time.indexOfFirst { it != INF }
        if (pivot < 0) return -1
        // The recurrence commutes with a uniform time shift. If the whole vector
        // repeats up to that shift, all subsequent periods are identical, including unreachable stops.
        val shift = time[pivot] - checkpoint[pivot]
        if (checkpoint[pivot] != INF && time.indices.all { i ->
                if (time[i] == INF || checkpoint[i] == INF) time[i] == checkpoint[i]
                else time[i] - checkpoint[i] == shift
            }) {
            val period = units - checkpointUnits
            val repeats = (target - units) / period
            skippedTime = repeats * shift
            units += repeats * period
            while (units < target) {
                advance(false)
                units++
            }
            break
        }
        // Exponentially spaced checkpoints detect a periodic tail with only one saved vector.
        if ((units and (units - 1)) == 0L) {
            checkpoint = time.copyOf()
            checkpointUnits = units
        }
    }
    return if (time[0] == INF) -1 else time[0] + skippedTime
}

fun part2(notes: String): Int = if (notes.isBlank()) 0 else collect(notes, 2026, exact = false).toInt()

fun part3(notes: String): Long = if (notes.isBlank()) -1 else collect(notes, 202_620_262_026L, exact = true)
