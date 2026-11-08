package everybody.codes.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.IOException
import java.net.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

interface Parameters : BuildServiceParameters {
    val cookie: Property<String>
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

val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

abstract class ApiService : BuildService<Parameters>, AutoCloseable {
    private val client: HttpClient
    private val user: User

    init {
        val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_NONE).apply {
            val cookie = HttpCookie("everybody-codes", parameters.cookie.get()).apply {
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
        user = json.decodeFromString<User>(response.body())
    }

    fun fetchNotes(event: String, quest: String, output: Path) {
        val response = client.send(
            HttpRequest.newBuilder(URI("https://everybody.codes/assets/$event/$quest/input/${user.seed}.json")).build(),
            HttpResponse.BodyHandlers.ofFile(output)
        )
        if (response.statusCode() !in 200..299)
            throw RuntimeException("failed to downloads notes")
    }

    fun state(event: String, quest: String, output: Path) {
        val response = client.send(
            HttpRequest.newBuilder(URI("https://api.everybody.codes/event/$event/quest/$quest")).build(),
            HttpResponse.BodyHandlers.ofFile(
                output, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
            )
        )
        if (response.statusCode() !in 200..299)
            throw RuntimeException("failed to fetch keys")
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

    @get:OutputFile
    abstract val notesFile: RegularFileProperty

    @TaskAction
    fun download() {
        api.get().fetchNotes(event.get(), quest.get(), notesFile.get().asFile.toPath())
    }

}

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

class EverybodyCodesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(KotlinPluginWrapper::class.java)
        project.tasks.withType(KotlinJvmCompile::class.java).configureEach {
            it.jvmTargetValidationMode.set(JvmTargetValidationMode.IGNORE)
        }

        project.gradle.sharedServices.registerIfAbsent("api", ApiService::class.java) { spec ->
            spec.parameters.cookie.set(project.readCookie())
        }

        val downloadNotes = project.tasks.register("downloadNotes", DownloadNotes::class.java) { task ->
            task.event.set(project.parent!!.name)
            task.quest.set(project.name)
            task.notesFile.set(project.layout.buildDirectory.file("everybody_codes/notes.json"))
        }

        val downloadState = project.tasks.register("downloadState", DownloadState::class.java) { task ->
            task.event.set(project.parent!!.name)
            task.quest.set(project.name)
            task.stateFile.set(project.layout.buildDirectory.file("everybody_codes/state.json"))
        }

        val (notes1, notes2, notes3) = (1..3).map { part ->
            project.tasks.register("decryptNotes$part", DecryptNotes::class.java) { task ->
                task.part.set(part.toString())
                task.notesFile.set(downloadNotes.flatMap { it.notesFile })
                task.statusFile.set(downloadState.flatMap { it.stateFile })
            }
        }

        val generateMain = project.tasks.register("generateMain", GenerateMain::class.java) { task ->
            task.outputDir.set(project.layout.buildDirectory.dir("everybody_codes/gen"))
        }

        val sourceSetMain = project.extensions.getByType(KotlinSourceSetContainer::class.java)
            .sourceSets.getByName("main")
        sourceSetMain.kotlin.setSrcDirs(listOf(project.layout.projectDirectory))
        sourceSetMain.generatedKotlin.srcDir(generateMain)

        val (answer1, answer2, answer3) = (1..3).map {
            project.layout.buildDirectory.file("everybody_codes/answer$it.txt")
        }

        val run = project.tasks.register("run", JavaExec::class.java) { task ->
            task.dependsOn(notes1, notes2, notes3)
            task.classpath = project.files(
                project.extensions.getByType(SourceSetContainer::class.java).getByName("main").runtimeClasspath
            )
            task.mainClass.set("MainKt")
            task.doFirst {
                task.args(notes1.flatMap { it.decryptedNotes }.map { it.asFile.absolutePath }.get())
                task.args(answer1.get().asFile.absolutePath)
                task.args(notes2.flatMap { it.decryptedNotes }.map { it.asFile.absolutePath }.get())
                task.args(answer2.get().asFile.absolutePath)
                task.args(notes3.flatMap { it.decryptedNotes }.map { it.asFile.absolutePath }.get())
                task.args(answer3.get().asFile.absolutePath)
            }
            task.outputs.files(answer1, answer2, answer3)
        }

        project.tasks.register("submit", Submit::class.java) { task ->
            task.dependsOn(run)
            task.inputs.files(answer1, answer2, answer3)
            task.statusFile.set(downloadState.flatMap { it.stateFile })
        }
    }
}

fun Project.readCookie(): String {
    val file = project.layout.settingsDirectory.file(".everybody-codes").asFile
    val cookie = try {
        file.readText().trim()
    } catch (e: IOException) {
        throw ProjectConfigurationException("cannot read `${file.name}` in project root directory", e)
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
    val decryptedNotes = part.flatMap { project.layout.buildDirectory.file("everybody_codes/notes$it.txt") }

    @TaskAction
    fun decryptNotes() {
        val part = part.get()
        val key = status.get().key(part) ?: throw StopExecutionException()
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
            import kotlin.time.measureTimedValue
            
            fun main(args: Array<String>) {
                val (part1, part2, part3) = args.map(::File).chunked(2)
                val (input1, answer1) = part1
                if (input1.exists()) {
                    val notes = input1.readText()
                    val (answer, duration) = measureTimedValue {
                        part1(notes)
                    }
                    println("1: $answer\t($duration)")
                    answer1.writeText("$answer")
                }
                val (input2, answer2) = part2
                if (input2.exists()) {
                    val notes = input2.readText()
                    val (answer, duration) = measureTimedValue {
                        part2(notes)
                    }
                    println("2: $answer\t($duration)")
                    answer2.writeText("$answer")
                }
                val (input3, answer3) = part3
                if (input3.exists()) {
                    val notes = input3.readText()
                    val (answer, duration) = measureTimedValue {
                        part3(notes)
                    }
                    println("3: $answer\t($duration)")
                    answer3.writeText("$answer")
                }
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

    @get:Internal
    val status = statusFile.map { file ->
        val value = file.asFile.readText()
        json.decodeFromString<Status>(value)
    }

    @get:Internal
    val part = status.map {
        if (it.answer1 == null) "1"
        else if (it.answer2 == null) "2"
        else if (it.answer3 == null) "3"
        else null
    }

    @get:Internal
    val answerFile = part.flatMap { project.layout.buildDirectory.file("everybody_codes/answer$it.txt") }

    @TaskAction
    fun submit() {
        val part = part.getOrNull() ?: throw StopExecutionException("nothing to submit")
        val answerFile = answerFile.get().asFile
        if (!answerFile.exists())
            throw StopExecutionException("no answer found for part $part")
        val answer = answerFile.readText()
        print("part $part: submitting $answer... ")
        val verdict = api.get().submit(event, quest, part, answer)
        if (verdict.correct) {
            println("correct!")
            statusFile.asFile.get().delete()
        } else {
            println("WRONG ANSWER")
            println(
                "length ${if (verdict.lengthCorrect!!) "correct" else "wrong"}, first character ${if (verdict.firstCorrect!!) "correct" else "wrong"}"
            )
        }
    }
}