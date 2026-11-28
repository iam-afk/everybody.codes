package everybody.codes.plugin

import com.sun.net.httpserver.HttpServer
import org.gradle.api.GradleException
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DownloadJsonTest {
    @field:TempDir
    lateinit var directory: Path

    private val previous =
        """{"key1":"previous key","answer1":"previous answer","key2":"b","answer2":"c","key3":"d","answer3":"e"}"""

    @Test
    fun `503 preserves output and reports the status without the response body`() {
        val marker = "private-response-detail"
        withResponse(503, """{"error":"$marker"}""") { client, uri, output ->
            val error = assertFailsWith<GradleException> { client.downloadJson<Map<String, String>>(uri, output) }
            val message = error.message.orEmpty()
            assertContains(message, "HTTP 503")
            assertFalse(marker in message)
            assertEquals(previous, Files.readString(output))
        }
    }

    @Test
    fun `malformed JSON on 200 preserves output`() {
        withResponse(200, "{\"key1\":") { client, uri, output ->
            assertFailsWith<GradleException> { client.downloadJson<Map<String, String>>(uri, output) }
            assertEquals(previous, Files.readString(output))
        }
    }

    @Test
    fun `incompatible Status on 200 preserves output`() {
        withResponse(200, """{"key1":["not a string"]}""") { client, uri, output ->
            assertFailsWith<GradleException> { client.downloadJson<Status>(uri, output) }
            assertEquals(previous, Files.readString(output))
        }
    }

    @Test
    fun `shorter valid JSON on 200 completely replaces output`() {
        val replacement = """{"key1":"new"}"""
        withResponse(200, replacement) { client, uri, output ->
            client.downloadJson<Map<String, String>>(uri, output)
            assertEquals(replacement, Files.readString(output))
        }
    }

    private fun withResponse(status: Int, body: String, check: (HttpClient, URI, Path) -> Unit) {
        val output = Files.writeString(directory.resolve("output.json"), previous)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/data") { exchange ->
            exchange.use {
                val bytes = body.toByteArray(Charsets.UTF_8)
                it.responseHeaders.set("Content-Type", "application/json")
                it.sendResponseHeaders(status, bytes.size.toLong())
                it.responseBody.write(bytes)
            }
        }
        server.start()
        try {
            val uri = URI("http://127.0.0.1:${server.address.port}/data")
            HttpClient.newBuilder().proxy(ProxySelector.of(null)).build().use { client ->
                check(client, uri, output)
            }
            Files.list(directory).use { assertEquals(listOf(output), it.toList()) }
        } finally {
            server.stop(0)
        }
    }
}
