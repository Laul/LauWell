package com.laul.lauwell.core.sync

import com.laul.lauwell.core.data.RecordRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The sync API (backend/api): send changed records, fetch the records changed since a change number. */
interface SyncApi {
    suspend fun push(changes: List<RecordRows>): PushResult

    suspend fun pull(after: Long, limit: Int): PullPage
}

/** Per record: taken (or an older edit, dropped), or refused with a [Refusal.reason]. */
data class PushResult(val accepted: List<String>, val refused: List<Refusal>)

/**
 * [reason] is `invalid` or `not_yours` (never passes as it is) or [MissingLink] (passes once the linked
 * record reaches the server). [id] is null when the server couldn't read one.
 */
data class Refusal(val id: String?, val reason: String)

const val MissingLink = "missing_link"

/** Records changed after the asked change number, oldest first; [next] is the one to ask from next. */
data class PullPage(val records: List<RecordRows>, val next: Long, val more: Boolean)

/** The server answered [status]: 401 still signed out after a fresh token, 4xx a bug, 5xx worth a retry. */
class SyncHttpException(val status: Int) : IOException("sync API answered $status")

/** No one is signed in: nothing to send or fetch. */
class NotSignedInException : IOException("not signed in")

/**
 * [SyncApi] over HTTPS with `HttpURLConnection` and `org.json`, both in Android. [idToken] gives the
 * signed-in user's Firebase ID token (fresh when asked), null when signed out. The token never reaches
 * a log or an exception message.
 */
class HttpSyncApi(
    private val baseUrl: String,
    private val idToken: suspend (forceRefresh: Boolean) -> String?,
) : SyncApi {

    override suspend fun push(changes: List<RecordRows>): PushResult {
        val body = JSONObject().put("changes", JSONArray(changes.map { it.toWire() }))
        val answer = post("/push", body)
        val refused = answer.getJSONArray("rejected")
        return PushResult(
            accepted = answer.getJSONArray("accepted").strings(),
            refused = (0 until refused.length()).map {
                val r = refused.getJSONObject(it)
                Refusal(if (r.isNull("id")) null else r.getString("id"), r.getString("reason"))
            },
        )
    }

    override suspend fun pull(after: Long, limit: Int): PullPage {
        val answer = post("/pull", JSONObject().put("after", after).put("limit", limit))
        val records = answer.getJSONArray("records")
        return PullPage(
            records = (0 until records.length()).map { recordRowsFromWire(records.getJSONObject(it)) },
            next = answer.getLong("next"),
            more = answer.getBoolean("more"),
        )
    }

    /** POSTs [body]; on 401 once more with a refreshed token (the cached one may have just expired). */
    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val text = body.toString()
        var (status, answer) = send(path, text, idToken(false) ?: throw NotSignedInException())
        if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            val fresh = idToken(true) ?: throw NotSignedInException()
            send(path, text, fresh).let { status = it.first; answer = it.second }
        }
        if (status !in 200..299) throw SyncHttpException(status)
        JSONObject(answer)
    }

    private fun send(path: String, body: String, token: String): Pair<Int, String> {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TimeoutMs
            connection.readTimeout = TimeoutMs
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TimeoutMs = 30_000
    }
}

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
