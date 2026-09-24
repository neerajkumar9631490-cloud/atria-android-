package com.atria.chat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AtriaException(message: String) : Exception(message)

@Serializable
private data class WireMsg(val role: String, val content: String)

@Serializable
private data class WireReq(val model: String, val messages: List<WireMsg>, val stream: Boolean = true)

private val wireJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class AtriaApi {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Volatile
    private var currentCall: okhttp3.Call? = null

    fun cancel() {
        try { currentCall?.cancel() } catch (_e: Exception) { }
    }

    /**
     * Streams a chat completion. Calls [onDelta] on each text piece.
     * Returns the full accumulated text. Throws [AtriaException] on API errors
     * and [java.io.IOException] (as CancellationException-safe) on network errors.
     * Must be called from a coroutine; cancellation cancels the HTTP call.
     */
    suspend fun streamChat(
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AtriaException(
            "No API key configured. Open Settings from the menu or drawer and paste your Atria API key."
        )
        val clean = history
            .filter { !it.local && (it.role == "user" || it.role == "assistant" || it.role == "system") && it.content.isNotBlank() }
            .takeLast(60)
            .map { WireMsg(it.role, it.content) }
        if (clean.isEmpty()) throw AtriaException("No valid messages in request.")

        val bodyJson = wireJson.encodeToString(WireReq.serializer(), WireReq(model.ifBlank { DEFAULT_MODEL }, clean, true))
        val req = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .addHeader("User-Agent", "Atria-Android/1.2.0")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(req)
        currentCall = call
        // Coroutine cancellation must also interrupt a blocking SSE read.
        kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.invokeOnCompletion { cause ->
            if (cause is kotlinx.coroutines.CancellationException) call.cancel()
        }
        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val raw = try { resp.body?.string().orEmpty() } catch (_e: Exception) { "" }
                    throw AtriaException(describeHttpError(resp.code, raw))
                }
                val source = resp.body?.source() ?: throw AtriaException("Empty response from the model.")
                val out = StringBuilder()
                while (true) {
                    // Cooperative cancellation
                    if (!kotlin.coroutines.coroutineContext.isActive) {
                        try { call.cancel() } catch (_e: Exception) { }
                        throw kotlinx.coroutines.CancellationException("Stopped")
                    }
                    val line: String? = try {
                        // readUtf8Line with deadline — returns null on EOF
                        source.readUtf8Line()
                    } catch (e: java.io.IOException) {
                        if (call.isCanceled()) throw kotlinx.coroutines.CancellationException("Stopped")
                        throw e
                    }
                    if (line == null) break
                    var l = line.trim()
                    if (l.isEmpty()) continue
                    if (l.startsWith(":")) continue // SSE comment / keep-alive
                    if (l.startsWith("data:")) l = l.removePrefix("data:").trim()
                    if (l == "[DONE]") break
                    val chunk: SseChunk = try {
                        wireJson.decodeFromString(SseChunk.serializer(), l)
                    } catch (_e: Exception) {
                        continue
                    }
                    if (chunk.error != null) {
                        val msg = when (val e = chunk.error) {
                            is kotlinx.serialization.json.JsonPrimitive -> e.content.trim('"')
                            else -> e.toString()
                        }
                        throw AtriaException(msg.ifBlank { "Upstream error" })
                    }
                    val piece = chunk.choices?.firstOrNull()?.delta?.content
                    if (!piece.isNullOrEmpty()) {
                        out.append(piece)
                        withContext(Dispatchers.Main) { onDelta(piece) }
                    }
                }
                val full = out.toString()
                if (full.isBlank()) throw AtriaException("Empty response from the model.")
                full
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: AtriaException) {
            throw e
        } catch (e: java.io.IOException) {
            if (call.isCanceled()) throw kotlinx.coroutines.CancellationException("Stopped")
            throw AtriaException("Could not reach the Atria API: ${e.message ?: e}")
        } finally {
            if (currentCall === call) currentCall = null
        }
    }

    private fun describeHttpError(code: Int, body: String): String {
        if (body.isNotBlank()) {
            try {
                val el = wireJson.parseToJsonElement(body)
                val obj = el as? kotlinx.serialization.json.JsonObject
                val err = obj?.get("error")
                if (err != null) {
                    val msg = when (err) {
                        is kotlinx.serialization.json.JsonPrimitive ->
                            if (err.isString) err.content else err.toString()
                        is kotlinx.serialization.json.JsonObject ->
                            (err["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                ?: err.toString()
                        else -> err.toString()
                    }
                    if (msg.isNotBlank()) return "API error $code: ${msg.take(500)}"
                } else if (body.length < 500) {
                    return "API error $code: ${body.take(500)}"
                }
            } catch (_e: Exception) {
                if (body.length < 500) return "API error $code: ${body.take(500)}"
            }
        }
        return "API error $code. Check your API key and model, then retry."
    }

    @Serializable
    private data class SseChunk(
        val choices: List<SseChoice>? = null,
        val error: kotlinx.serialization.json.JsonElement? = null
    )

    @Serializable
    private data class SseChoice(val delta: SseDelta? = null)

    @Serializable
    private data class SseDelta(val content: String? = null)
}
