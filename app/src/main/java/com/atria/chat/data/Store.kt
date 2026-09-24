package com.atria.chat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

private val Context.atriaPrefs by preferencesDataStore(name = "atria_settings")

private object Keys {
    val ApiKey = stringPreferencesKey("api_key")
    val Model = stringPreferencesKey("model")
    val System = stringPreferencesKey("system")
    val Dark = booleanPreferencesKey("dark_theme")
}

class AtriaStore(private val appContext: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val writeRequests = Channel<PersistRequest>(Channel.CONFLATED)

    private val _settings = MutableStateFlow(SettingsData())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    private val _convos = MutableStateFlow<List<Conversation>>(emptyList())
    val convos: StateFlow<List<Conversation>> = _convos.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    private fun convosFile(): File = File(appContext.filesDir, "atria_convos_v1.json")
    private fun activeFile(): File = File(appContext.filesDir, "atria_active.txt")

    private val initialization: Job = scope.launch {
        val prefs = try {
            appContext.atriaPrefs.data.first()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyPreferences()
        }
        _settings.value = SettingsData(
            apiKey = prefs[Keys.ApiKey].orEmpty(),
            model = prefs[Keys.Model].ifNullOrBlank(DEFAULT_MODEL),
            system = prefs[Keys.System].orEmpty(),
            darkTheme = prefs[Keys.Dark] ?: true
        )

        val loaded = loadConvos()
        _convos.value = loaded
        val requested = loadActiveId()
        _activeId.value = requested?.takeIf { id -> loaded.any { it.id == id } }
            ?: loaded.maxByOrNull { it.updated }?.id
        _initialized.value = true
    }

    private val writer: Job = scope.launch {
        initialization.join()
        for (request in writeRequests) {
            try {
                replaceAtomically(
                    convosFile(),
                    json.encodeToString(ListSerializer(Conversation.serializer()), request.conversations)
                )
                val activeTarget = activeFile()
                if (request.activeId != null) replaceAtomically(activeTarget, request.activeId)
                else activeTarget.delete()
            } catch (_: Exception) {
                // In-memory state remains available if a disk write is temporarily unavailable.
            }
        }
    }

    private fun String?.ifNullOrBlank(fallback: String): String =
        if (this.isNullOrBlank()) fallback else this

    private fun loadConvos(): List<Conversation> {
        val file = convosFile()
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readText()
            if (raw.isBlank()) emptyList()
            else json.decodeFromString(ListSerializer(Conversation.serializer()), raw)
        } catch (_: Exception) {
            // Keep damaged data for manual recovery instead of silently overwriting it.
            runCatching {
                file.copyTo(File(file.parentFile, "${file.name}.corrupt-${System.currentTimeMillis()}"), overwrite = false)
            }
            emptyList()
        }
    }

    private fun loadActiveId(): String? = try {
        val file = activeFile()
        if (file.exists()) file.readText().trim().takeIf { it.isNotEmpty() } else null
    } catch (_: Exception) {
        null
    }

    fun persistConvos(list: List<Conversation>, active: String?) {
        _convos.value = list
        _activeId.value = active
        writeRequests.trySend(PersistRequest(list, active))
    }

    suspend fun saveSettings(s: SettingsData) {
        initialization.join()
        _settings.value = s
        appContext.atriaPrefs.edit { p ->
            p[Keys.ApiKey] = s.apiKey
            p[Keys.Model] = s.model.ifBlank { DEFAULT_MODEL }
            p[Keys.System] = s.system
            p[Keys.Dark] = s.darkTheme
        }
    }

    private fun replaceAtomically(target: File, contents: String) {
        val temp = File(target.parentFile, ".${target.name}.tmp")
        try {
            temp.writeText(contents)
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                // A few Android filesystem providers do not implement REPLACE_EXISTING.
                if (temp.exists()) {
                    if (target.exists()) target.delete()
                    if (!temp.renameTo(target)) temp.copyTo(target, overwrite = true)
                }
            }
        } finally {
            temp.delete()
        }
    }

    fun newId(): String = System.currentTimeMillis().toString(36) + UUID.randomUUID().toString().take(6)

    private data class PersistRequest(
        val conversations: List<Conversation>,
        val activeId: String?
    )
}

// Observe settings as StateFlow-friendly helper
fun AtriaStore.settingsFlow() = settings
fun AtriaStore.convosFlow() = convos
