fun main() {
    fun example(expected: Int, notes: String) {
        val actual = minimumSpells(notes.trimIndent())
        check(actual == expected) { "Expected $expected spells, got $actual" }
        println(actual)
    }

    example(
        7, """
        ........#.
        .##.......
        .##..####.
        .##..####.
        .....####.
        .###......
    """
    )
    example(
        6, """
        ........#.
        .##.......
        .##..####.
        .#...#....
        .....#....
        .####.....
    """
    )
    example(
        23, """
        ...............................
        .####....#####..#######........
        .####....#####..#######........
        .####....#####..#######........
        .........#####.......##........
        .........########....##........
        .........########....##........
        ...............................
        .######....#.....#.....###.....
        ...........#...........###.....
        ...........#......########.....
        ...........#......########.....
        ...........#......########.....
        ..................########.....
        ...............................
    """
    )
    example(
        8, """
        ........#.
        .##.......
        .##..####.
        .#...#..#.
        .....####.
        .###......
    """
    )
    example(
        29, """
        ..............................
        .##########....##.....#######.
        .##########.##.##..#..#######.
        .####..####.##.##.....#.#####.
        .##########.##.#####..#.#####.
        .##########.##........#######.
        ..............................
        .##########..################.
        .##########..#..............#.
        .#######..#..#..............#.
        .#######..#..################.
        .##########..################.
        ..............................
    """
    )
    example(
        16, """
        ..............................
        .############################.
        .##.......................###.
        .##..################.###.###.
        .##..######.........#.###.###.
        .##..################.###.###.
        .##...................###.###.
        .##..####################.###.
        .##..####################.###.
        .##.......................###.
        .############################.
        ..............................
    """
    )
}

fun part1(notes: String): Int = minimumSpells(notes)
fun part2(notes: String): Int = minimumSpells(notes)
fun part3(notes: String): Int = minimumSpells(notes)

private fun minimumSpells(notes: String): Int {
    val rows = notes.lines()
    val verticalAt = IntArray(rows.maxOf { it.length })
    val crossings = mutableListOf<MutableList<Int>>()
    var verticalCount = 0
    for ((r, row) in rows.withIndex()) {
        for (c in row.indices) {
            if (row[c] != '#') continue
            if (c == 0 || row[c - 1] != '#') crossings.add(mutableListOf())
            if (r == 0 || rows[r - 1].getOrNull(c) != '#') verticalAt[c] = verticalCount++
            crossings.last().add(verticalAt[c])
        }
    }

    // Each block is an edge between its two possible spells. A minimum vertex
    // cover of this bipartite graph has the size of a maximum matching (Kőnig).
    val matchedRow = IntArray(verticalCount) { -1 }
    val seen = IntArray(verticalCount) { -1 }
    fun augment(horizontal: Int, search: Int): Boolean {
        for (vertical in crossings[horizontal]) {
            if (seen[vertical] == search) continue
            seen[vertical] = search
            if (matchedRow[vertical] == -1 || augment(matchedRow[vertical], search)) {
                matchedRow[vertical] = horizontal
                return true
            }
        }
        return false
    }
    return crossings.indices.count { augment(it, it) }
}
