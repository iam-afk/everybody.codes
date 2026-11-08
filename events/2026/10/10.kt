@file:OptIn(ExperimentalStdlibApi::class)

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

data class P(val row: Int, val col: Int) {
    operator fun plus(d: D) = P(row + d.dr, col + d.dc)
}

data class D(val dr: Int, val dc: Int) {
    fun turn90() = D(dc, -dr)
    fun turn180() = D(-dr, -dc)
}

data class Ghost(var p: P, var d: D, var c: Long = 0) {
    fun move(): P = p + d
}

operator fun List<String>.get(p: P): Char = this[p.row][p.col]

fun parseGhosts(map: List<String>) = buildList {
    for (row in map.indices) {
        for (col in map[row].indices) {
            when (map[row][col]) {
                '>' -> add(Ghost(P(row, col), D(0, 1)))
                'v' -> add(Ghost(P(row, col), D(1, 0)))
                '<' -> add(Ghost(P(row, col), D(0, -1)))
                '^' -> add(Ghost(P(row, col), D(-1, 0)))
            }
        }
    }
}

fun part1(notes: String): Int {
    val map = notes.lines()
    val ghosts = parseGhosts(map)

    var collisions = 0
    repeat(10) {
        val after = buildMap {
            for (g in ghosts) {
                val n = g.move()
                if (map[n] == '#') {
                    g.d = g.d.turn90()
                    getOrPutIfMissing(g.p) { mutableListOf() }.add(g)
                    collisions += 1
                } else {
                    g.p = n
                    getOrPutIfMissing(g.p) { mutableListOf() }.add(g)
                }
            }
        }
        for ((_, ghosts) in after.filter { (_, ghosts) -> ghosts.size > 1 }) {
            for (g in ghosts) {
                g.d = g.d.turn180()
                collisions += 1
            }
        }
    }

    return collisions
}

fun part2(notes: String, disappear: Int = 10_000): Int {
    val map = notes.lines()
    val ghosts = parseGhosts(map)

    var round = 0
    while (ghosts.any { it.c < disappear }) {
        val after = buildMap {
            for (g in ghosts.filter { it.c < disappear }) {
                val n = g.move()
                if (map[n] == '#') {
                    g.d = g.d.turn90()
                    g.c += 1
                    if (g.c < disappear) {
                        getOrPutIfMissing(g.p) { mutableListOf() }.add(g)
                    }
                } else {
                    g.p = n
                    getOrPutIfMissing(g.p) { mutableListOf() }.add(g)
                }
            }
        }
        for ((_, ghosts) in after.filter { (_, ghosts) -> ghosts.size > 1 }) {
            for (g in ghosts) {
                g.d = g.d.turn180()
                g.c += 1
            }
        }
        round += 1
    }

    return round
}

fun part3(notes: String, disappear: Long = 1_000_000_000_000): Long {
    val map = notes.lines()
    val ghosts = parseGhosts(map)

    fun alive() = ghosts.filter { it.c < disappear }
    fun state() = alive().map { it.copy(c = 0) }

    data class S(val round: Long, val collisions: List<Long>)

    val states = mutableMapOf<List<Ghost>, S>()

    var round = 0L
    while (alive().isNotEmpty()) {
        val prev = states.put(state(), S(round, alive().map { it.c }))
        if (prev != null) {
            val skip = prev.collisions.zip(alive().map { it.c }).minOf { (p, c) -> (disappear - 1 - c) / (c - p) }
            round += skip * (round - prev.round)
            for ((g, p) in alive().zip(prev.collisions)) {
                g.c += skip * (g.c - p)
            }
            states.clear()
        }

        var disappeared = false
        val after = buildMap {
            for (g in alive()) {
                val n = g.move()
                if (map[n] == '#') {
                    g.d = g.d.turn90()
                    g.c += 1
                    if (g.c < disappear) {
                        getOrPutIfMissing(g.p) { mutableListOf() }.add(g)
                    } else {
                        disappeared = true
                    }
                } else {
                    g.p = n
                    getOrPutIfMissing(g.p) { mutableListOf() }.add(g)
                }
            }
        }
        for ((_, ghosts) in after.filter { (_, ghosts) -> ghosts.size > 1 }) {
            for (g in ghosts) {
                g.d = g.d.turn180()
                g.c += 1
                if (g.c >= disappear)
                    disappeared = true
            }
        }
        round += 1

        if (disappeared) {
            states.clear()
        }
    }

    return round
}