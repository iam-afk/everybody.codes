fun main() {
    println(
        part3(
            """
        id=1, growth=3, limit=10
        id=2, growth=5, limit=20
        id=3, growth=7, limit=30
    """.trimIndent()
        )
    )
}

fun part1(notes: String): Int = notes.lines().sumOf { line ->
    val (_, growth, limit) = line.split(", ").map { it.substringAfter('=').toInt() }
    var a = mapOf(0 to 1)
    var t = 0
    var branches = 0
    while (branches + a.entries.sumOf { (k, v) -> k * v } < limit) {
        a = buildMap {
            for ((k, v) in a) {
                if (k + 1 == growth) {
                    compute(k / 2) { _, n -> (n ?: 0) + 2 * v }
                    compute(1) { _, n -> (n ?: 0) + v }
                    branches += v
                } else {
                    compute(k + 1) { _, n -> (n ?: 0) + v }
                }
            }
        }
        t += 1
    }
    t
}

fun part2(notes: String): Int {
    return notes.lines().sumOf { line ->
        val (_, growth, limit) = line.split(", ").map { it.substringAfter('=').toInt() }
        var a = mapOf(0 to 1)
        var t = 0
        var leaves = 1
        while (leaves < limit) {
            a = buildMap {
                for ((k, v) in a) {
                    if (k + 1 == growth) {
                        compute(k / 2) { _, n -> (n ?: 0) + 2 * v }
                        compute(1) { _, n -> (n ?: 0) + v }
                        leaves += v
                    } else {
                        compute(k + 1) { _, n -> (n ?: 0) + v }
                    }
                }
            }
            t += 1
        }
        t
    }
}

fun part3(notes: String): Long {
    return notes.lines().sumOf { line ->
        val (_, growth, limit) = line.split(", ").map { it.substringAfter('=').toLong() }
        var a = mapOf(1L to 1L)
        var t = 1L
        var leaves = 1L
        while (leaves < limit) {
            val mx = a.keys.max()
            t += growth - mx
            a = buildMap {
                for ((k, v) in a) {
                    if (k == mx) {
                        compute(growth / 2) { _, n -> (n ?: 0) + 2 * v }
                        compute(1L) { _, n -> (n ?: 0) + v }
                        leaves += v
                    } else {
                        compute(k + growth - mx) { _, n -> (n ?: 0) + v }
                    }
                }
            }
        }
        t
    }
}

