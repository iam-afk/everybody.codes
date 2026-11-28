fun part1(notes: String): Long {
    val (first, second) = layers(notes)
    val counts = LongArray(26)
    for (row in first.indices) {
        for (col in first[row].indices) {
            val colour = mix(first[row][col], second[row][col])
            counts[colour - 'A']++
        }
    }
    return score(counts)
}

private fun layers(notes: String): List<List<String>> =
    notes.trim().replace("\r\n", "\n").split("\n\n").map { it.lines() }

private fun score(counts: LongArray): Long {
    val (a, b, c) = counts.sortedDescending()
    return a * b * c
}

private fun mix(a: Char, b: Char): Char = when {
    a == 'K' || b == 'K' -> 'K'
    a == b -> a
    a != 'Y' && b != 'Y' -> 'B'
    a != 'M' && b != 'M' -> 'G'
    else -> 'R'
}

fun part2(notes: String): Long {
    val (first, second) = layers(notes)
    val colours = "CMYK"
    val pairs = Array(4) { LongArray(4) }
    for (row in first.indices) {
        for (col in first[row].indices) {
            val digit = second[row][col] - '1'
            val colour = colours.indexOf(first[row][col])
            pairs[digit][colour]++
        }
    }

    val assignment = CharArray(4)
    fun best(digit: Int, used: Int): Long {
        if (digit == assignment.size) {
            val counts = LongArray(26)
            for (i in assignment.indices) {
                for (j in colours.indices) {
                    val colour = mix(colours[j], assignment[i])
                    counts[colour - 'A'] += pairs[i][j]
                }
            }
            return score(counts)
        }

        var result = 0L
        for (i in colours.indices) {
            val bit = 1 shl i
            if (used and bit != 0) continue
            assignment[digit] = colours[i]
            result = maxOf(result, best(digit + 1, used or bit))
        }
        return result
    }
    return best(0, 0)
}

fun part3(notes: String): Long {
    val sources = "CMYK"
    val outputs = "CMYKBGR"
    val counts = LongArray(sources.length)
    for (line in notes.lineSequence()) {
        for (colour in line.trim()) counts[sources.indexOf(colour)]++
    }
    val reachable = sources.map { a -> "CMY".map { b -> mix(a, b) }.toSet() }

    var best = 0L
    for (i in 0 until outputs.length - 2) {
        for (j in i + 1 until outputs.length - 1) {
            for (k in j + 1 until outputs.length) {
                val target = charArrayOf(outputs[i], outputs[j], outputs[k])
                val lower = LongArray(3)
                val upper = LongArray(3)
                var total = 0L
                for (s in sources.indices) {
                    val choices = target.indices.filter { target[it] in reachable[s] }
                    if (choices.isEmpty()) continue
                    total += counts[s]
                    for (t in choices) upper[t] += counts[s]
                    if (choices.size == 1) lower[choices.single()] += counts[s]
                }
                best = maxOf(best, balancedProduct(total, lower, upper))
            }
        }
    }
    return best
}

private fun balancedProduct(total: Long, lower: LongArray, upper: LongArray): Long {
    // For three outputs, the capacity limits for pairs are the lower bounds on their complements.
    // With those bounds and a fixed sum, balancing the counts maximizes their product.
    var low = 0L
    var high = total + 1
    while (low + 1 < high) {
        val level = (low + high) / 2
        val used = lower.indices.sumOf { level.coerceIn(lower[it], upper[it]) }
        if (used <= total) low = level else high = level
    }

    val counts = LongArray(3) { low.coerceIn(lower[it], upper[it]) }
    var remaining = total - counts.sum()
    for (i in counts.indices) {
        if (remaining > 0 && counts[i] == low && counts[i] < upper[i]) {
            counts[i]++
            remaining--
        }
    }
    return counts[0] * counts[1] * counts[2]
}
