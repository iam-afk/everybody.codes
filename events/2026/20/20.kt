fun main() {
    println(
        part1(
            """
            5 - 4 = 9 - 8
            2 + 1 = 3 + 0
            6 + 7 = 4 + 9
            1 + 2 = 3 + 4
            """.trimIndent()
        )
    )
    println(
        part2(
            """
            10 + 2A = 13 + 27
            98 - 54 = 76 - 32
            10 + 12 = 30 - 9
            20 - 10 = 5 + 5
            """.trimIndent()
        )
    )
    println(
        part3(
            """
            1 + 1 = 5 - 1
            1 + 1 = 3 - 3
            0 - 1 = 1 + 7
            1 + 1 = 1 - 6
            1 + 1 = 2 - 1
            1 + 1 = 5 + 7
            1 + 4 = 9 - 9
            1 + 1 = 2 + 9
            1 + 1 = 1 + 2
            """.trimIndent()
        )
    )
    println(
        part3(
            """
            4469 - 5934 = 1631 - 2AA7
            8471 - 2276 = 4510 + 2736
            A767 - 6104 = 1825 + 2538
            9961 + 1140 = 5963 + 2974
            9188 - 406A = 3598 + 1533
            96A6 - 1606 = 4676 - 3524
            9114 - 15A2 = 5258 + 2371
            7023 - 2209 = 1433 + 3436
            921A - 2696 = 2A91 + 3688
            8492 - 2055 = 4186 + 3260
            1234 + 4567 = 8901 + 2345
            """.trimIndent()
        )
    )
}

val segments = mapOf(
    '0' to 6,
    '1' to 2,
    '2' to 5,
    '3' to 5,
    '4' to 4,
    '5' to 5,
    '6' to 6,
    '7' to 3,
    '8' to 7,
    '9' to 6,
    '+' to 2,
    '-' to 1,
    '=' to 2,
    'A' to 6,
)

fun part1(notes: String): Int {
    var ans = 0
    for (line in notes.lines()) {
        val (left, right) = line.split(" = ")
        val (l1, op1, r1) = left.split(' ')
        val (l2, op2, r2) = right.split(' ')
        val lhs = if (op1 == "+") l1.toInt() + r1.toInt() else l1.toInt() - r1.toInt()
        val rhs = if (op2 == "+") l2.toInt() + r2.toInt() else l2.toInt() - r2.toInt()
        if (lhs == rhs)
            ans += line.sumOf { segments[it] ?: 0 }
    }
    return ans
}

fun part2(notes: String): String {
    var ans = 0
    for (line in notes.lines()) {
        val (left, right) = line.split(" = ")
        val (l1, op1, r1) = left.split(' ')
        val (l2, op2, r2) = right.split(' ')
        val lhs = if (op1 == "+") l1.toInt(11) + r1.toInt(11) else l1.toInt(11) - r1.toInt(11)
        val rhs = if (op2 == "+") l2.toInt(11) + r2.toInt(11) else l2.toInt(11) - r2.toInt(11)
        if (lhs == rhs)
            ans += line.sumOf { segments[it] ?: 0 }
    }
    return ans.toString(11)
}

data class T(val remove: List<Char>, val move: List<Char>, val append: List<Char>)

val transforms = mapOf(
    '0' to T(listOf(), listOf('6', '9', 'A'), listOf('8')),
    '1' to T(listOf(), listOf(), listOf('7')),
    '2' to T(listOf(), listOf('3'), listOf()),
    '3' to T(listOf(), listOf('2', '5'), listOf('9')),
    '5' to T(listOf(), listOf('3'), listOf('6', '9')),
    '6' to T(listOf('5'), listOf('0', '9', 'A'), listOf('8')),
    '7' to T(listOf('1'), listOf(), listOf()),
    '8' to T(listOf('0', '6', '9', 'A'), listOf(), listOf()),
    '9' to T(listOf('3', '5'), listOf('0', '6', 'A'), listOf('8')),
    '+' to T(listOf('-'), listOf(), listOf()),
    '-' to T(listOf(), listOf(), listOf('+')),
    'A' to T(listOf(), listOf('0', '6', '9'), listOf('8')),
)

fun part3(notes: String): String {
    var ans = 0

    fun check(expr: String): Boolean {
        val (left, right) = expr.split(" = ")
        val (l1, op1, r1) = left.split(' ')
        val (l2, op2, r2) = right.split(' ')
        val lhs = if (op1 == "+") l1.toInt(11) + r1.toInt(11) else l1.toInt(11) - r1.toInt(11)
        val rhs = if (op2 == "+") l2.toInt(11) + r2.toInt(11) else l2.toInt(11) - r2.toInt(11)
        return lhs == rhs
    }

    fun doTransform(line: String): Boolean {
        val chars = line.toCharArray()
        for ((i, c) in chars.withIndex()) {
            val t = transforms[c] ?: continue
            for (x in t.move) {
                chars[i] = x
                if (check(chars.joinToString(""))) return true
            }
            for (j in i + 1 until chars.size) {
                val d = chars[j]
                val s = transforms[d] ?: continue
                for (c1 in t.remove) {
                    chars[i] = c1
                    for (c2 in s.append) {
                        chars[j] = c2
                        if (check(chars.joinToString(""))) return true
                    }
                }
                for (c1 in t.append) {
                    chars[i] = c1
                    for (c2 in s.remove) {
                        chars[j] = c2
                        if (check(chars.joinToString(""))) return true
                    }
                }
                chars[j] = d
            }
            chars[i] = c
        }
        return false
    }

    for (line in notes.lines()) {
        if (check(line) || doTransform(line)) {
            ans += line.sumOf { segments[it] ?: 0 }
        }
    }
    return ans.toString(11)
}
