fun main() {
    println(
        part1(
            """
            AAAEEEFFFF
            AADDDDDGFF
            BACDDIGGFF
            BABBIIIGGH
            BBBBIIIIHH
            """.trimIndent()
        )
    )
    //println(
    //    part2(
    //        """
    //
    //""".trimIndent()
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
    val grid = notes.lines()
    val v = mutableSetOf<Pair<Int, Int>>()

    fun dfs(row: Int, col: Int): Int {
        if (!v.add(row to col)) return 0
        var res = 0
        for ((dr, dc) in arrayOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)) {
            val nr = row + dr
            val nc = col + dc
            res += if (nr in grid.indices && nc in grid[nr].indices)
                if (grid[row][col] == grid[nr][nc])
                    dfs(nr, nc)
                else if (nr to nc !in v)
                    1
                else
                    0
            else
                1
        }
        return res
    }

    var ans = 0
    for (i in grid.indices) {
        for (j in grid[i].indices) {
            if (i to j !in v) {
                ans += dfs(i, j)
            }
        }
    }
    return ans
}

fun part2(notes: String): Int {
    val grid = notes.lines().filter { it.isNotEmpty() }
    if (grid.isEmpty()) return 0

    val h = grid.size
    val w = grid[0].length
    val dirs = arrayOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

    // Размечаем куски стекла
    val id = Array(h) { IntArray(w) { -1 } }
    var pieceCount = 0

    for (r in 0 until h) {
        for (c in 0 until w) {
            if (id[r][c] != -1) continue

            val ch = grid[r][c]
            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.addLast(r to c)
            id[r][c] = pieceCount

            while (stack.isNotEmpty()) {
                val (cr, cc) = stack.removeLast()

                for ((dr, dc) in dirs) {
                    val nr = cr + dr
                    val nc = cc + dc

                    if (nr in 0 until h &&
                        nc in 0 until w &&
                        id[nr][nc] == -1 &&
                        grid[nr][nc] == ch
                    ) {
                        id[nr][nc] = pieceCount
                        stack.addLast(nr to nc)
                    }
                }
            }
            pieceCount++
        }
    }

    val frame = pieceCount
    val nodeCount = pieceCount + 1
    val weights = HashMap<Long, Int>()

    fun addEdge(a: Int, b: Int, weight: Int) {
        if (weight == 0) return
        val x = minOf(a, b)
        val y = maxOf(a, b)
        val key = x.toLong() * nodeCount + y
        weights[key] = (weights[key] ?: 0) + weight
    }

    for (r in 0 until h) {
        for (c in 0 until w) {
            val a = id[r][c]

            // Проверяем только правого и нижнего соседа,
            // чтобы каждое внутреннее ребро между кусками посчитать один раз.
            for ((dr, dc) in arrayOf(0 to 1, 1 to 0)) {
                val nr = r + dr
                val nc = c + dc

                if (nr in 0 until h && nc in 0 until w) {
                    val b = id[nr][nc]
                    if (a != b) addEdge(a, b, 1)
                } else {
                    // Выход за границу снизу или справа
                    addEdge(a, frame, 1)
                }
            }

            // Добавляем верхнюю и левую границы (они не были учтены выше)
            if (r == 0) addEdge(a, frame, 1)
            if (c == 0) addEdge(a, frame, 1)
        }
    }

    val edges = weights.map { (key, weight) ->
        val u = (key / nodeCount).toInt()
        val v = (key % nodeCount).toInt()
        Triple(u, v, weight)
    }.sortedBy { it.third }

    val parent = IntArray(nodeCount) { it }

    fun find(x: Int): Int {
        var p = x
        while (parent[p] != p) p = parent[p]
        var cur = x
        while (parent[cur] != cur) {
            val next = parent[cur]
            parent[cur] = p
            cur = next
        }
        return p
    }

    fun union(a: Int, b: Int): Boolean {
        val ra = find(a)
        val rb = find(b)
        if (ra == rb) return false
        parent[rb] = ra
        return true
    }

    var answer = 0
    var usedEdges = 0

    for ((u, v, weight) in edges) {
        if (union(u, v)) {
            answer += weight
            usedEdges++
            if (usedEdges == nodeCount - 1) break
        }
    }

    return answer
}

fun part3(notes: String): Int {
    val grid = notes.lines().filter { it.isNotEmpty() }
    if (grid.isEmpty()) return 0

    val h = grid.size
    val w = grid[0].length
    val dirs = arrayOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

    // 1. Размечаем куски стекла.
    val id = Array(h) { IntArray(w) { -1 } }
    var pieceCount = 0

    for (r in 0 until h) {
        for (c in 0 until w) {
            if (id[r][c] != -1) continue

            val ch = grid[r][c]
            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.addLast(r to c)
            id[r][c] = pieceCount

            while (stack.isNotEmpty()) {
                val (cr, cc) = stack.removeLast()

                for ((dr, dc) in dirs) {
                    val nr = cr + dr
                    val nc = cc + dc

                    if (nr in 0 until h &&
                        nc in 0 until w &&
                        id[nr][nc] == -1 &&
                        grid[nr][nc] == ch
                    ) {
                        id[nr][nc] = pieceCount
                        stack.addLast(nr to nc)
                    }
                }
            }
            pieceCount++
        }
    }

    val frame = pieceCount
    val nodeCount = pieceCount + 1

    // 2. Собираем веса рёбер между кусками и рамкой.
    val weights = HashMap<Long, Int>()

    fun addEdge(a: Int, b: Int, weight: Int) {
        if (weight == 0) return
        val x = minOf(a, b)
        val y = maxOf(a, b)
        val key = x.toLong() * nodeCount + y
        weights[key] = (weights[key] ?: 0) + weight
    }

    for (r in 0 until h) {
        for (c in 0 until w) {
            val a = id[r][c]

            // Правый и нижний соседи.
            for ((dr, dc) in arrayOf(0 to 1, 1 to 0)) {
                val nr = r + dr
                val nc = c + dc

                if (nr in 0 until h && nc in 0 until w) {
                    val b = id[nr][nc]
                    if (a != b) addEdge(a, b, 1)
                } else {
                    addEdge(a, frame, 1)
                }
            }

            // Верхняя и левая границы.
            if (r == 0) addEdge(a, frame, 1)
            if (c == 0) addEdge(a, frame, 1)
        }
    }

    data class Edge(val u: Int, val v: Int, val w: Int)

    val baseEdges = weights.map { (key, weight) ->
        val u = (key / nodeCount).toInt()
        val v = (key % nodeCount).toInt()
        Edge(u, v, weight)
    }

    // 3. MST для выбранного набора кусков с зарядами.
    fun mstCost(selected: List<Int>): Int {
        val sel = selected.toHashSet()

        val edges = baseEdges.map { e ->
            if (e.u in sel || e.v in sel) Edge(e.u, e.v, 0) else e
        }.sortedBy { it.w }

        val parent = IntArray(nodeCount) { it }

        fun find(x: Int): Int {
            var p = x
            while (parent[p] != p) p = parent[p]

            var cur = x
            while (parent[cur] != cur) {
                val next = parent[cur]
                parent[cur] = p
                cur = next
            }
            return p
        }

        fun union(a: Int, b: Int): Boolean {
            val ra = find(a)
            val rb = find(b)
            if (ra == rb) return false
            parent[rb] = ra
            return true
        }

        var cost = 0
        var used = 0

        for (e in edges) {
            if (union(e.u, e.v)) {
                cost += e.w
                used++
                if (used == nodeCount - 1) break
            }
        }

        return cost
    }

    var answer = Int.MAX_VALUE

    // Не использовать заряды.
    answer = minOf(answer, mstCost(emptyList()))

    // Использовать один заряд.
    for (i in 0 until pieceCount) {
        answer = minOf(answer, mstCost(listOf(i)))
    }

    // Использовать два заряда.
    for (i in 0 until pieceCount) {
        for (j in i + 1 until pieceCount) {
            answer = minOf(answer, mstCost(listOf(i, j)))
        }
    }

    return answer
}