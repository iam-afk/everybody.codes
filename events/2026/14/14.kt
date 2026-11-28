fun main() {
    val small = """
        AAAEEEFFFF
        AADDDDDGFF
        BACDDIGGFF
        BABBIIIGGH
        BBBBIIIIHH
    """.trimIndent()
    val large = """
        IIIIIILOHHHHHHHHGGGGGQSRRRRKKK
        IIIIIILOOHHHHHHGGXGGGQSRRRRKKK
        PPPPILLLOOOODDHBGGGGGQSSRRRKKK
        PPZPLLLLFFOODDBBBBGGQQQSSRKKKK
        PPPPLLLLFFFONDDDBBMGGQQSSKKJJJ
        PPPCCCCCCFFONNDDMMMGQQQSSSKJJJ
        PCCCCCCCCFFNNDDDDMAAQQQSSSKKJJ
        PPCCECCCFFNNNNDADMAATQTTSSSJJJ
        PPEEEEECCFFNNAAAMMATTTTTSTSSSJ
        PPEEEEEEEFFNNNAAAAAATTTTTTSSJJ
    """.trimIndent()
    check(part1(small) == 67)
    check(part2(small) == 12 && part3(small) == 0)
    check(part2(large) == 63 && part3(large) == 21)
}

private const val FRAME = -1 // Outside the Unicode code point range.

private inline fun forEachBinding(notes: String, bind: (Int, Int) -> Unit) {
    var above = intArrayOf()
    for (line in notes.lineSequence()) {
        if (line.isEmpty()) continue
        val row = line.codePoints().toArray()
        bind(row.first(), FRAME)
        for (col in row.indices) {
            val piece = row[col]
            val top = if (above.isEmpty()) FRAME else above[col]
            if (piece != top) bind(piece, top)
            if (col > 0 && piece != row[col - 1]) bind(piece, row[col - 1])
        }
        bind(row.last(), FRAME)
        above = row
    }
    for (piece in above) bind(piece, FRAME)
}

fun part1(notes: String): Int {
    var total = 0
    forEachBinding(notes) { _, _ -> total++ }
    return total
}

private class Components(size: Int) {
    private val parent = IntArray(size) { -1 } // Roots store the negative component size.

    private fun root(node: Int): Int {
        if (parent[node] < 0) return node
        parent[node] = root(parent[node])
        return parent[node]
    }

    fun join(a: Int, b: Int): Boolean {
        val x = root(a)
        val y = root(b)
        if (x == y) return false
        val larger = if (parent[x] <= parent[y]) x else y
        val smaller = if (larger == x) y else x
        parent[larger] += parent[smaller]
        parent[smaller] = larger
        return true
    }
}

private data class Seam(val a: Int, val b: Int, val length: Int)

private class Window(notes: String) {
    val neighbors: Array<MutableList<Int>>
    val tree: List<Seam>

    init {
        val ids = mutableMapOf(FRAME to 0)
        val lengths = mutableMapOf<Pair<Int, Int>, Int>()
        forEachBinding(notes) { first, second ->
            val a = ids.getOrPut(first) { ids.size }
            val b = ids.getOrPut(second) { ids.size }
            val pair = if (a < b) a to b else b to a
            lengths[pair] = (lengths[pair] ?: 0) + 1
        }
        neighbors = Array(ids.size) { mutableListOf() }
        val seams = lengths.map { (pair, length) ->
            val (a, b) = pair
            neighbors[a].add(b)
            neighbors[b].add(a)
            Seam(a, b, length)
        }
        val components = Components(ids.size)
        tree = seams.sortedBy { it.length }.filter { components.join(it.a, it.b) }
    }
}

fun part2(notes: String): Int = Window(notes).tree.sumOf { it.length }

fun part3(notes: String): Int {
    val window = Window(notes)
    val neighbors = window.neighbors
    if (neighbors.size <= 3) return 0 // The frame plus at most two pieces.

    var best = Int.MAX_VALUE
    for (a in 1 until neighbors.size) {
        for (b in a + 1 until neighbors.size) {
            val components = Components(neighbors.size)
            for (n in neighbors[a]) components.join(a, n)
            for (n in neighbors[b]) components.join(b, n)

            // Every omitted seam has a tree path of no heavier seams. That path
            // still exists after free joins, so Kruskal only needs the tree edges.
            var cost = 0
            for (seam in window.tree) {
                if (components.join(seam.a, seam.b)) cost += seam.length
                if (cost >= best) break
            }
            best = minOf(best, cost)
            if (best == 0) return 0
        }
    }
    return best
}
