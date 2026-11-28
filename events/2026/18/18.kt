/*  */fun part1(notes: String): Long = mouldMaterial(notes)

fun part2(notes: String): Long = mouldMaterial(notes)

fun part3(notes: String): Long = mouldMaterial(notes)

private data class Stack(val left: Long, val right: Long, val height: Long)

private fun mouldMaterial(notes: String): Long {
    val stacks = notes.lineSequence().filter { it.isNotBlank() }.map { line ->
        val fields = line.trim().split(' ', '\t').filter { it.isNotEmpty() }
        val (x, y) = fields
        val left = x.removePrefix("x=").toLong()
        val height = y.removePrefix("y=").toLong()
        val width = fields.getOrNull(2)?.removePrefix("width=")?.toLong() ?: 1L
        require(width > 0) { "Stack width must be positive: $line" }
        Stack(left, left + (width - 1), height)
    }.sortedByDescending { it.height }.toList()
    val tallest = stacks.firstOrNull() ?: return 0

    var left = tallest.left
    var right = tallest.right
    var height = tallest.height
    var material = 0L
    for (i in 1 until stacks.size) {
        val stack = stacks[i]
        // Rows above this stack's top still have the previous span.
        material += (height - stack.height) * (right - left + 1)
        left = minOf(left, stack.left)
        right = maxOf(right, stack.right)
        height = stack.height
    }
    return material + height * (right - left + 1)
}
