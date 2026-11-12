package com.laul.lauwell.core.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laul.lauwell.core.data.RecordEntity
import com.laul.lauwell.core.data.RecordLinkEntity
import com.laul.lauwell.core.data.RecordRows
import com.laul.lauwell.core.data.RecordValueEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

// HttpSyncApi against a minimal local HTTP responder, answering like backend/api. Robolectric for org.json.
@RunWith(AndroidJUnit4::class)
class HttpSyncApiTest {

    private val server = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
    private val requests = mutableListOf<Triple<String, String?, JSONObject>>()

    /** Answers each call with the next (status, body); records path, Authorization and body. One call per connection. */
    private fun answer(vararg replies: Pair<Int, String>) {
        thread(isDaemon = true) {
            for ((status, reply) in replies) {
                val socket = runCatching { server.accept() }.getOrNull() ?: return@thread
                socket.use {
                    val input = it.getInputStream().bufferedReader()
                    val path = input.readLine().split(" ")[1]
                    val headers = generateSequence { input.readLine() }.takeWhile { line -> line.isNotEmpty() }
                        .associate { line -> line.substringBefore(":").lowercase() to line.substringAfter(":").trim() }
                    val body = CharArray(headers.getValue("content-length").toInt()).also { chars -> input.read(chars) }
                    requests += Triple(path, headers["authorization"], JSONObject(String(body)))
                    val bytes = reply.toByteArray()
                    it.getOutputStream().write(
                        ("HTTP/1.1 $status X\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n").toByteArray() + bytes,
                    )
                }
            }
        }
    }

    @After
    fun stop() = server.close()

    private var refreshed = false
    private val api by lazy {
        HttpSyncApi("http://127.0.0.1:${server.localPort}/api") { force ->
            if (force) refreshed = true
            if (force) "fresh-token" else "cached-token"
        }
    }

    private val entry = RecordRows(
        record = RecordEntity(
            id = "0b9d6a3e-5d64-4a7e-9a39-1c1f6d3c2b11", metricId = "mood", shape = "Measurement",
            timeUtc = 1_793_000_000_123, zoneOffsetS = -18000, source = "manual", recordedAt = 1_793_000_060_000,
            note = "after the walk", schemaVersion = 1,
        ),
        values = listOf(RecordValueEntity("0b9d6a3e-5d64-4a7e-9a39-1c1f6d3c2b11", "value", 7.5)),
        links = listOf(RecordLinkEntity("0b9d6a3e-5d64-4a7e-9a39-1c1f6d3c2b11", "6f1e2d3c-4b5a-4968-8776-655443322110", "recorded_during")),
    )

    @Test
    fun `push sends the records with the token and reads what was taken or refused`() = runBlocking {
        answer(200 to """{"accepted":["${entry.record.id}"],"rejected":[{"id":null,"reason":"invalid"}]}""")

        val result = api.push(listOf(entry))

        assertEquals(PushResult(listOf(entry.record.id), listOf(Refusal(null, "invalid"))), result)
        val (path, auth, body) = requests.single()
        assertEquals("/api/push", path)
        assertEquals("Bearer cached-token", auth)
        assertEquals(entry, recordRowsFromWire(body.getJSONArray("changes").getJSONObject(0)))
        assertEquals(JSONObject.NULL, body.getJSONArray("changes").getJSONObject(0).get("edited_at"))
    }

    @Test
    fun `pull reads the records back as rows`() = runBlocking {
        val wire = entry.toWire().put("change_seq", 42)
        answer(200 to """{"records":[$wire],"next":42,"more":true}""")

        assertEquals(PullPage(listOf(entry), next = 42, more = true), api.pull(after = 7, limit = 500))
        assertEquals(7L, requests.single().third.getLong("after"))
    }

    @Test
    fun `an expired token is refreshed once`() = runBlocking {
        answer(401 to """{"error":"sign in again"}""", 200 to """{"records":[],"next":0,"more":false}""")

        api.pull(after = 0, limit = 500)

        assertEquals(listOf("Bearer cached-token", "Bearer fresh-token"), requests.map { it.second })
        assertEquals(true, refreshed)
    }

    @Test
    fun `an error status is reported with its code`() {
        answer(503 to """{"error":"server error"}""")
        val error = assertThrows(SyncHttpException::class.java) { runBlocking { api.pull(after = 0, limit = 500) } }
        assertEquals(503, error.status)
    }

    @Test
    fun `nothing is sent when signed out`() {
        answer()
        val signedOut = HttpSyncApi("http://127.0.0.1:${server.localPort}/api") { null }
        assertThrows(NotSignedInException::class.java) { runBlocking { signedOut.pull(after = 0, limit = 500) } }
        assertEquals(0, requests.size)
    }
}
