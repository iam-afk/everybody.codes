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

fun part1(notes: String): Int {
    val lines = notes.lines()
    val n = lines.first().removePrefix("portals: ").toInt()

    val strainBefore = n * (1..n).sumOf { min(it, n - it) }

    val d = Array(n) { IntArray(n) { Int.MAX_VALUE / 2 } }
    (0..<n).forEach {
        d[it][it] = 0
        d[it][(it + 1).mod(n)] = 1
        d[(it + 1).mod(n)][it] = 1
    }
    for (line in lines.drop(2)) {
        val (v, u) = line.split(' ').map { it.toInt() }.map { it - 1 }
        d[v][u] = 1
        d[u][v] = 1
    }

    (0..<n).forEach { k ->
        (0..<n).forEach { i ->
            (0..<n).forEach { j ->
                d[i][j] = min(d[i][j], d[i][k] + d[k][j])
            }
        }
    }

    val strainAfter = d.sumOf { it.sum() }

    return strainBefore - strainAfter
}

fun part2(notes: String): Int {
    val lines = notes.lines()
    val n = lines.first().removePrefix("portals: ").toInt()

    fun make(): Array<IntArray> {
        val d = Array(n) { IntArray(n) { Int.MAX_VALUE / 2 } }
        (0..<n).forEach {
            d[it][it] = 0
            d[(it + 1).mod(n)][it] = 1
            d[(it + 2).mod(n)][it] = 1
            d[(it + 3).mod(n)][it] = 1
            d[it][(it + 1).mod(n)] = 1
            d[it][(it + 2).mod(n)] = 1
            d[it][(it + 3).mod(n)] = 1
        }
        return d
    }

    fun Array<IntArray>.calc(): Int {
        (0..<n).forEach { k ->
            (0..<n).forEach { i ->
                (0..<n).forEach { j ->
                    this[i][j] = min(this[i][j], this[i][k] + this[k][j])
                }
            }
        }
        return this.sumOf { it.sum() }
    }

    val strainBefore = make().calc()

    val d = make()
    for (line in lines.drop(2)) {
        val (v, u) = line.split(' ').map { it.toInt() }.map { it - 1 }
        d[v][u] = 1
        d[u][v] = 1
    }

    var strainAfter = strainBefore
    (0..<n).forEach { i ->
        (i + 1..<n).forEach { j ->
            if (d[i][j] != 1) {
                val e = Array(n) { d[it].clone() }
                e[i][j] = 1
                e[j][i] = 1
                strainAfter = min(strainAfter, e.calc())
            }
        }
    }

    return strainBefore - strainAfter
}

fun part3(notes: String): Int {
    val lines = notes.lines()
    val n = lines.first().removePrefix("portals: ").toInt()

    fun make(): Array<IntArray> {
        val d = Array(n) { IntArray(n) { Int.MAX_VALUE / 2 } }
        (0..<n).forEach {
            d[it][it] = 0
            d[(it + 1).mod(n)][it] = 1
            d[(it + 2).mod(n)][it] = 1
            d[(it + 3).mod(n)][it] = 1
            d[it][(it + 1).mod(n)] = 1
            d[it][(it + 2).mod(n)] = 1
            d[it][(it + 3).mod(n)] = 1
        }
        return d
    }

    fun Array<IntArray>.calc(): Int {
        (0..<n).forEach { k ->
            (0..<n).forEach { i ->
                (0..<n).forEach { j ->
                    this[i][j] = min(this[i][j], this[i][k] + this[k][j])
                }
            }
        }
        return this.sumOf { it.sum() }
    }

    val strainBefore = make().calc()

    val d = make()
    for (line in lines.drop(2)) {
        val (v, u) = line.split(' ').map { it.toInt() }.map { it - 1 }
        d[v][u] = 1
        d[u][v] = 1
    }

    val ps = buildSet {
        (0..<8).forEach { a1 ->
            (a1 + 1..<8).forEach { b1 ->
                (0..<8).filterNot { it == a1 || it == b1 }.forEach { a2 ->
                    (a2 + 1..<8).filterNot { it == a1 || it == b1 }.forEach { b2 ->
                        (0..<8).filterNot { it == a1 || it == b1 || it == a2 || it == b2 }.forEach { a3 ->
                            (a3 + 1..<8).filterNot { it == a1 || it == b1 || it == a2 || it == b2 }.forEach { b3 ->
                                (0..<8).filterNot { it == a1 || it == b1 || it == a2 || it == b2 || it == a3 || it == b3 }
                                    .forEach { a4 ->
                                        (a4 + 1..<8).filterNot { it == a1 || it == b1 || it == a2 || it == b2 || it == a3 || it == b3 }
                                            .forEach { b4 ->
                                                add(setOf(a1 to b1, a2 to b2, a3 to b3, a4 to b4))
                                            }
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    var strainAfter = strainBefore
    (0..<n).forEach { i ->
        val vs = IntArray(8) { (i + it * n / 8).mod(n) }
        for (p in ps) {
            val e = Array(n) { d[it].clone() }
            var skip = false
            for ((v, u) in p) {
                skip = e[vs[v]][vs[u]] == 1
                e[vs[v]][vs[u]] = 1
                e[vs[u]][vs[v]] = 1
            }
            if (!skip) {
                strainAfter = min(strainAfter, e.calc())
            }
        }
    }

    return strainBefore - strainAfter
}
