package everybody.codes.plugin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File
import java.io.IOException
import java.net.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Duration
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.milliseconds

interface Parameters : BuildServiceParameters {
    val cookieFile: RegularFileProperty
}

@Serializable
data class User(
    val id: Long,
    val seed: Long
)

@Serializable
data class Status(
    @get:Input @get:Optional val key1: String?,
    @get:Input @get:Optional val answer1: String?,
    @get:Input @get:Optional val key2: String?,
    @get:Input @get:Optional val answer2: String?,
    @get:Input @get:Optional val key3: String?,
    @get:Input @get:Optional val answer3: String?
) {
    fun key(part: String): String? = when (part) {
        "1" -> key1
        "2" -> key2
        "3" -> key3
        else -> throw IllegalArgumentException()
    }
}

@Serializable
data class Answer(val answer: String)

@Serializable
data class Verdict(
    val correct: Boolean,
    val lengthCorrect: Boolean?,
    val firstCorrect: Boolean?,
    val penaltyUntil: Long?,
    val penaltyLeftMs: Long?
)

internal fun Verdict.formatResult(): String {
    if (correct) return "correct!"

    val hints = listOfNotNull(
        lengthCorrect?.let { if (it) "length correct" else "length wrong" },
        firstCorrect?.let { if (it) "first character correct" else "first character wrong" }
    )
    return buildString {
        append("WRONG ANSWER")
        if (hints.isNotEmpty()) append("\n${hints.joinToString(", ")}")
        if (penaltyLeftMs != null && penaltyLeftMs > 0) {
            append("\nRetry in ${penaltyLeftMs.milliseconds}.")
        } else if (penaltyLeftMs == null && penaltyUntil != null && penaltyUntil > 0) {
            append("\nCooldown until: $penaltyUntil (server timestamp)")
        }
    }
}

val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> HttpClient.downloadJson(uri: URI, output: Path) {
    val staged = Files.createTempFile(output.toAbsolutePath().parent, ".${output.fileName}.", ".tmp")
    try {
        val response = send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofFile(staged))
        if (response.statusCode() !in 200..299) {
            throw GradleException("Failed to download ${output.fileName}: HTTP ${response.statusCode()}")
        }
        try {
            Files.newInputStream(staged).use { json.decodeFromStream<T>(it) }
        } catch (_: SerializationException) {
            throw GradleException("Invalid JSON response for ${output.fileName}")
        }
        Files.move(staged, output, ATOMIC_MOVE, REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(staged)
    }
}

abstract class ApiService : BuildService<Parameters>, AutoCloseable {
    private val client: HttpClient
    val seed: Long

    init {
        val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_NONE).apply {
            val cookie = HttpCookie("everybody-codes", readCookie(parameters.cookieFile.get().asFile)).apply {
                domain = "api.everybody.codes"
                path = "/"
                version = 0
            }
            cookieStore.add(null, cookie)
        }
        client = HttpClient.newBuilder()
            .proxy(ProxySelector.of(InetSocketAddress("127.0.0.1", 8118)))
            .connectTimeout(Duration.ofSeconds(5))
            .cookieHandler(cookieManager)
            .build()

        val request = HttpRequest.newBuilder(URI("https://api.everybody.codes/user/me")).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299)
            throw RuntimeException("failed to fetch seed")
        seed = json.decodeFromString<User>(response.body()).seed
    }

    fun fetchNotes(event: String, quest: String, seed: Long, output: Path) {
        client.downloadJson<Map<String, String>>(
            URI("https://everybody.codes/assets/$event/$quest/input/$seed.json"),
            output
        )
    }

    fun state(event: String, quest: String, output: Path) {
        client.downloadJson<Status>(URI("https://api.everybody.codes/event/$event/quest/$quest"), output)
    }

    fun submit(event: String, quest: String, part: String, answer: String): Verdict {
        val body = json.encodeToString(Answer(answer))
        val response = client.send(
            HttpRequest.newBuilder(URI("https://api.everybody.codes/event/$event/quest/$quest/part/$part/answer"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        return when (response.statusCode()) {
            in 200..299 -> json.decodeFromString<Verdict>(response.body())
            409 -> throw RuntimeException("failed to submit: ${response.statusCode()} conflict")
            423 -> throw RuntimeException("failed to submit: ${response.statusCode()} locked")
            else -> throw RuntimeException("failed to submit: ${response.statusCode()}")
        }
    }

    override fun close() {
        client.close()
    }
}

abstract class DownloadNotes : DefaultTask() {

    @get:ServiceReference("api")
    abstract val api: Property<ApiService>

    @get:Input
    abstract val event: Property<String>

    @get:Input
    abstract val quest: Property<String>

    @get:Input
    abstract val seed: Property<Long>

    @get:OutputFile
    abstract val notesFile: RegularFileProperty

    @TaskAction
    fun download() {
        api.get().fetchNotes(event.get(), quest.get(), seed.get(), notesFile.get().asFile.toPath())
    }

}

@UntrackedTask(because = "Quest state can change on the website between builds")
abstract class DownloadState : DefaultTask() {

    @get:ServiceReference("api")
    abstract val api: Property<ApiService>

    @get:Input
    abstract val event: Property<String>

    @get:Input
    abstract val quest: Property<String>

    @get:OutputFile
    abstract val stateFile: RegularFileProperty

    @TaskAction
    fun download() {
        api.get().state(event.get(), quest.get(), stateFile.get().asFile.toPath())
    }

}

class PartArguments(
    // Locked parts have a path but no file, so use InputFiles rather than InputFile.
    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE) val notes: Provider<RegularFile>,
    @get:Internal val answer: Provider<RegularFile>
) : CommandLineArgumentProvider {
    override fun asArguments() = listOf(notes.get().asFile.absolutePath, answer.get().asFile.absolutePath)
}

class EverybodyCodesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(KotlinPluginWrapper::class.java)
        project.tasks.withType(KotlinJvmCompile::class.java).configureEach {
            it.jvmTargetValidationMode.set(JvmTargetValidationMode.IGNORE)
        }

        val api = project.gradle.sharedServices.registerIfAbsent("api", ApiService::class.java) { spec ->
            spec.parameters.cookieFile.set(project.layout.settingsDirectory.file(".everybody-codes"))
        }

        val downloadNotes = project.tasks.register("downloadNotes", DownloadNotes::class.java) { task ->
            task.event.set(project.parent!!.name)
            task.quest.set(project.name)
            task.seed.set(api.map { it.seed })
            task.notesFile.set(project.layout.buildDirectory.file("everybody_codes/notes.json"))
        }

        val downloadState = project.tasks.register("downloadState", DownloadState::class.java) { task ->
            task.event.set(project.parent!!.name)
            task.quest.set(project.name)
            task.stateFile.set(project.layout.buildDirectory.file("everybody_codes/state.json"))
        }

        val parts = (1..3).map { part ->
            val decrypted = project.layout.buildDirectory.file("everybody_codes/notes$part.txt")
            val answer = project.layout.buildDirectory.file("everybody_codes/answer$part.txt")
            val decrypt = project.tasks.register("decryptNotes$part", DecryptNotes::class.java) { task ->
                task.part.set(part.toString())
                task.notesFile.set(downloadNotes.flatMap { it.notesFile })
                task.statusFile.set(downloadState.flatMap { it.stateFile })
                task.decryptedNotes.set(decrypted)
            }
            PartArguments(decrypt.flatMap { it.decryptedNotes }, answer)
        }

        val generateMain = project.tasks.register("generateMain", GenerateMain::class.java) { task ->
            task.outputDir.set(project.layout.buildDirectory.dir("everybody_codes/gen"))
        }

        val sourceSetMain = project.extensions.getByType(KotlinSourceSetContainer::class.java)
            .sourceSets.getByName("main")
        sourceSetMain.kotlin.setSrcDirs(listOf(project.layout.projectDirectory))
        sourceSetMain.generatedKotlin.srcDir(generateMain)

        val run = project.tasks.register("run", JavaExec::class.java) { task ->
            task.classpath = project.files(
                project.extensions.getByType(SourceSetContainer::class.java).getByName("main").runtimeClasspath
            )
            task.mainClass.set("MainKt")
            task.argumentProviders.addAll(parts)
            task.outputs.files(parts.map { it.answer })
        }

        project.tasks.register("submit", Submit::class.java) { task ->
            task.dependsOn(run)
            parts.forEach { task.answerFiles.add(it.answer) }
            task.statusFile.set(downloadState.flatMap { it.stateFile })
        }
    }
}

private fun readCookie(file: File): String {
    val cookie = try {
        file.readText().trim()
    } catch (e: IOException) {
        throw GradleException(
            "Cannot read `${file.name}` in the project root. API tasks require a session cookie in that file.",
            e
        )
    }
    if (cookie.isEmpty()) {
        throw GradleException("no session cookie in `${file.name}`")
    }
    return cookie
}

fun decrypt(encrypted: String, key: String): String {
    val encryptedBytes = encrypted.hexToByteArray()
    val keyBytes = key.toByteArray()
    val ivBytes = key.substring(0, 16).toByteArray()

    val secretKeySpec = SecretKeySpec(keyBytes, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(ivBytes))

    return cipher.doFinal(encryptedBytes).decodeToString()
}

abstract class DecryptNotes : DefaultTask() {
    @get:InputFile
    abstract val notesFile: RegularFileProperty

    @get:InputFile
    abstract val statusFile: RegularFileProperty

    @get:Internal
    val status = statusFile.map { file ->
        val value = file.asFile.readText()
        json.decodeFromString<Status>(value)
    }

    @get:Input
    abstract val part: Property<String>

    @get:OutputFile
    abstract val decryptedNotes: RegularFileProperty

    @TaskAction
    fun decryptNotes() {
        val part = part.get()
        val key = status.get().key(part)
        if (key == null) {
            Files.deleteIfExists(decryptedNotes.get().asFile.toPath())
            return
        }
        val notes = notesFile.get().asFile.inputStream().use {
            json.decodeFromStream<Map<String, String>>(it)
        }
        decryptedNotes.get().asFile.writeText(
            decrypt(notes[part]!!, key)
        )
    }
}

abstract class GenerateMain : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        outputDir.get().file("Main.kt").asFile.writeText(
            $$"""
            import java.io.File
            import java.nio.file.Files
            import kotlin.time.measureTimedValue

            fun main(args: Array<String>) {
                val parts = args.map(::File).chunked(2)
                fun runPart(part: Int, solve: (String) -> Any?) {
                    val (input, output) = parts[part - 1]
                    if (!input.exists()) {
                        Files.deleteIfExists(output.toPath())
                        return
                    }
                    val notes = input.readText()
                    val (answer, duration) = measureTimedValue { solve(notes) }
                    println("$part: $answer\t($duration)")
                    output.writeText("$answer")
                }
                runPart(1) { part1(it) }
                runPart(2) { part2(it) }
                runPart(3) { part3(it) }
            }
            """.trimIndent()
        )
    }
}

abstract class Submit : DefaultTask() {

    @get:ServiceReference("api")
    abstract val api: Property<ApiService>

    @get:Internal
    val event = project.parent!!.name

    @get:Internal
    val quest = project.name

    @get:InputFile
    abstract val statusFile: RegularFileProperty

    @get:InputFiles
    abstract val answerFiles: ListProperty<RegularFile>

    @TaskAction
    fun submit() {
        val status = json.decodeFromString<Status>(statusFile.get().asFile.readText())
        val part = when {
            status.answer1 == null -> 1
            status.answer2 == null -> 2
            status.answer3 == null -> 3
            else -> throw StopExecutionException("nothing to submit")
        }
        if (status.key(part.toString()) == null) throw StopExecutionException("part $part is locked")
        val answerFile = answerFiles.get()[part - 1].asFile
        if (!answerFile.exists())
            throw StopExecutionException("no answer found for part $part")
        val answer = answerFile.readText()
        print("part $part: submitting $answer... ")
        val verdict = api.get().submit(event, quest, part.toString(), answer)
        println(verdict.formatResult())
    }
}