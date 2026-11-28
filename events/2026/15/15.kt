import java.util.*

fun main() {
    println(
        part1(
            """
#####################################################
#.@.........#................#.....................##
#.#########@#.##############.#.###################.##
#.#.......#.#.##..........##.#.###..@..@......@###.##
#.#.#####.#.#.##.########.##.#.###@###########.###.##
#.#.#@@@#.#.#.##.##....##.##@#.###.###.....###.###.##
#.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###.##
S...................................................E
#.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###@##
#.#.#...#.#.#.##@##.@..##.##.#.###.###@....###.###.##
#.#.#####.#.#.##.########.##.#.###.###########.###.##
#.#.......#.#.##..@.......##.#.###.............###.##
#.#########.#.##############.#.###################.##
#...........#................#................@....##
#####################################################
            """.trimIndent()
        )
    )
    println(
        part2(
            """
#####################################################
#.@.........#................#.....................##
#.#########@#.##############.#.###################.##
#.#.......#.#.##..........##.#.###..@..@......@###.##
#.#.#####.#.#.##.########.##.#.###@###########.###.##
#.#.#@@@#.#.#.##.##....##.##@#.###.###.....###.###.##
#.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###.##
S...................................................E
#.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###@##
#.#.#...#.#.#.##@##.@..##.##.#.###.###@....###.###.##
#.#.#####.#.#.##.########.##.#.###.###########.###.##
#.#.......#.#.##..@.......##.#.###.............###.##
#.#########.#.##############.#.###################.##
#...........#................#................@....##
#####################################################
            """.trimIndent()
        )
    )
    println(
        part3(
            """
#####################################################
#.@.........#................#..........@..........##
#.#########@#.##############.#.###################.##
#.#...@...#.#.##.....@....##.#.###..@..@......@###.##
#.#.#####.#.#.##.########.##.#.###@###########.###.##
#.#.#@@@#.#.#.##.##..@.##.##@#.###.###..@..###.###.##
#.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###.##
S...................................................E
#.#.#.#.#.#.#.##.##.##.##.##.#.###.###.###.###.###@##
#.#.#.@.#.#.#.##@##.@..##.##.#.###.###@....###.###.##
#.#.#####.#.#.##.########.##.#.###.###########.###.##
#.#...@...#.#.##..@.......##.#.###......@......###.##
#.#########.#.##############.#.###################.##
#.....@.....#........@.......#................@....##
#####################################################
            """.trimIndent()
        )
    )
}

// ---------- Part I ----------
data class State1(val mask: Long, val pos: Int, val dir: Int)

fun part1(notes: String): Int {
    val grid = notes.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (grid.isEmpty()) return 0

    val h = grid.size
    val w = grid[0].length
    val depositIndex = Array(h) { IntArray(w) { -1 } }
    var depositCount = 0
    var start = -1
    var end = -1

    for (r in 0 until h) {
        for (c in 0 until w) {
            when (grid[r][c]) {
                'S' -> start = r * w + c
                'E' -> end = r * w + c
                '@' -> depositIndex[r][c] = depositCount++
            }
        }
    }

    val allMask = if (depositCount == 0) 0L else (1L shl depositCount) - 1L

    var initialMask = 0L
    val sr = start / w
    val sc = start % w
    if (depositIndex[sr][sc] >= 0) {
        initialMask = initialMask or (1L shl depositIndex[sr][sc])
    }

    if (start == end && initialMask == allMask) return 0

    val visited = HashSet<State1>()
    val queue = ArrayDeque<State1>()
    val initialState = State1(initialMask, start, 4)
    visited.add(initialState)
    queue.add(initialState)

    val dr = intArrayOf(-1, 0, 1, 0)
    val dc = intArrayOf(0, 1, 0, -1)
    var seconds = 0

    while (queue.isNotEmpty()) {
        repeat(queue.size) {
            val state = queue.removeFirst()

            if (state.pos == end && state.mask == allMask) return seconds

            val r = state.pos / w
            val c = state.pos % w

            for (nd in 0..3) {
                if (state.dir != 4 && nd == (state.dir + 2) % 4) continue

                val nr = r + dr[nd]
                val nc = c + dc[nd]

                if (nr !in 0 until h || nc !in 0 until w) continue
                if (grid[nr][nc] == '#') continue

                var newMask = state.mask
                val di = depositIndex[nr][nc]
                if (di >= 0) newMask = newMask or (1L shl di)

                val nextState = State1(newMask, nr * w + nc, nd)
                if (visited.add(nextState)) {
                    queue.addLast(nextState)
                }
            }
        }
        seconds++
    }

    return -1
}

// ---------- Part II ----------
fun part2(notes: String): Int {
    val grid = notes.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (grid.isEmpty()) return 0

    val h = grid.size
    val w = grid[0].length
    var start = -1
    var end = -1

    for (r in 0 until h) {
        for (c in 0 until w) {
            when (grid[r][c]) {
                'S' -> start = r * w + c
                'E' -> end = r * w + c
            }
        }
    }

    val cap = 2026
    val nCells = h * w
    val dirCount = 5
    val totalStates = (cap + 1) * nCells * dirCount

    val visited = BooleanArray(totalStates)
    val queue = IntArray(totalStates)
    var head = 0
    var tail = 0

    fun encode(s: Int, pos: Int, dir: Int): Int =
        ((s * nCells + pos) * dirCount + dir)

    fun decodeS(code: Int): Int = code / (nCells * dirCount)
    fun decodePos(code: Int): Int = (code / dirCount) % nCells
    fun decodeDir(code: Int): Int = code % dirCount

    val sr = start / w
    val sc = start % w
    var initialS = if (grid[sr][sc] == '@') 1 else 0
    if (initialS > cap) initialS = cap

    val initialCode = encode(initialS, start, 4)
    visited[initialCode] = true
    queue[tail++] = initialCode

    val dr = intArrayOf(-1, 0, 1, 0)
    val dc = intArrayOf(0, 1, 0, -1)
    var seconds = 0

    while (head < tail) {
        val levelEnd = tail

        while (head < levelEnd) {
            val code = queue[head++]
            val s = decodeS(code)
            val pos = decodePos(code)
            val dir = decodeDir(code)

            if (pos == end && s >= cap) return seconds

            val r = pos / w
            val c = pos % w

            for (nd in 0..3) {
                if (dir != 4 && nd == (dir + 2) % 4) continue

                val nr = r + dr[nd]
                val nc = c + dc[nd]

                if (nr !in 0 until h || nc !in 0 until w) continue
                if (grid[nr][nc] == '#') continue

                var ns = s
                if (grid[nr][nc] == '@' && ns < cap) ns++
                if (ns > cap) ns = cap

                val npos = nr * w + nc
                val ncode = encode(ns, npos, nd)

                if (!visited[ncode]) {
                    visited[ncode] = true
                    queue[tail++] = ncode
                }
            }
        }

        seconds++
    }

    return -1
}

// ---------- Part III ----------
private const val INF = Long.MAX_VALUE / 4

fun part3(notes: String): Long {
    val grid = notes.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (grid.isEmpty()) return -1L

    val h = grid.size
    val w = grid[0].length

    // Map open cells to compact indices
    val cellId = Array(h) { IntArray(w) { -1 } }
    var cellCount = 0
    var startCell = -1
    var endCell = -1

    for (r in 0 until h) {
        for (c in 0 until w) {
            if (grid[r][c] != '#') {
                cellId[r][c] = cellCount
                if (grid[r][c] == 'S') startCell = cellCount
                if (grid[r][c] == 'E') endCell = cellCount
                cellCount++
            }
        }
    }

    val totalStates = cellCount * 5
    val startState = startCell * 5 + 4

    // Build adjacency lists
    val adj = Array(totalStates) { mutableListOf<Pair<Int, Int>>() } // (nextState, reward)
    val radj = Array(totalStates) { mutableListOf<Int>() }

    val dr = intArrayOf(-1, 0, 1, 0)
    val dc = intArrayOf(0, 1, 0, -1)

    // Precompute cell coordinates for each cellId
    val cellR = IntArray(cellCount)
    val cellC = IntArray(cellCount)
    for (r in 0 until h) {
        for (c in 0 until w) {
            val id = cellId[r][c]
            if (id >= 0) {
                cellR[id] = r
                cellC[id] = c
            }
        }
    }

    for (cid in 0 until cellCount) {
        val r = cellR[cid]
        val c = cellC[cid]
        for (dir in 0..4) {
            val state = cid * 5 + dir
            for (nd in 0..3) {
                if (dir != 4 && nd == (dir + 2) % 4) continue
                val nr = r + dr[nd]
                val nc = c + dc[nd]
                if (nr !in 0 until h || nc !in 0 until w) continue
                val ncid = cellId[nr][nc]
                if (ncid < 0) continue
                val nstate = ncid * 5 + nd
                val reward = if (grid[nr][nc] == '@') 1 else 0
                adj[state].add(Pair(nstate, reward))
                radj[nstate].add(state)
            }
        }
    }

    // Reachable from start
    val reachable = BooleanArray(totalStates)
    val q = ArrayDeque<Int>()
    reachable[startState] = true
    q.add(startState)
    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        for ((v, _) in adj[u]) {
            if (!reachable[v]) {
                reachable[v] = true
                q.add(v)
            }
        }
    }

    // Can reach any end state
    val canReachEnd = BooleanArray(totalStates)
    q.clear()
    for (dir in 0..4) {
        val eState = endCell * 5 + dir
        canReachEnd[eState] = true
        q.add(eState)
    }
    while (q.isNotEmpty()) {
        val u = q.removeFirst()
        for (v in radj[u]) {
            if (!canReachEnd[v]) {
                canReachEnd[v] = true
                q.add(v)
            }
        }
    }

    // Valid states
    val valid = BooleanArray(totalStates)
    for (i in 0 until totalStates) {
        valid[i] = reachable[i] && canReachEnd[i]
    }

    // Build valid subgraph
    val validAdj = Array(totalStates) { mutableListOf<Pair<Int, Int>>() }
    for (u in 0 until totalStates) {
        if (!valid[u]) continue
        for ((v, rew) in adj[u]) {
            if (valid[v]) {
                validAdj[u].add(Pair(v, rew))
            }
        }
    }

    // Find SCCs using Kosaraju
    val visited = BooleanArray(totalStates)
    val order = mutableListOf<Int>()

    fun dfs1(u: Int) {
        visited[u] = true
        for ((v, _) in validAdj[u]) {
            if (!visited[v]) dfs1(v)
        }
        order.add(u)
    }

    for (i in 0 until totalStates) {
        if (valid[i] && !visited[i]) dfs1(i)
    }

    // Reverse graph for valid states
    val revValidAdj = Array(totalStates) { mutableListOf<Int>() }
    for (u in 0 until totalStates) {
        if (!valid[u]) continue
        for ((v, _) in validAdj[u]) {
            revValidAdj[v].add(u)
        }
    }

    val comp = IntArray(totalStates) { -1 }
    var compCount = 0

    fun dfs2(u: Int, c: Int) {
        comp[u] = c
        for (v in revValidAdj[u]) {
            if (comp[v] == -1) dfs2(v, c)
        }
    }

    for (i in order.indices.reversed()) {
        val u = order[i]
        if (valid[u] && comp[u] == -1) {
            dfs2(u, compCount++)
        }
    }

    // Group nodes by SCC
    val sccNodes = Array(compCount) { mutableListOf<Int>() }
    for (i in 0 until totalStates) {
        if (valid[i]) {
            sccNodes[comp[i]].add(i)
        }
    }

    // Find max mean reward cycle using Karp's algorithm per SCC
    var bestRatioNum = -1L
    var bestRatioDen = 1L
    var bestScc = -1
    var bestSourceLocal = -1
    var bestVLocal = -1
    var bestK = -1
    var bestN = 0
    var bestF: Array<LongArray>? = null
    var bestParent: Array<IntArray>? = null
    var bestLocalToGlobal: IntArray? = null

    for (sccIdx in 0 until compCount) {
        val nodes = sccNodes[sccIdx]
        val n = nodes.size
        if (n < 2) continue // no cycle without self-loop

        val localToGlobal = IntArray(n)
        val globalToLocal = HashMap<Int, Int>()
        for (i in 0 until n) {
            localToGlobal[i] = nodes[i]
            globalToLocal[nodes[i]] = i
        }

        // Build local adjacency
        val localAdj = Array(n) { mutableListOf<Pair<Int, Int>>() }
        for (i in 0 until n) {
            val u = nodes[i]
            for ((v, rew) in validAdj[u]) {
                val vLocal = globalToLocal[v] ?: continue
                localAdj[i].add(Pair(vLocal, rew))
            }
        }

        // Karp's algorithm
        val sourceLocal = 0
        val F = Array(n + 1) { LongArray(n) { -INF } }
        val parent = Array(n + 1) { IntArray(n) { -1 } }
        F[0][sourceLocal] = 0L

        for (k in 1..n) {
            for (v in 0 until n) {
                var bestVal = -INF
                var bestPrev = -1
                for ((u, rew) in localAdj[v].map { Pair(it.first, it.second) }) {
                    // This is wrong: localAdj is outgoing. We need incoming.
                }
                // Actually we need incoming edges to v. Let's build incoming local adjacency.
            }
        }

        // Build incoming local adjacency
        val localIn = Array(n) { mutableListOf<Pair<Int, Int>>() }
        for (u in 0 until n) {
            for ((v, rew) in localAdj[u]) {
                localIn[v].add(Pair(u, rew))
            }
        }

        // Re-run Karp
        for (k in 1..n) {
            for (v in 0 until n) {
                var bestVal = -INF
                var bestPrev = -1
                for ((u, rew) in localIn[v]) {
                    if (F[k - 1][u] != -INF) {
                        val valHere = F[k - 1][u] + rew
                        if (valHere > bestVal) {
                            bestVal = valHere
                            bestPrev = u
                        }
                    }
                }
                F[k][v] = bestVal
                parent[k][v] = bestPrev
            }
        }

        // Find max ratio in this SCC
        var sccBestNum = -1L
        var sccBestDen = 1L
        var sccBestV = -1
        var sccBestK = -1

        for (v in 0 until n) {
            if (F[n][v] == -INF) continue
            for (k in 0 until n) {
                if (F[k][v] == -INF) continue
                val num = F[n][v] - F[k][v]
                val den = (n - k).toLong()
                if (num < 0) continue // ratio negative, ignore
                if (sccBestV == -1 || num * sccBestDen > sccBestNum * den) {
                    sccBestNum = num
                    sccBestDen = den
                    sccBestV = v
                    sccBestK = k
                }
            }
        }

        if (sccBestV != -1) {
            if (bestScc == -1 || sccBestNum * bestRatioDen > bestRatioNum * sccBestDen) {
                bestRatioNum = sccBestNum
                bestRatioDen = sccBestDen
                bestScc = sccIdx
                bestSourceLocal = sourceLocal
                bestVLocal = sccBestV
                bestK = sccBestK
                bestN = n
                bestF = F
                bestParent = parent
                bestLocalToGlobal = localToGlobal
            }
        }
    }

    if (bestScc == -1) return -1L // no cycle found

    // Reconstruct walk of length bestN from source to bestVLocal
    val walkLocal = IntArray(bestN + 1)
    var curV = bestVLocal
    for (k in bestN downTo 0) {
        walkLocal[k] = curV
        if (k > 0) {
            curV = bestParent!![k][curV]
            if (curV == -1) return -1L
        }
    }

    // Convert to global states
    val walkGlobal = IntArray(bestN + 1) { bestLocalToGlobal!![walkLocal[it]] }

    // Compute prefix rewards along walk
    val prefReward = LongArray(bestN + 1)
    for (i in 0 until bestN) {
        val u = walkGlobal[i]
        val v = walkGlobal[i + 1]
        var rew = 0
        for ((next, r) in validAdj[u]) {
            if (next == v) {
                rew = r
                break
            }
        }
        prefReward[i + 1] = prefReward[i] + rew
    }

    // Extract best cycle from walk
    var cycleR = -1L
    var cycleL = -1L
    var bestMeanNum = -1L
    var bestMeanDen = 1L

    for (i in 0..bestN) {
        for (j in i + 1..bestN) {
            if (walkGlobal[i] == walkGlobal[j]) {
                val len = (j - i).toLong()
                val rew = prefReward[j] - prefReward[i]
                if (rew > 0) {
                    if (cycleR == -1L || rew * bestMeanDen > bestMeanNum * len) {
                        bestMeanNum = rew
                        bestMeanDen = len
                        cycleR = rew
                        cycleL = len
                    }
                }
            }
        }
    }

    if (cycleR <= 0) return -1L

    val R = cycleR
    val L = cycleL

    // Now compute shortest path with exact reward N using modulo R
    // We work on valid states only. Build compact mapping for valid states.
    val validStates = mutableListOf<Int>()
    for (i in 0 until totalStates) {
        if (valid[i]) validStates.add(i)
    }
    val S = validStates.size
    val stateToIdx = IntArray(totalStates) { -1 }
    for (i in 0 until S) {
        stateToIdx[validStates[i]] = i
    }

    val startIdx = stateToIdx[startState]
    if (startIdx == -1) return -1L

    // Build edges for valid states (compact indices)
    val edges = Array(S) { mutableListOf<Pair<Int, Int>>() } // (to, reward)
    for (i in 0 until S) {
        val u = validStates[i]
        for ((v, rew) in validAdj[u]) {
            val vi = stateToIdx[v]
            if (vi != -1) {
                edges[i].add(Pair(vi, rew))
            }
        }
    }

    // Initial reward from start cell
    val sr = startCell.let { cellR[it] }
    val sc = startCell.let { cellC[it] }
    val initialReward = if (grid[sr][sc] == '@') 1 else 0

    // Compute potentials h(v) using SPFA to handle negative edge costs
    // Edge weight w = R - reward * L
    val distH = LongArray(S) { 0L }
    val inQueue = BooleanArray(S)
    val queueH = ArrayDeque<Int>()
    for (i in 0 until S) {
        queueH.add(i)
        inQueue[i] = true
    }

    while (queueH.isNotEmpty()) {
        val u = queueH.removeFirst()
        inQueue[u] = false
        for ((v, rew) in edges[u]) {
            val w = R - rew.toLong() * L
            if (distH[u] + w < distH[v]) {
                distH[v] = distH[u] + w
                if (!inQueue[v]) {
                    inQueue[v] = true
                    queueH.add(v)
                }
            }
        }
    }

    // Dijkstra on expanded graph (state, remainder)
    val totalExp = S * R.toInt()
    val dist = LongArray(totalExp) { INF }
    val pq = PriorityQueue<Pair<Long, Int>>(compareBy { it.first })

    val initRem = (initialReward % R).toInt()
    val startCode = startIdx * R.toInt() + initRem
    dist[startCode] = 0L
    pq.add(Pair(0L, startCode))

    val targetRem = (202620262026L % R).toInt()
    val endIndices = mutableListOf<Int>()
    for (dir in 0..4) {
        val eState = endCell * 5 + dir
        val idx = stateToIdx[eState]
        if (idx != -1) endIndices.add(idx)
    }

    while (pq.isNotEmpty()) {
        val (d, code) = pq.poll()
        if (d > dist[code]) continue

        val state = code / R.toInt()
        val rem = code % R.toInt()

        // If this is an end state with correct remainder, we could update answer,
        // but we need min over all, so just continue Dijkstra.

        for ((v, rew) in edges[state]) {
            val nrem = (rem + rew) % R.toInt()
            val w = R - rew.toLong() * L
            val reduced = w + distH[state] - distH[v]
            val nd = d + reduced
            val ncode = v * R.toInt() + nrem
            if (nd < dist[ncode]) {
                dist[ncode] = nd
                pq.add(Pair(nd, ncode))
            }
        }
    }

    var minCost = INF
    for (eIdx in endIndices) {
        val code = eIdx * R.toInt() + targetRem
        if (dist[code] != INF) {
            // original cost = reducedDist - h[start] + h[end]
            val origCost = dist[code] - distH[startIdx] + distH[eIdx]
            if (origCost < minCost) minCost = origCost
        }
    }

    if (minCost == INF) return -1L

    val N = 202620262026L
    val answer = ((N - initialReward) * L + minCost) / R
    return answer
}