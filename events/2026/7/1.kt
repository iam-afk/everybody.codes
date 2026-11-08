fun part1(notes: String): Int {
    data class Cloud(val id: String, var energy: Int, val connections: List<String>)

    val clouds = buildMap {
        for (line in notes.lines()) {
            val (id, energy, connections) = line.split(", ")
            put(
                id.substringAfter('='),
                Cloud(
                    id.substringAfter('='),
                    energy.substringAfter('=').toInt(),
                    connections.substringAfter('=').split(',')
                )
            )
        }
    }

    data class Q(val id: String, val energy: Int)

    val queue = ArrayDeque<Q>();
    queue.add(Q(clouds.values.maxBy { it.connections.size }.id, 5))
    while (true) {
        val (id, energy) = queue.removeFirstOrNull() ?: break
        val cloud = clouds[id]!!
        cloud.energy += energy
        if (energy > 1)
            for (other in cloud.connections) {
                queue.add(Q(other, energy - 1))
            }

    }

    return clouds.values.sumOf(Cloud::energy)
}

fun part2(notes: String): Int {
    data class Cloud(val id: String, var energy: Int, val connections: List<String>)

    val clouds = buildMap {
        for (line in notes.lines()) {
            val (id, energy, connections) = line.split(", ")
            put(
                id.substringAfter('='),
                Cloud(
                    id.substringAfter('='),
                    energy.substringAfter('=').toInt(),
                    connections.substringAfter('=').split(',')
                )
            )
        }
    }

    data class Q(val id: String, val energy: Int)

    var stage = mapOf(Q(clouds.values.maxBy { it.connections.size }.id, 10) to 1)
    while (stage.isNotEmpty()) {
        stage = buildMap {
            for ((key, count) in stage) {
                val cloud = clouds[key.id]!!
                cloud.energy += key.energy * count
                if (key.energy > 1)
                    for (other in cloud.connections) {
                        compute(Q(other, key.energy - 1)) { _, v -> (v ?: 0) + count }
                    }
            }
        }
    }
    return clouds.values.sumOf(Cloud::energy)
}

fun part3(notes: String, limit: Int = 1000000): Int {
    data class Cloud(val id: String, var energy: Int, val connections: List<String>, var exploded: Boolean = false)

    val clouds = buildMap {
        for (line in notes.lines()) {
            val (id, energy, connections) = line.split(", ")
            put(
                id.substringAfter('='),
                Cloud(
                    id.substringAfter('='),
                    energy.substringAfter('=').toInt(),
                    connections.substringAfter('=').split(',')
                )
            )
        }
    }

    data class Q(val id: String, val energy: Int)
    return clouds.keys.maxOf { id ->
        val copy = buildMap {
            for ((k, v) in clouds) {
                put(k, v.copy())
            }
        }
        var stage = mapOf(Q(id, 10) to 1)
        while (stage.isNotEmpty()) {
            stage = buildMap {
                for ((key, count) in stage) {
                    val cloud = copy[key.id]!!
                    if (cloud.exploded) continue
                    cloud.energy += key.energy * count
                    if (key.energy > 1)
                        for (other in cloud.connections) {
                            compute(Q(other, key.energy - 1)) { _, v -> (v ?: 0) + count }
                        }
                }
                for (cloud in copy.values)
                    if (!cloud.exploded && cloud.energy >= limit) {
                        cloud.exploded = true
                        for (other in cloud.connections) {
                            compute(Q(other, 10), { _, v -> (v ?: 0) + 1 })
                        }
                    }
            }
        }
        copy.values.filterNot<Cloud>(Cloud::exploded).sumOf<Cloud>(Cloud::energy)
    }
}

fun main() {
    println(part1(example1))
    println(part1(notes1))
    println(part2(example2))
    println(part2(notes2))
    println(part3(example3, 100))
    println(part3(notes3))
}

val example1 = """
    id=1, energy=5, connections=2,5,7
    id=2, energy=9, connections=6,7
    id=3, energy=3, connections=4
    id=4, energy=8, connections=3
    id=5, energy=1, connections=6,7
    id=6, energy=7, connections=1,7
    id=7, energy=6, connections=5
""".trimIndent()

val example2 = """
    id=1, energy=5, connections=2,5,7
    id=2, energy=9, connections=6,7
    id=3, energy=3, connections=4
    id=4, energy=8, connections=3
    id=5, energy=1, connections=6,7
    id=6, energy=7, connections=1,7
    id=7, energy=6, connections=5
""".trimIndent()

val example3 = """
    id=1, energy=5, connections=2,5,7
    id=2, energy=9, connections=6,7
    id=3, energy=3, connections=4
    id=4, energy=8, connections=3
    id=5, energy=1, connections=6,7
    id=6, energy=7, connections=1,7
    id=7, energy=6, connections=5
""".trimIndent()

val notes1 = """
    id=1, energy=7, connections=2
    id=2, energy=9, connections=13,15
    id=3, energy=4, connections=4
    id=4, energy=5, connections=1
    id=5, energy=9, connections=4,8
    id=6, energy=6, connections=1,11
    id=7, energy=5, connections=4,9
    id=8, energy=3, connections=1
    id=9, energy=8, connections=3,5
    id=10, energy=5, connections=3,17
    id=11, energy=7, connections=1,3,9,14,16,19
    id=12, energy=5, connections=1,5,15,19
    id=13, energy=2, connections=2,5,17
    id=14, energy=2, connections=10
    id=15, energy=4, connections=1,3,5,6,9,10,17,20
    id=16, energy=7, connections=2,6,8,14,15,17
    id=17, energy=8, connections=1,13
    id=18, energy=8, connections=1,7
    id=19, energy=8, connections=3,7,18,20
    id=20, energy=3, connections=1
""".trimIndent()

val notes2 = """
    id=1, energy=9, connections=3,7,30,99
    id=2, energy=1, connections=64,79,82
    id=3, energy=2, connections=9,29,50,58
    id=4, energy=9, connections=21,57
    id=5, energy=4, connections=28,34,41,42,56,59,66,84,100
    id=6, energy=2, connections=4,20,38,69,81,90
    id=7, energy=4, connections=3,10,12,27,50,58,65,97
    id=8, energy=3, connections=9,15,36,59
    id=9, energy=9, connections=10,18,22,27,63,65,79,87
    id=10, energy=8, connections=14,16,17,31,39,67,76,95,98
    id=11, energy=3, connections=25,58,73,96
    id=12, energy=9, connections=3,30
    id=13, energy=1, connections=10,26,41,44,47,58,78,83,100
    id=14, energy=9, connections=22,39,43,55,59,75
    id=15, energy=4, connections=8,12,20,22,28,31,40,47,76,82,89,94
    id=16, energy=3, connections=28,51,53,65,80,84
    id=17, energy=2, connections=57
    id=18, energy=3, connections=7,19,29,40,46,94
    id=19, energy=7, connections=62,65,79,84
    id=20, energy=6, connections=4,19,28,31,47,88
    id=21, energy=2, connections=8,18,48,92
    id=22, energy=1, connections=11,18,35,80,86,90
    id=23, energy=2, connections=7,56,60
    id=24, energy=2, connections=8,12,22,29,48,55,61,62,69,75,95,97
    id=25, energy=7, connections=34,93
    id=26, energy=1, connections=14,17,20,29,52,63,78,90
    id=27, energy=2, connections=12,20,75,88
    id=28, energy=9, connections=11,18,19,43,59,82
    id=29, energy=6, connections=7,34,54,63,73,81,83,99
    id=30, energy=1, connections=20,29,32,56,68,69,79,83,98
    id=31, energy=1, connections=5,30,57,72
    id=32, energy=7, connections=8,28,31,44,50,54,55,65,75,79,80,85
    id=33, energy=5, connections=4,7,9,15,21,24,31,37,45,49,55,62,73,82,96,100
    id=34, energy=3, connections=12,48,100
    id=35, energy=2, connections=4,12,31,36,44,59,66,92
    id=36, energy=9, connections=3,4,16,24,27,31,32,37,63,75,93,97
    id=37, energy=2, connections=16,22,46,50,63,96
    id=38, energy=8, connections=18,26,35,81
    id=39, energy=5, connections=32
    id=40, energy=8, connections=20,28,51,95
    id=41, energy=2, connections=6,19,42,61,94,96
    id=42, energy=1, connections=5,11,18,28,29,39,43,62,82
    id=43, energy=7, connections=10,39
    id=44, energy=1, connections=16,18,20,46,48,80,89,95,100
    id=45, energy=2, connections=4,66,73,94
    id=46, energy=7, connections=13,21,33,41,54,81
    id=47, energy=8, connections=1,6,54,58,62,81
    id=48, energy=7, connections=6,9,13,20,21,24,25,40,46,56,97,100
    id=49, energy=8, connections=9,25,30,31,42,71,82,98
    id=50, energy=1, connections=4,8,11,17,18,20,40,43,44,49,50,52,55,60,66,72,75,80,81,86,88,89,94,96,98,99
    id=51, energy=2, connections=25,60
    id=52, energy=4, connections=6,11,25,29,48,76
    id=53, energy=7, connections=23,26,60,88
    id=54, energy=1, connections=4,5,16,20,21,46,63,87,90
    id=55, energy=6, connections=16,27,39,61
    id=56, energy=3, connections=8,9,14,45
    id=57, energy=6, connections=12,37,87,91
    id=58, energy=6, connections=20,21,33,39,53,78
    id=59, energy=7, connections=4,20,23,35,36,38,43,49,51,53,57,65,77,79,84,88
    id=60, energy=3, connections=4,13,19,28,46,51,80,86
    id=61, energy=5, connections=13,36,76,77,91,96
    id=62, energy=6, connections=28,32,47,63,67,93
    id=63, energy=1, connections=6,8,9,20,24,27,32,48,54,56,67,68,73,77,81,87
    id=64, energy=2, connections=4,5,54,58,61,75,97,100
    id=65, energy=3, connections=7,27,39,42,64,85
    id=66, energy=5, connections=7,10,20,35,61,64
    id=67, energy=7, connections=9,12,22,27,36,38,58,66,74
    id=68, energy=4, connections=17,60
    id=69, energy=8, connections=9,32,34,40,45,46,49,53,57,60,73,76,85,95,96,98
    id=70, energy=8, connections=16
    id=71, energy=6, connections=10,15,54,55,79,100
    id=72, energy=7, connections=5,7,13,46,48,52
    id=73, energy=9, connections=1,8,15,23,31,40,51,53,56,59,60,65,80,86,87,91
    id=74, energy=3, connections=15,25,33,83
    id=75, energy=4, connections=25,30,36,46,58,59,63,92
    id=76, energy=7, connections=25,29,41
    id=77, energy=9, connections=31,32,57
    id=78, energy=7, connections=42,79
    id=79, energy=4, connections=25,50,51,55,94,95
    id=80, energy=1, connections=18,20,29,31,44,45,59,62,65,73,84,95
    id=81, energy=3, connections=11,12,15,26,27,34,45,48,75,79,87,97
    id=82, energy=5, connections=14,59,70,100
    id=83, energy=5, connections=7,17,47,59,89,98
    id=84, energy=1, connections=20,34,37,91
    id=85, energy=5, connections=1,43,95,98
    id=86, energy=5, connections=5,39,60,87
    id=87, energy=1, connections=2,27,30,35,60,88
    id=88, energy=3, connections=11,57,62,63,70,81
    id=89, energy=4, connections=7,21,46,57,61,66,67,77,91,95,96,99
    id=90, energy=8, connections=34,59,74,77
    id=91, energy=5, connections=3,11,21,30,31,33,54,55,65,70,79,92
    id=92, energy=4, connections=1,9,16,26,48,56,65,79,97
    id=93, energy=6, connections=4,8,17,38,43,53,65,100
    id=94, energy=4, connections=2,35,40
    id=95, energy=5, connections=39,46,68,70
    id=96, energy=4, connections=5,10,27,34,41,62,63,76,80
    id=97, energy=2, connections=12,20,38,53,59,62,82,98
    id=98, energy=8, connections=9,57,88
    id=99, energy=5, connections=11,15,29,31,51,69,78,89
    id=100, energy=4, connections=18,34,41,55,68,94
""".trimIndent()

val notes3 = """
    id=1, energy=9, connections=9,10,23,26,35,42,45,50,52,53,56,66,67,89,98,100
    id=2, energy=5, connections=18,75
    id=3, energy=1, connections=28,33
    id=4, energy=4, connections=5,25,45,75,80,87
    id=5, energy=2, connections=3,17,63,93
    id=6, energy=9, connections=28,34,71,90
    id=7, energy=5, connections=6,13,16,25,32,36,39,46,60,61,63,83,84,85,94,100
    id=8, energy=1, connections=6,12,14,41,49,53,84,90
    id=9, energy=9, connections=2,30,38,39,48,56,68,85,90
    id=10, energy=9, connections=23,61,69,79,87,94
    id=11, energy=7, connections=2,9,17,26,28,33,44,51,57,60,63,67,74,85,95,100
    id=12, energy=6, connections=3,11,30,41,52,57,58,72,80,88,95,97
    id=13, energy=1, connections=2,14,22,40,41,57,60,69,80,84,94,98
    id=14, energy=3, connections=5,7,11,21,23,51,53,65,89
    id=15, energy=4, connections=1,27,49,55,69,71,72,84,87,88,90,95
    id=16, energy=6, connections=15,21,24,26,28,43,51,52,56,61,81,86
    id=17, energy=3, connections=5,16,20,21,24,37,41,72,78,86,88,94
    id=18, energy=8, connections=5,15,20,53,60,61,65,72,78,87,96,98
    id=19, energy=5, connections=10,13,47,94
    id=20, energy=7, connections=5,13,36,49,73,74,83,98
    id=21, energy=2, connections=10,11,12,29,32,35,40,47,58,65,79,96
    id=22, energy=1, connections=21,35,65,100
    id=23, energy=7, connections=4,5,9,10,28,30,31,36,37,54,60,71,79,82,84,97
    id=24, energy=9, connections=26,30,31,38,41,71,83,98
    id=25, energy=1, connections=2,8,24,26,27,46,53,59,84,86,87,98
    id=26, energy=2, connections=50,69,70,99
    id=27, energy=7, connections=44,65,93
    id=28, energy=4, connections=6,10,17,26,30,31,67,91
    id=29, energy=5, connections=4,10,16,23,43,46,74,76,79,81,89,97
    id=30, energy=7, connections=10,16,23,28,36,69,78,82,95
    id=31, energy=8, connections=3,8,15,18,19,26,27,28,33,43,48,50,67,72,92,94
    id=32, energy=8, connections=5,44,47,48,49,51,52,88
    id=33, energy=9, connections=15,94,98
    id=34, energy=9, connections=4,35,78
    id=35, energy=8, connections=3,4,5,41,68,71,81,86,88
    id=36, energy=6, connections=10,20,42,43,51,64,65,66,68,72,97,100
    id=37, energy=4, connections=11,23,30,58,97,98
    id=38, energy=4, connections=15,18,45,69,75,76,80,90,96
    id=39, energy=3, connections=6,8,16,45,50,88
    id=40, energy=6, connections=67,82
    id=41, energy=9, connections=19,34,50,54,80,100
    id=42, energy=1, connections=3,4,14,27,28,41,59,86
    id=43, energy=4, connections=1,5,8,9,17,27,36,37,41,47,55,62,69,74,75,85
    id=44, energy=4, connections=7,10,20,39,46,67,70,83
    id=45, energy=7, connections=5,26,32,36,46,50,66,80,97
    id=46, energy=5, connections=4,31,45,72
    id=47, energy=8, connections=17,74
    id=48, energy=9, connections=7,14,97
    id=49, energy=2, connections=10,20,30,34,41,51,57,67,74,79,81,85
    id=50, energy=5, connections=48,53,72,95
    id=51, energy=9, connections=7,10,18,76,92,97
    id=52, energy=9, connections=19,38,55,75,78,82,84,92,99
    id=53, energy=6, connections=5,21,84,98
    id=54, energy=2, connections=5,19,24,33,34,46
    id=55, energy=8, connections=3,12,27,67,68,71
    id=56, energy=2, connections=1,4,18,29,37,44,45,52,54,59,61,62,74,77,88,96
    id=57, energy=8, connections=3,9,18,21,51,75,88,91
    id=58, energy=9, connections=2,3,15,21,26,41,43,52,66,72,86,99
    id=59, energy=4, connections=83,84,87
    id=60, energy=7, connections=2,7,14,41,49,54,79,88,90
    id=61, energy=8, connections=38,46,50,58,66,82
    id=62, energy=2, connections=13,14,19,31,50,66,67,68,79,80,82,95
    id=63, energy=5, connections=3,6,21,51,71,75,78,80,98
    id=64, energy=3, connections=1,3,6,15,26,27,38,49,63,67,73,91
    id=65, energy=4, connections=17,19,32,44,46,55,56,58,63,75,85,95
    id=66, energy=8, connections=1,5,8,22,30,32,38,48,71,75,79,95
    id=67, energy=6, connections=13,20,35,51,56,77,84,88,95
    id=68, energy=4, connections=63,73
    id=69, energy=5, connections=1,7,15,19,25,32,34,37,46,63,75,87,88,91,93,96
    id=70, energy=3, connections=5,15,33,36,42,51,59,62,68,87,92,95
    id=71, energy=1, connections=6,34,37,41,44,50,52,55,64,69,78,81,84,91,92,95
    id=72, energy=3, connections=7,11,22,41,47,52,63,73,77,78,80,81,83,85,92,94
    id=73, energy=7, connections=4,12,34,47,54,74,91,94,96
    id=74, energy=5, connections=5,22,35,36,41,60,80,82
    id=75, energy=3, connections=1,26,28,30,53,74,85,87,88,89,97,100
    id=76, energy=5, connections=3,55,69,88
    id=77, energy=9, connections=24,52,72,94
    id=78, energy=5, connections=5,31,41,46,63,64,65,92
    id=79, energy=5, connections=17,40,49,63
    id=80, energy=4, connections=4,19,26,49,53,61,81,99
    id=81, energy=3, connections=5,9,12,13,31,32,42,44,48,49,61,62,82,85,94,99
    id=82, energy=6, connections=25,28,34,37,40,41,48,51,85,90,97,100
    id=83, energy=9, connections=8,9,40,45,55,56,67,79,84,85,87,95
    id=84, energy=1, connections=16,31,47,59,63,69
    id=85, energy=8, connections=20,24,43,48,50,53,81,95,100
    id=86, energy=7, connections=6,45,49,55,59,71,80,95
    id=87, energy=5, connections=7,42,46,86
    id=88, energy=8, connections=10,28,36,47,74,93
    id=89, energy=1, connections=40,61,93,97
    id=90, energy=8, connections=4,6,21,25,29,32,33,41,47,55,66,97
    id=91, energy=1, connections=85,92
    id=92, energy=6, connections=1,7,9,19,26,27,29,36,41,42,45,63,64,66,69,94
    id=93, energy=8, connections=27,48,91,94
    id=94, energy=2, connections=8,20,22,37,41,56,58,66,76,78,79,90
    id=95, energy=5, connections=21,34,47,55,93,94
    id=96, energy=9, connections=12,13,19,20,24,42,47,56,65,84,86,100
    id=97, energy=4, connections=1,3,5,18,23,26,29,30,35,56,63,88
    id=98, energy=6, connections=23,39,44,58,64,65,73,89
    id=99, energy=7, connections=6,31,44,55,58,62,63,81,87
    id=100, energy=8, connections=32,60,73,96
""".trimIndent()