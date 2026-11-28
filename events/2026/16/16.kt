fun main() {
    println(part1("RRGGBBGRRGGBBG"))
    println(part1("BBRGGRRGBBRGGBRGBBRRBRRRBGGRRRBGBGGBBRGGRRGBBRGGBRGBBRRBRRRBGGRRRBGBGG"))
    println(part2("RRYGYBYG"))
    println(part3("RRYGGYBBYG"))
}

fun part1(notes: String): Int = groupTablets(notes, 1).flips

fun part2(notes: String): Int = groupTablets(notes, 10).lengthSum.toInt()

fun part3(notes: String): Long = groupTablets(notes, 100).lengthSum

private data class FlipTotals(val flips: Int, val lengthSum: Long)

private fun groupTablets(notes: String, copies: Int): FlipTotals {
    var shelf = CharArray(notes.length * copies)
    repeat(copies) { notes.toCharArray(shelf, it * notes.length) }
    var next = CharArray(shelf.size)
    var start = 0
    var flips = 0
    var lengthSum = 0L

    while (start < shelf.size) {
        val shade = shelf[start]
        var left = start
        var right = shelf.lastIndex
        var write = next.size
        var forward = true

        // The front run joins the grouped prefix. The tail past the last matching
        // run won't move again for this shade, so save it from back to front.
        while (left <= right) {
            if (forward) {
                while (left <= right && shelf[left] == shade) left++
                while (left <= right && shelf[right] != shade) next[--write] = shelf[right--]
            } else {
                while (left <= right && shelf[right] == shade) right--
                while (left <= right && shelf[left] != shade) next[--write] = shelf[left++]
            }
            if (left > right) break

            // Only the remaining interval flips; read it backwards on the next turn.
            lengthSum += right - left + 1
            flips++
            forward = !forward
        }

        start = write
        val previous = shelf
        shelf = next
        next = previous
    }
    return FlipTotals(flips, lengthSum)
}
