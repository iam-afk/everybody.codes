fun main() {
    println(
        part1(
            """
        ###########
        #####.#####
        ###.....###
        ##.......##
        ##.>>.<<.##
        #v...>...v#
        ##...v...##
        ##.......##
        ###.....###
        #####.#####
        ###########
    """.trimIndent()
        )
    )
    println(
        part2(
            """
        ###########
        #####.#####
        ###.....###
        ##.......##
        ##.>>.<<.##
        #v...>...v#
        ##...v...##
        ##.......##
        ###.....###
        #####.#####
        ###########
    """.trimIndent()
        )
    )
    println(
        part3(
            """
        #######################
        ###########.###########
        #######.^>.^.<^.#######
        #####..<.>.v^>>^^<#####
        ####..v<^v.>.>v..^.####
        ###.................###
        ###.................###
        ##.......<...<.<....>##
        ##...v....>....v.v<.^##
        ##........###........##
        ##v......#####.......##
        #.<......#####......>.#
        ##.......#####.......##
        ##........###........##
        ##...................##
        ##...................##
        ###.................###
        ###.................###
        ####...............####
        #####.............#####
        #######....v....#######
        ###########.###########
        #######################
    """.trimIndent()
        )
    )
}

private class Cave(notes: String, allowance: Long) {
    // Each state packs the cell index and two direction bits (up, right, down, left).
    private val move: IntArray
    private val occupancy: IntArray
    private val state: IntArray
    private val remaining: LongArray
    private var alive: Int

    init {
        val rows = notes.trimEnd().lines()
        val width = rows.first().length
        val cells = rows.joinToString("")
        val directions = "^>v<"
        val offsets = intArrayOf(-width, 1, width, -1)
        move = IntArray(cells.length * 4)
        occupancy = IntArray(cells.length)
        state = IntArray(cells.count { it in directions })
        remaining = LongArray(state.size) { allowance }
        alive = 0
        for (cell in cells.indices) {
            if (cells[cell] == '#') continue
            for (dir in offsets.indices) {
                val next = cell + offsets[dir]
                move[4 * cell + dir] = if (cells[next] == '#') {
                    4 * cell + (dir + 1) % 4
                } else {
                    4 * next + dir
                }
            }
            val dir = directions.indexOf(cells[cell])
            if (dir >= 0 && allowance > 0) state[alive++] = 4 * cell + dir
        }
    }

    fun step(): Int {
        var hits = 0
        occupancy.fill(0)
        for (i in 0 until alive) {
            val old = state[i]
            val next = move[old]
            state[i] = next
            if (old / 4 == next / 4) {
                hits++
                if (--remaining[i] == 0L) continue // Gone before ghosts on this cell collide.
            }
            occupancy[next / 4]++
        }

        var survivors = 0
        for (i in 0 until alive) {
            if (remaining[i] == 0L) continue
            var next = state[i]
            if (occupancy[next / 4] > 1) {
                hits++
                if (--remaining[i] == 0L) continue
                next = next xor 2
            }
            state[survivors] = next
            remaining[survivors++] = remaining[i]
        }
        alive = survivors
        return hits
    }

    fun lastRound(): Long {
        val savedState = state.copyOf()
        val savedRemaining = remaining.copyOf()
        var savedRound = 0L
        var window = 1L
        var round = 0L
        var skipped = false
        while (alive > 0) {
            val previousAlive = alive
            step()
            round++
            if (alive != previousAlive) {
                // A disappearance changes the dynamics and the indices of the survivors.
                window = 1L
                skipped = false
            } else {
                if (skipped) continue
                val period = round - savedRound
                if ((0 until alive).all { state[it] == savedState[it] }) {
                    var cycles = Long.MAX_VALUE
                    for (i in 0 until alive) {
                        val loss = savedRemaining[i] - remaining[i]
                        if (loss > 0) cycles = minOf(cycles, (remaining[i] - 1) / loss)
                    }
                    // Leave every ghost alive: play out the first death in round order.
                    round += cycles * period
                    for (i in 0 until alive) remaining[i] -= cycles * (savedRemaining[i] - remaining[i])
                    skipped = true
                    continue
                }
                // Doubling the checkpoint interval detects a cycle without keeping its history.
                if (period < window) continue
                window *= 2
            }
            savedRound = round
            state.copyInto(savedState, endIndex = alive)
            remaining.copyInto(savedRemaining, endIndex = alive)
        }
        return round
    }
}

fun part1(notes: String): Int {
    val cave = Cave(notes, Long.MAX_VALUE)
    return (1..10).sumOf { cave.step() }
}

fun part2(notes: String, disappear: Int = 10_000): Int = Cave(notes, disappear.toLong()).lastRound().toInt()

fun part3(notes: String, disappear: Long = 1_000_000_000_000): Long = Cave(notes, disappear).lastRound()
