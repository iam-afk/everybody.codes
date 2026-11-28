package everybody.codes.plugin

import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.io.TempDir

class EverybodyCodesPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private fun createQuest(): File {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "fixture"
            include("2026:1")
            """.trimIndent()
        )

        val quest = projectDir.resolve("2026/1").apply { mkdirs() }
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("everybody.codes") apply false }
            project(":2026:1") {
                apply(plugin = "everybody.codes")
                repositories { mavenCentral() }
            }
            """.trimIndent()
        )
        return quest
    }

    private fun runner(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments)

    @Test
    fun `only API tasks require a session cookie`() {
        createQuest()
        runner("help", "--offline", "--configuration-cache").build()

        val download = runner(":2026:1:downloadState", "--offline")
        assertContains(download.buildAndFail().output, "Cannot read `.everybody-codes` in the project root")

        projectDir.resolve(".everybody-codes").writeText(" \n")
        assertContains(download.buildAndFail().output, "no session cookie in `.everybody-codes`")
    }

    @Test
    fun `answers follow changed notes and disappear when the part locks`() {
        val quest = createQuest()
        quest.resolve("Quest.kt").writeText(
            """
            fun part1(notes: String): String {
                println("called part1 solver")
                return "solved: " + notes
            }
            fun part2(notes: String): String = error("called locked part2 solver")
            fun part3(notes: String): String = error("called locked part3 solver")
            """.trimIndent()
        )

        val files = quest.resolve("build/everybody_codes").apply { mkdirs() }
        val notes = files.resolve("notes.json")
        val state = files.resolve("state.json")
        val decrypted = files.resolve("notes1.txt")
        val answer = files.resolve("answer1.txt")
        val key = "0123456789abcdef0123456789abcdef"
        val runner = runner(":2026:1:run", "-x", ":2026:1:downloadNotes", "-x", ":2026:1:downloadState")

        state.writeText("""{"key1":"$key"}""")
        for (text in listOf("first notes", "replacement notes for the same solver")) {
            notes.writeText("""{"1":"${encrypt(text, key)}"}""")
            runner.build()
            assertEquals(text, decrypted.readText())
            assertEquals("solved: $text", answer.readText())
        }

        state.writeText("{}")
        val locked = runner.build()
        assertFalse(decrypted.exists(), "Locking part 1 should remove its decrypted notes")
        assertFalse(answer.exists(), "Locking part 1 should remove its last answer")
        assertFalse("called part1 solver" in locked.output, "The locked solver must not run")
    }

    @Test
    fun `submit selects the current part when reusing the configuration cache`() {
        val files = createQuest().resolve("build/everybody_codes").apply { mkdirs() }
        val state = files.resolve("state.json")
        val submit = runner(
            ":2026:1:submit", "-x", ":2026:1:run", "-x", ":2026:1:downloadState",
            "--configuration-cache", "--offline", "--info"
        )

        state.writeText("""{"key1":"unlocked"}""")
        assertContains(submit.build().output, "no answer found for part 1")

        files.resolve("answer1.txt").writeText("already submitted")
        state.writeText("""{"answer1":"already submitted","key2":"unlocked"}""")
        val second = submit.build()
        assertContains(second.output, "Configuration cache entry reused.")
        assertContains(second.output, "no answer found for part 2")
    }

    @Test
    fun `notes follow the seed while state refreshes on every build`() {
        val quest = createQuest()
        quest.resolve("build.gradle.kts").writeText(
            """
            import everybody.codes.plugin.DownloadNotes
            import everybody.codes.plugin.DownloadState

            val remoteNotes = file("remote-notes.json")
            val remoteState = file("remote-state.json")
            tasks.named<DownloadNotes>("downloadNotes") {
                seed.set(providers.gradleProperty("testSeed").map { it.toLong() })
                val target = notesFile
                actions.clear()
                doLast { target.get().asFile.writeText(remoteNotes.readText()) }
            }
            tasks.named<DownloadState>("downloadState") {
                val target = stateFile
                actions.clear()
                doLast { target.get().asFile.writeText(remoteState.readText()) }
            }
            """.trimIndent()
        )
        val notesTask = ":2026:1:downloadNotes"
        val stateTask = ":2026:1:downloadState"
        fun download(seed: Long) = runner(notesTask, stateTask, "-PtestSeed=$seed", "--offline").build()

        val remoteNotes = quest.resolve("remote-notes.json")
        val remoteState = quest.resolve("remote-state.json")
        val notes = quest.resolve("build/everybody_codes/notes.json")
        val state = quest.resolve("build/everybody_codes/state.json")
        remoteNotes.writeText("""{"1":"original notes"}""")
        remoteState.writeText("{}")
        val first = download(1)
        assertEquals(SUCCESS, first.task(notesTask)?.outcome)
        assertEquals(SUCCESS, first.task(stateTask)?.outcome)
        assertEquals(remoteNotes.readText(), notes.readText())
        assertEquals(remoteState.readText(), state.readText())

        remoteNotes.writeText("""{"1":"new"}""")
        remoteState.writeText("""{"key1":"unlocked"}""")
        val sameSeed = download(1)
        assertEquals(UP_TO_DATE, sameSeed.task(notesTask)?.outcome)
        assertEquals(SUCCESS, sameSeed.task(stateTask)?.outcome)
        assertEquals("""{"1":"original notes"}""", notes.readText())
        assertEquals(remoteState.readText(), state.readText())

        val newSeed = download(2)
        assertEquals(SUCCESS, newSeed.task(notesTask)?.outcome)
        assertEquals(remoteNotes.readText(), notes.readText())
    }

    private fun encrypt(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(), "AES"),
            IvParameterSpec(key.take(16).toByteArray())
        )
        return cipher.doFinal(text.toByteArray()).toHexString()
    }
}
