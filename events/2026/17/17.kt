import kotlin.math.min

fun main() {
    println(part1("""
        ####.######
        #.#.##..#..
        ##.##.##..#
        #.#.###.###
        #.###.###.#
        ###.##.#.##
    """.trimIndent()))
    println(part2("""
        ####.######
        #.#.##..#..
        ##.##.##..#
        #.#.###.###
        #.###.###.#
        ###.##.#.##
    """.trimIndent()))
    println(part3("""
        ####.######
        #.#.##..#..
        ##.##.##..#
        #.#.###.###
        #.###.###.#
        ###.##.#.##
    """.trimIndent()))
}

operator fun List<String>.get(row: Int, col: Int): Boolean = row in this.indices && col in this[row].indices && this[row][col] == '#'

fun part1(notes: String): Int {
    val field = notes.lines()
    val n = field.size
    val m = field.last().length

    data class S(val step: Int, val row: Int, val col: Int)

    val queue = ArrayDeque<S>()
    queue.add(S(0, 0, 0))
    val v = mutableSetOf(0 to 0)
    while (queue.isNotEmpty()) {
        val (step, row, col) = queue.removeFirst()
        if (row == n - 1 && col == m - 1) return step
        for ((dr, dc) in arrayOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)) {
            val nr = row + dr
            val nc = col + dc
            if (nr to nc !in v && field[nr, nc]) {
                queue.add(S(step + 1, nr, nc))
                v.add(nr to nc)
            }
        }
    }
    return -1
}

data class D(val row: Int, val col: Int) {
    fun left(): D = D(-col, row)
    fun right(): D = D(col, -row)
}

data class P(var row: Int, var col: Int) {
    operator fun plus(d: D): P = P(row + d.row, col + d.col)
}

operator fun List<String>.get(p: P): Boolean = p.row in this.indices && p.col in this[p.row].indices && this[p.row][p.col] == '#'


fun part2(notes: String): Int {
    val field = notes.lines()

    var p = P(0, 0)
    var d = D(0, 1)
    var ans = 0
    do {
        if (!field[p + d]) {
            d = d.right()
            ans++
        } else {
            p += d
            ans++
            if (field[p + d.left()]) {
                d = d.left()
                p += d
            }
        }
    } while (!(p == P(0, 0) && d == D(0, 1)))

    return ans
}

fun part3(notes: String): Int {
    val field = notes.lines()

    val bottomRight = P(field.size - 1, field[field.size-1].length - 1)

    class Loop(var d: D, val rotate: D.() -> D, val hand: D.() -> D) {
        var p = P(0, 0)
        var steps = 0

        fun move() {
            if (!field[p + d]) {
                d = d.rotate()
                steps++
            } else {
                p += d
                steps++
                if (field[p + d.hand()]) {
                    d = d.hand()
                    p += d
                }
            }
        }
    }

    val left = Loop(D(0, 1), D::right, D::left)
    val right = Loop(D(1, 0), D::left, D::right)
    do {
        left.move()
        right.move()
    } while (!(left.p == bottomRight && left.d == D(0, -1) || right.p == bottomRight && right.d == D(-1, 0)))

    return min(left.steps, right.steps)
}
