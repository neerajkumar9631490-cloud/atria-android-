package com.atria.chat.data

import kotlinx.serialization.Serializable

const val DEFAULT_MODEL = "Atria-Dawn-Preview"
const val API_URL = "https://api.atria-asi.ai/v1/chat/completions"

@Serializable
data class ChatMessage(
    val role: String, // "user" | "assistant" | "system"(transient only)
    val content: String = "",
    val local: Boolean = false,
    val error: String? = null,
    val stopped: Boolean = false,
    val metaModel: String? = null,
    val metaMs: Long? = null
)

@Serializable
data class Conversation(
    val id: String,
    val title: String = "New chat",
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

data class SettingsData(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val system: String = "",
    val darkTheme: Boolean = true
)

fun makeTitle(text: String): String {
    val t = text.replace("\\s+".toRegex(), " ").trim()
    return if (t.length > 42) t.take(42).trimEnd() + "…" else t.ifEmpty { "New chat" }
}

fun bucketOf(ts: Long, now: Long = System.currentTimeMillis()): String {
    fun sod(t: Long): Long {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = t
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
    val diffDays = (sod(now) - sod(ts)) / 86_400_000.0
    return when {
        diffDays <= 0 -> "Today"
        diffDays < 1.5 -> "Yesterday"
        diffDays < 7 -> "Previous 7 days"
        diffDays < 30 -> "Previous 30 days"
        else -> "Older"
    }
}

fun buildHelpMarkdown(currentModel: String): String = listOf(
    "### Professional AI Chat Commands",
    "",
    "| Command | What it does |",
    "| --- | --- |",
    "| `/new` | Start a new chat |",
    "| `/clear` | Clear the messages in this chat |",
    "| `/model <id>` | Switch model — current: `$currentModel` |",
    "| `/export` | Share this chat as Markdown |",
    "| `/theme` | Toggle light / dark mode |",
    "| `/stop` | Stop the current response |",
    "| `/help` | Show this guide |",
    "",
    "#### Keyboard & gestures",
    "- **Enter** — send · long-press a message to copy",
    "- Type **/** at the start of a message to open the command menu",
    "",
    "#### Professional features",
    "- **Syntax-highlighted code blocks** — with copy buttons",
    "- **Structured tables** — professional formatting",
    "- **Streaming** — live token rendering with stop",
    "",
    "Chats are saved automatically on this device."
).joinToString("\n")

fun exportMarkdown(title: String, model: String, messages: List<ChatMessage>): String {
    val sb = StringBuilder()
    sb.appendLine("# $title")
    sb.appendLine()
    sb.appendLine("> Exported from Atria · ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
    sb.appendLine()
    messages.forEach { m ->
        if (m.local) return@forEach
        if (m.role == "user") {
            sb.appendLine("**You:**")
            sb.appendLine()
            sb.appendLine(m.content)
            sb.appendLine()
        } else if (m.role == "assistant") {
            val label = if (!m.metaModel.isNullOrBlank()) " (${m.metaModel})" else " ($model)"
            sb.appendLine("**Atria**$label:")
            sb.appendLine()
            sb.appendLine(m.content.ifBlank { if (!m.error.isNullOrBlank()) "_Error: ${m.error}_" else "" })
            sb.appendLine()
        }
    }
    return sb.toString()
}
