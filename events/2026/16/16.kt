import kotlin.random.Random

fun main() {
    println(part1("RRGGBBGRRGGBBG"))
    println(part1("BBRGGRRGBBRGGBRGBBRRBRRRBGGRRRBGBGGBBRGGRRGBBRGGBRGBBRRBRRRBGGRRRBGBGG"))
    println(part2("RRYGYBYG"))
    println(part3("RRYGGYBBYG"))
}

fun part1(notes: String): Int {
    val list = notes.toCharArray()
    var flips = 0
    var i = 1
    while (true) {
        while (i < list.size && list[i] == list[i - 1])
            i++
        if (i == list.size) break
        val r = list.withIndex().drop(i).findLast { it.value == list[i - 1] }
        if (r == null) {
            i++
            continue
        }
        list.reverse(i, r.index + 1)
        flips++
    }
    return flips
}

fun part2(notes: String): Int {
    val list = notes.repeat(10).toCharArray()
    var sum = 0
    var i = 1
    while (true) {
        while (i < list.size && list[i] == list[i - 1])
            i++
        if (i == list.size) break
        val r = list.withIndex().drop(i).findLast { it.value == list[i - 1] }
        if (r == null) {
            i++
            continue
        }
        list.reverse(i, r.index + 1)
        sum += r.index + 1 - i
    }
    return sum
}

fun part3(notes: String): Long = GroupSorter(notes, 100).sort().second

class Node(val color: Char, var len: Int) {
    var priority = Random.nextInt()
    var left: Node? = null
    var right: Node? = null
    var size = 1
    var totalLen = len.toLong()
    var mask = 1 shl (color - 'A')
    var rev = false
}

private fun update(t: Node?) {
    if (t == null) return
    t.size = 1 + (t.left?.size ?: 0) + (t.right?.size ?: 0)
    t.totalLen = t.len.toLong() + (t.left?.totalLen ?: 0L) + (t.right?.totalLen ?: 0L)
    t.mask = (1 shl (t.color - 'A')) or (t.left?.mask ?: 0) or (t.right?.mask ?: 0)
}

private fun push(t: Node?) {
    if (t == null || !t.rev) return
    t.rev = false
    val tmp = t.left
    t.left = t.right
    t.right = tmp
    t.left?.rev = !(t.left?.rev ?: false)
    t.right?.rev = !(t.right?.rev ?: false)
}

private fun split(t: Node?, k: Int): Pair<Node?, Node?> {
    if (t == null) return null to null
    push(t)
    val leftSize = t.left?.size ?: 0
    if (k <= leftSize) {
        val (a, b) = split(t.left, k)
        t.left = b
        update(t)
        return a to t
    } else {
        val (a, b) = split(t.right, k - leftSize - 1)
        t.right = a
        update(t)
        return t to b
    }
}

private fun merge(a: Node?, b: Node?): Node? {
    if (a == null) return b
    if (b == null) return a
    if (a.priority > b.priority) {
        push(a)
        a.right = merge(a.right, b)
        update(a)
        return a
    } else {
        push(b)
        b.left = merge(a, b.left)
        update(b)
        return b
    }
}

private fun toggleRev(t: Node?) {
    if (t != null) t.rev = !t.rev
}

private fun popLast(t: Node?): Pair<Node?, Node?> {
    if (t == null) return null to null
    push(t)
    if (t.right == null) {
        val newRoot = t.left
        t.left = null
        t.right = null
        update(t)
        return newRoot to t
    } else {
        val (newRight, last) = popLast(t.right)
        t.right = newRight
        update(t)
        return t to last
    }
}

private fun popFirst(t: Node?): Pair<Node?, Node?> {
    if (t == null) return null to null
    push(t)
    if (t.left == null) {
        val newRoot = t.right
        t.left = null
        t.right = null
        update(t)
        return newRoot to t
    } else {
        val (newLeft, first) = popFirst(t.left)
        t.left = newLeft
        update(t)
        return t to first
    }
}

private fun mergeWithBoundary(a: Node?, b: Node?): Node? {
    if (a == null) return b
    if (b == null) return a
    val (a2, last) = popLast(a)
    val (b2, first) = popFirst(b)
    if (last != null && first != null && last.color == first.color) {
        val merged = Node(last.color, last.len + first.len)
        return merge(merge(a2, merged), b2)
    } else {
        return merge(merge(a2, last), merge(first, b2))
    }
}

private fun getFirstColor(t: Node?): Char? {
    if (t == null) return null
    var cur = t
    while (true) {
        push(cur)
        if (cur?.left == null) return cur?.color
        cur = cur.left!!
    }
}

private fun findFirst(t: Node?, badMask: Int, add: Int = 0): Pair<Node, Int>? {
    if (t == null) return null
    push(t)
    val left = t.left
    if (left != null && (left.mask and badMask) != 0) {
        return findFirst(left, badMask, add)
    }
    if ((t.mask and badMask) == 0) return null
    val bit = 1 shl (t.color - 'A')
    if ((bit and badMask) != 0) {
        val idx = add + (left?.size ?: 0)
        return t to idx
    }
    return findFirst(t.right, badMask, add + (left?.size ?: 0) + 1)
}

private fun findLastColor(t: Node?, colorBit: Int, add: Int = 0): Int? {
    if (t == null) return null
    push(t)
    val leftSize = t.left?.size ?: 0
    val right = t.right
    if (right != null && (right.mask and colorBit) != 0) {
        return findLastColor(right, colorBit, add + leftSize + 1)
    }
    if (((1 shl (t.color - 'A')) and colorBit) != 0) {
        return add + leftSize
    }
    return findLastColor(t.left, colorBit, add)
}

class GroupSorter(initial: String, repeat: Int = 1) {
    private val counts = IntArray(26)
    private var badMask = 0
    private var root: Node? = null

    var flips = 0
    var totalLength = 0L

    init {
        val full = if (repeat == 1) initial else initial.repeat(repeat)
        if (full.isNotEmpty()) {
            var color = full[0]
            var len = 1
            for (i in 1 until full.length) {
                if (full[i] == color) {
                    len++
                } else {
                    addGroup(color, len)
                    color = full[i]
                    len = 1
                }
            }
            addGroup(color, len)
        }
        updateBadMask()
    }

    private fun addGroup(c: Char, l: Int) {
        val idx = c - 'A'
        counts[idx]++
        root = merge(root, Node(c, l))
    }

    private fun updateBadMask() {
        var m = 0
        for (i in 0 until 26) {
            if (counts[i] > 1) m = m or (1 shl i)
        }
        badMask = m
    }

    fun sort(): Pair<Int, Long> {
        while (badMask != 0) {
            val firstPair = findFirst(root, badMask) ?: break
            val firstNode = firstPair.first
            val p = firstPair.second
            val X = firstNode.color
            val xBit = 1 shl (X - 'A')
            val q = findLastColor(root, xBit) ?: break

            val (left, rest) = split(root, p)
            val (first, rest2) = split(rest, 1)
            val (mid, right) = split(rest2, q - p)

            val lenMid = mid?.totalLen ?: 0L
            val A = getFirstColor(mid)
            val B = getFirstColor(right)

            val xi = X - 'A'
            counts[xi]--
            if (B != null && A == B) {
                counts[A - 'A']--
            }
            updateBadMask()

            if (mid != null) toggleRev(mid)

            var newRoot = mergeWithBoundary(left, first)
            newRoot = mergeWithBoundary(newRoot, mid)
            newRoot = mergeWithBoundary(newRoot, right)
            root = newRoot

            flips++
            totalLength += lenMid
        }
        return flips to totalLength
    }
}
