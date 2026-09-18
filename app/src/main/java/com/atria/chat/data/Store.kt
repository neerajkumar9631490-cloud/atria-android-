package com.atria.chat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
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

    private val _settings = MutableStateFlow(SettingsData())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    private val _convos = MutableStateFlow<List<Conversation>>(emptyList())
    val convos: StateFlow<List<Conversation>> = _convos.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private fun convosFile(): File = File(appContext.filesDir, "atria_convos_v1.json")
    private fun activeFile(): File = File(appContext.filesDir, "atria_active.txt")

    init {
        scope.launch {
            // Load settings
            val prefs = appContext.atriaPrefs.data.first()
            _settings.value = SettingsData(
                apiKey = prefs[Keys.ApiKey].orEmpty(),
                model = prefs[Keys.Model].ifNullOrBlank(DEFAULT_MODEL),
                system = prefs[Keys.System].orEmpty(),
                darkTheme = prefs[Keys.Dark] ?: true
            )
            // Load conversations
            _convos.value = loadConvos()
            _activeId.value = loadActiveId() ?: _convos.value.maxByOrNull { it.updated }?.id
        }
    }

    private fun String?.ifNullOrBlank(fallback: String): String =
        if (this.isNullOrBlank()) fallback else this

    private suspend fun loadConvos(): List<Conversation> = withContext(Dispatchers.IO) {
        try {
            val f = convosFile()
            if (!f.exists()) return@withContext emptyList()
            val raw = f.readText()
            if (raw.isBlank()) return@withContext emptyList()
            json.decodeFromString(ListSerializer(Conversation.serializer()), raw)
        } catch (_e: Exception) {
            emptyList()
        }
    }

    private suspend fun loadActiveId(): String? = withContext(Dispatchers.IO) {
        try {
            val f = activeFile()
            if (!f.exists()) null else f.readText().trim().takeIf { it.isNotEmpty() }
        } catch (_e: Exception) { null }
    }

    fun persistConvos(list: List<Conversation>, active: String?) {
        _convos.value = list
        _activeId.value = active
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    convosFile().writeText(json.encodeToString(ListSerializer(Conversation.serializer()), list))
                    if (active != null) activeFile().writeText(active) else activeFile().delete()
                } catch (_e: Exception) { }
            }
        }
    }

    suspend fun saveSettings(s: SettingsData) {
        _settings.value = s
        appContext.atriaPrefs.edit { p ->
            p[Keys.ApiKey] = s.apiKey
            p[Keys.Model] = s.model.ifBlank { DEFAULT_MODEL }
            p[Keys.System] = s.system
            p[Keys.Dark] = s.darkTheme
        }
    }

    fun newId(): String = System.currentTimeMillis().toString(36) + UUID.randomUUID().toString().take(6)
}

// Observe settings as StateFlow-friendly helper
fun AtriaStore.settingsFlow() = settings
fun AtriaStore.convosFlow() = convos
