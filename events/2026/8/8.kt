import kotlin.math.abs

data class Hex(var q: Int, var r: Int) {
    val s: Int get() = -q - r
    fun toDoubledWidth() = DoubledCoord(2 * q + r, r)
    fun distance(other: Hex): Int = (abs(q - other.q) + abs(r - other.r) + abs(s - other.s)) / 2
    operator fun plus(other: Hex): Hex = Hex(q + other.q, r + other.r)
    operator fun times(other: Int): Hex = Hex(q * other, r * other)
    operator fun plusAssign(other: Hex) {
        q += other.q
        r += other.r
    }
}

data class DoubledCoord(val col: Int, val row: Int)

val dirs = arrayOf(
    Hex(0, 1), Hex(-1, 1), Hex(-1, 0), Hex(0, -1), Hex(1, -1), Hex(1, 0)
)

operator fun List<List<Int?>>.get(hex: Hex): Int {
    val dw = hex.toDoubledWidth()
    return this[dw.row + size / 2][dw.col + size / 2 * 2]!!
}

fun energy(field: List<List<Int?>>, dirIndex: Int): List<Hex> {
    val ans = mutableListOf<Hex>()

    val n = field.size / 2

    val center = Hex(0, 0)

    fun ray(hex: Hex) {
        var shadow = 0
        while (center.distance(hex) <= n) {
            val v = field[hex]
            if (v > 9)
                ans.add(hex.copy())
            else if (shadow < v) {
                shadow = v
                ans.add(hex.copy())
            }
            shadow -= 1
            hex += dirs[dirIndex]
        }
    }

    val hex = center + dirs[(dirIndex + 2).mod(6)] * n
    for (i in -2..-1) {
        val neighborDir = (dirIndex + i).mod(6)
        repeat(n) {
            ray(hex.copy())
            hex += dirs[neighborDir]
        }
    }
    ray(hex.copy())

    return ans
}

fun part1(notes: String): Int {
    val field = notes.lines().map { row -> row.map(Char::digitToIntOrNull) }
    return energy(field, 0).size
}

fun part2(notes: String): Int {
    val field = notes.lines().map { row -> row.map(Char::digitToIntOrNull) }
    return dirs.indices.sumOf { energy(field, it).size }
}

fun part3(notes: String): Int {
    val field = notes.lines().map { row -> row.map(Char::digitToIntOrNull) }.map { it.toMutableList() }
    val n = field.size / 2

    fun increment(hex: Hex): Int {
        val dw = hex.toDoubledWidth()
        val v = field[dw.row + n][dw.col + 2 * n]!!
        field[dw.row + n][dw.col + 2 * n] = v + 1
        return v + 1
    }

    fun reset(hex: Hex) {
        val dw = hex.toDoubledWidth()
        field[dw.row + n][dw.col + 2 * n] = 1
    }

    var warriors = 0
    var dirIndex = 0
    var round = 0
    var totalEnergy = 0
    while (warriors < 100000) {
        val received = energy(field, dirIndex)
        for (hex in received) {
            val v = increment(hex)
            if (v <= 10)
                totalEnergy += 1
            if (v == 10)
                warriors += 1
            if (v == 13)
                reset(hex)
        }

        dirIndex = (dirIndex + 1).mod(6)
        round += 1
    }
    return totalEnergy
}

fun main() {
    println(
        part1(
            """
            ....1.9.6.1.7....
            ...9.2.3.3.8.1...
            ..7.8.3.5.1.4.3..
            .4.6.7.4.1.1.8.9.
            4.5.3.6.5.8.9.4.9
            .9.5.1.5.6.2.8.9.
            ..7.7.6.4.6.2.4..
            ...9.3.8.3.8.4...
            ....1.6.6.2.8....
            """.trimIndent()
        )
    )
    println(
        part2(
            """
            ....1.9.6.1.7....
            ...9.2.3.3.8.1...
            ..7.8.3.5.1.4.3..
            .4.6.7.4.1.1.8.9.
            4.5.3.6.5.8.9.4.9
            .9.5.1.5.6.2.8.9.
            ..7.7.6.4.6.2.4..
            ...9.3.8.3.8.4...
            ....1.6.6.2.8....
            """.trimIndent()
        )
    )
    println(
        part3(
            """
            ....1.9.6.1.7....
            ...9.2.3.3.8.1...
            ..7.8.3.5.1.4.3..
            .4.6.7.4.1.1.8.9.
            4.5.3.6.5.8.9.4.9
            .9.5.1.5.6.2.8.9.
            ..7.7.6.4.6.2.4..
            ...9.3.8.3.8.4...
            ....1.6.6.2.8....
            """.trimIndent()
        )
    )
}