import kotlin.math.min

fun main() {
    println(
        part1(
            """
            ........#.
            .##.......
            .##..####.
            .##..####.
            .....####.
            .###......
            """.trimIndent()
        )
    )
    println(
        part2(
            """
            ........#.
            .##.......
            .##..####.
            .#...#....
            .....#....
            .####.....
            """.trimIndent()
        )
    )
    //println(
    //    part2(
    //        """
    //        ...............................
    //        .####....#####..#######........
    //        .####....#####..#######........
    //        .####....#####..#######........
    //        .........#####.......##........
    //        .........########....##........
    //        .........########....##........
    //        ...............................
    //        .######....#.....#.....###.....
    //        ...........#...........###.....
    //        ...........#......########.....
    //        ...........#......########.....
    //        ...........#......########.....
    //        ..................########.....
    //        ...............................
    //        """.trimIndent()
    //    )
    //)
    //println(
    //    part3(
    //        """
    //
    //""".trimIndent()
    //    )
    //)
}

fun part1(notes: String): Int {
    val map = notes.lines()
    val v = mutableSetOf<Pair<Int, Int>>()
    var ans = 0
    for (t in map.indices) {
        for (l in map[t].indices) {
            if (t to l !in v && map[t][l] == '#') {
                var b = t
                while (b in map.indices && map[b][l] == '#')
                    b += 1
                var r = l
                while (r in map[t].indices && map[t][r] == '#')
                    r += 1
                ans += min(b - t, r - l)
                for (i in t..<b)
                    for (j in l..<r)
                        v.add(i to j)
            }
        }
    }
    return ans
}

data class P(var row: Int, var col: Int) {
    operator fun plus(d: D): P = P(row + d.row, col + d.col)
    operator fun plusAssign(d: D) {
        row += d.row
        col += d.col
    }
}

data class D(val row: Int, val col: Int) {
    fun turnLeft(): D = D(-col, row)
    fun turnRight(): D = D(col, -row)
}

operator fun List<String>.get(p: P): Char =
    if (p.row in this.indices && p.col in this[p.row].indices) this[p.row][p.col] else '.'

fun part2(notes: String): Int {
    val map = notes.lines()
    val v = mutableSetOf<Pair<Int, Int>>()
    var ans = 0
    for (t in map.indices) {
        for (l in map[t].indices) {
            if (t to l !in v && map[t][l] == '#') {
                val p = P(t, l)
                var d = D(0, 1)
                val corners = buildList {
                    while (p + d != P(t, l)) {
                        while (map[p + d] == '#')
                            p += d
                        add(p.copy())
                        d = d.turnLeft()
                        while (map[p + d] != '#')
                            d.turnRight()
                    }
                }
                println(corners)

                //var b = t
                //while (b in map.indices && map[b][l] == '#')
                //    b += 1
                //var r = l
                //while (t to r !in v && r in map[t].indices && map[t][r] == '#')
                //    r += 1
                //
                //ans += min(b - t, r - l)
                //println("$ans $t $b ${b - t} $l $r ${r - l}")
                //for (i in t..<b)
                //    for (j in l..<r)
                //        if (map[i][j] == '#')
                //            v.add(i to j)
            }
        }
    }
    return ans
}

fun part3(notes: String): Int = TODO()
