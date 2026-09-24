package com.atria.chat.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atria.chat.data.AtriaApi
import com.atria.chat.data.AtriaException
import com.atria.chat.data.AtriaStore
import com.atria.chat.data.ChatMessage
import com.atria.chat.data.Conversation
import com.atria.chat.data.DEFAULT_MODEL
import com.atria.chat.data.SettingsData
import com.atria.chat.data.buildHelpMarkdown
import com.atria.chat.data.exportMarkdown
import com.atria.chat.data.makeTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class Command(val cmd: String, val desc: String)

val COMMANDS = listOf(
    Command("/new", "Start a new chat"),
    Command("/clear", "Clear messages in this chat"),
    Command("/model", "Show or switch the model"),
    Command("/export", "Share this chat as Markdown"),
    Command("/theme", "Toggle light / dark mode"),
    Command("/stop", "Stop the current response"),
    Command("/help", "Show the command guide")
)

sealed interface UiEvent {
    data class Toast(val msg: String) : UiEvent
    data class Share(val title: String, val markdown: String) : UiEvent
    data object OpenSettings : UiEvent
}

class ChatViewModel(
    private val store: AtriaStore,
    private val api: AtriaApi
) : ViewModel() {

    val settings: StateFlow<SettingsData> = store.settings
    val convos: StateFlow<List<Conversation>> = store.convos
    val activeId: StateFlow<String?> = store.activeId
    val ready: StateFlow<Boolean> = store.initialized

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _composerDraft = MutableStateFlow("")
    val composerDraft: StateFlow<String> = _composerDraft.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _streamingId = MutableStateFlow<String?>(null)
    val streamingConvoId: StateFlow<String?> = _streamingId.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _confirmClear = MutableStateFlow(false)
    val confirmClear: StateFlow<Boolean> = _confirmClear.asStateFlow()

    private val _renameId = MutableStateFlow<String?>(null)
    val renameId: StateFlow<String?> = _renameId.asStateFlow()

    private val _confirmDeleteId = MutableStateFlow<String?>(null)
    val confirmDeleteId: StateFlow<String?> = _confirmDeleteId.asStateFlow()

    private var job: Job? = null

    fun active(): Conversation? {
        val id = activeId.value ?: return null
        return convos.value.firstOrNull { it.id == id }
    }

    // -- drawer / list -------------------------------------------------------
    fun setSearch(q: String) { _search.value = q }
    fun setComposerDraft(value: String) { _composerDraft.value = value.take(20_000) }

    fun newChat() {
        if (!store.initialized.value) return
        val cur = active()
        if (cur != null && cur.messages.isEmpty()) return
        stop()
        _composerDraft.value = ""
        val c = Conversation(id = store.newId(), title = "New chat")
        store.persistConvos(listOf(c) + convos.value, c.id)
    }

    fun select(id: String) {
        if (!store.initialized.value || activeId.value == id) return
        stop()
        _composerDraft.value = ""
        store.persistConvos(convos.value, id)
    }

    fun askDelete(id: String) { _confirmDeleteId.value = id }
    fun cancelDelete() { _confirmDeleteId.value = null }

    fun confirmDelete() {
        val id = _confirmDeleteId.value ?: return
        _confirmDeleteId.value = null
        if (activeId.value == id) {
            stop()
            _composerDraft.value = ""
        }
        val rest = convos.value.filterNot { it.id == id }
        val next = if (activeId.value == id) rest.maxByOrNull { it.updated }?.id else activeId.value
        store.persistConvos(rest, next)
        emit(UiEvent.Toast("Chat deleted"))
    }

    fun askRename(id: String) { _renameId.value = id; _confirmDeleteId.value = null }
    fun cancelRename() { _renameId.value = null }

    fun commitRename(title: String) {
        val id = _renameId.value ?: return
        _renameId.value = null
        val t = title.trim()
        if (t.isEmpty()) return
        updateConvo(id) { it.copy(title = t.take(60), updated = System.currentTimeMillis()) }
    }

    // -- settings ------------------------------------------------------------
    fun openSettings() { _showSettings.value = true }
    fun closeSettings() { _showSettings.value = false }

    private suspend fun persistSettings(value: SettingsData, successMessage: String? = null): Boolean {
        return try {
            store.saveSettings(value)
            successMessage?.let { emit(UiEvent.Toast(it)) }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emit(UiEvent.Toast("Could not save settings. Please try again."))
            false
        }
    }

    fun saveSettings(apiKey: String, model: String, system: String, darkTheme: Boolean? = null) {
        viewModelScope.launch {
            val cur = settings.value
            val saved = persistSettings(
                cur.copy(
                    apiKey = apiKey.trim(),
                    model = model.trim().ifEmpty { DEFAULT_MODEL },
                    system = system,
                    darkTheme = darkTheme ?: cur.darkTheme
                )
            )
            if (saved) {
                _showSettings.value = false
                emit(UiEvent.Toast("Settings saved"))
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val cur = settings.value
            persistSettings(cur.copy(darkTheme = !cur.darkTheme))
        }
    }

    fun setModel(id: String) {
        val v = id.trim()
        if (v.isEmpty()) return
        viewModelScope.launch {
            if (persistSettings(settings.value.copy(model = v))) {
                emit(UiEvent.Toast("Model set to $v"))
            }
        }
    }

    fun toast(msg: String) = emit(UiEvent.Toast(msg))

    override fun onCleared() {
        api.cancel()
        job?.cancel()
        super.onCleared()
    }

    fun shareMessage(content: String) {
        if (content.isBlank()) {
            emit(UiEvent.Toast("Nothing to share"))
            return
        }
        emit(UiEvent.Share("atria-message.md", content))
    }

    fun shareConvo(id: String) {
        val c = convos.value.firstOrNull { it.id == id } ?: return
        if (c.messages.isEmpty()) {
            emit(UiEvent.Toast("Nothing to share yet"))
            return
        }
        val md = exportMarkdown(c.title, settings.value.model, c.messages)
        emit(UiEvent.Share("atria-${c.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").take(40)}.md", md))
    }

    // -- composer ------------------------------------------------------------
    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        if (text.startsWith("/")) {
            execCommand(text)
            return
        }
        if (!store.initialized.value) {
            emit(UiEvent.Toast("Your conversations are still loading"))
            return
        }
        if (!hasApiKey()) return
        if (_generating.value) {
            emit(UiEvent.Toast("Still generating — stop the current reply first"))
            return
        }
        var c = active()
        if (c == null) {
            c = Conversation(id = store.newId(), title = makeTitle(text))
            store.persistConvos(listOf(c) + convos.value, c.id)
        } else if (c.messages.isEmpty() && c.title == "New chat") {
            updateConvo(c.id) { it.copy(title = makeTitle(text)) }
            c = active()
        }
        val target = c ?: return
        val withUser = target.messages + ChatMessage(role = "user", content = text)
        updateConvo(target.id) { it.copy(messages = withUser, updated = System.currentTimeMillis()) }
        _composerDraft.value = ""
        runCompletion(target.id)
    }

    fun stop() {
        api.cancel()
        job?.cancel()
    }

    private fun hasApiKey(): Boolean {
        if (settings.value.apiKey.isNotBlank()) return true
        _showSettings.value = true
        emit(UiEvent.Toast("Add your Atria API key to start chatting"))
        return false
    }

    fun retry() {
        if (!hasApiKey()) return
        val c = active() ?: return
        if (_generating.value) return
        // drop trailing failed assistant message, keep user messages
        val msgs = c.messages.toMutableList()
        if (msgs.isNotEmpty() && msgs.last().role == "assistant") msgs.removeAt(msgs.size - 1)
        updateConvo(c.id) { it.copy(messages = msgs) }
        runCompletion(c.id)
    }

    fun regenerate(index: Int) {
        if (!hasApiKey()) return
        val c = active() ?: return
        if (_generating.value) {
            emit(UiEvent.Toast("Still generating — stop the current reply first"))
            return
        }
        if (index < 0 || index >= c.messages.size) return
        updateConvo(c.id) { it.copy(messages = c.messages.take(index)) }
        runCompletion(c.id)
    }

    fun saveEdit(index: Int, newText: String) {
        if (!hasApiKey()) return
        val c = active() ?: return
        if (_generating.value) {
            emit(UiEvent.Toast("Stop the current reply before editing"))
            return
        }
        val v = newText.trim()
        if (v.isEmpty()) {
            emit(UiEvent.Toast("Message cannot be empty"))
            return
        }
        if (index < 0 || index >= c.messages.size) return
        val base = c.messages.take(index).toMutableList()
        base.add(ChatMessage(role = "user", content = v, edited = true))
        updateConvo(c.id) { it.copy(messages = base, updated = System.currentTimeMillis()) }
        runCompletion(c.id)
    }

    fun clearCurrent() {
        val c = active() ?: return
        if (c.messages.isEmpty()) {
            emit(UiEvent.Toast("Nothing to clear"))
            return
        }
        _confirmClear.value = true
    }

    fun confirmClearYes() {
        _confirmClear.value = false
        val c = active() ?: return
        stop()
        updateConvo(c.id) { it.copy(messages = emptyList(), updated = System.currentTimeMillis()) }
        emit(UiEvent.Toast("Chat cleared"))
    }

    fun cancelClear() { _confirmClear.value = false }

    fun exportCurrent() {
        val c = active()
        if (c == null || c.messages.isEmpty()) {
            emit(UiEvent.Toast("Nothing to export yet"))
            return
        }
        val md = exportMarkdown(c.title, settings.value.model, c.messages)
        emit(UiEvent.Share("atria-${c.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").take(40)}.md", md))
    }

    // -- commands ------------------------------------------------------------
    fun execCommand(text: String) {
        val parts = text.trim().split(Regex("\\s+"))
        val name = parts.firstOrNull().orEmpty()
        val arg = parts.drop(1).joinToString(" ")
        when (name) {
            "/new" -> newChat()
            "/clear" -> clearCurrent()
            "/help" -> {
                var h = active()
                if (h == null) {
                    h = Conversation(id = store.newId(), title = "Help")
                    store.persistConvos(listOf(h) + convos.value, h.id)
                }
                updateConvo(h.id) {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            role = "assistant",
                            content = buildHelpMarkdown(settings.value.model),
                            local = true
                        ),
                        updated = System.currentTimeMillis()
                    )
                }
            }
            "/model" -> {
                if (arg.isBlank()) {
                    emit(UiEvent.Toast("Current model: ${settings.value.model} — usage: /model <id>"))
                } else {
                    val cur = settings.value
                    viewModelScope.launch {
                        if (persistSettings(cur.copy(model = arg.trim()))) {
                            emit(UiEvent.Toast("Model set to ${arg.trim()}"))
                        }
                    }
                }
            }
            "/export" -> exportCurrent()
            "/theme" -> toggleTheme()
            "/stop" -> stop()
            else -> emit(UiEvent.Toast("Unknown command: $name — try /help"))
        }
    }

    // -- streaming core ------------------------------------------------------
    private fun runCompletion(convoId: String) {
        if (_generating.value) return
        _generating.value = true
        pending = StringBuilder()
        lastPaint = 0L
        val t0 = System.currentTimeMillis()
        val aiMsg = ChatMessage(role = "assistant", content = "")
        val base = (convos.value.firstOrNull { it.id == convoId }?.messages.orEmpty()) + aiMsg
        updateConvo(convoId) { it.copy(messages = base) }
        _streamingId.value = convoId

        job = viewModelScope.launch {
            val s = settings.value
            val history: List<ChatMessage> = buildList {
                if (s.system.isNotBlank()) add(ChatMessage(role = "system", content = s.system.trim()))
                addAll(base.filter { !it.local && (it.role == "user" || it.role == "assistant") && it.content.isNotBlank() }.takeLast(30))
            }
            val acc = StringBuilder()
            var failed: String? = null
            var stopped = false
            try {
                api.streamChat(s.apiKey, s.model, history) { delta ->
                    acc.append(delta)
                    // throttle UI writes: update at most every ~60ms
                    appendDelta(convoId, delta)
                }
            } catch (e: CancellationException) {
                stopped = true
            } catch (e: AtriaException) {
                failed = e.message ?: "Something went wrong."
            } catch (e: Exception) {
                failed = "Could not reach the Atria API: ${e.message ?: e}"
            }

            val full = acc.toString()
            val convo = convos.value.firstOrNull { it.id == convoId }
            if (convo != null) {
                val msgs = convo.messages.toMutableList()
                val idx = msgs.indexOfLast { it.role == "assistant" }
                if (stopped && full.isBlank()) {
                    if (idx >= 0) msgs.removeAt(idx)
                } else if (idx >= 0) {
                    val prev = msgs[idx]
                    msgs[idx] = prev.copy(
                        content = full,
                        error = failed,
                        stopped = stopped && failed == null,
                        metaModel = s.model,
                        metaMs = System.currentTimeMillis() - t0
                    )
                    if (full.isBlank() && failed == null && !stopped) {
                        msgs[idx] = msgs[idx].copy(error = "Empty response from the model.")
                    }
                }
                updateConvo(convoId) { it.copy(messages = msgs, updated = System.currentTimeMillis()) }
            }
            _generating.value = false
            _streamingId.value = null
        }
    }

    // Append a delta to the trailing assistant message without rebuilding everything
    // on the main thread too often — small throttle via direct list copy is fine at this scale.
    private var lastPaint = 0L
    private var pending = StringBuilder()
    private fun appendDelta(convoId: String, delta: String) {
        pending.append(delta)
        val now = System.currentTimeMillis()
        if (now - lastPaint < 60) return
        lastPaint = now
        val chunk = pending.toString()
        pending = StringBuilder()
        val convo = convos.value.firstOrNull { it.id == convoId } ?: return
        val msgs = convo.messages.toMutableList()
        val idx = msgs.indexOfLast { it.role == "assistant" }
        if (idx < 0) return
        msgs[idx] = msgs[idx].copy(content = msgs[idx].content + chunk)
        // publish synchronously (we are on Main because api calls onDelta on Main)
        updateConvoSilent(convoId, msgs)
    }

    private fun updateConvo(id: String, fn: (Conversation) -> Conversation) {
        val list = convos.value.map { if (it.id == id) fn(it) else it }
        store.persistConvos(list, activeId.value)
    }

    private fun updateConvoSilent(id: String, messages: List<ChatMessage>) {
        val list = convos.value.map {
            if (it.id == id) it.copy(messages = messages, updated = System.currentTimeMillis()) else it
        }
        store.persistConvos(list, activeId.value)
    }

    private fun emit(e: UiEvent) {
        viewModelScope.launch { _events.emit(e) }
    }

    fun shareIntent(ctx: Context, fileName: String, markdown: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, markdown)
        }
        if (ctx !is Activity) send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(Intent.createChooser(send, "Share chat"))
        } catch (_: Exception) {
            emit(UiEvent.Toast("No sharing app is available on this device"))
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val store: AtriaStore, private val api: AtriaApi) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(store, api) as T
        }
    }
}

/** Greeting matching server.py web UI. */
fun greetingFor(hour: Int): String = when {
    hour < 5 -> "Burning the midnight oil."
    hour < 12 -> "Good morning."
    hour < 18 -> "Good afternoon."
    else -> "Good evening."
}
