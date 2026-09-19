package com.atria.chat.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItem
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtriaHome(vm: ChatViewModel) {
    val settings by vm.settings.collectAsState()
    val convos by vm.convos.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val query by vm.search.collectAsState()
    val generating by vm.generating.collectAsState()
    val showSettings by vm.showSettings.collectAsState()
    val confirmClear by vm.confirmClear.collectAsState()
    val renameId by vm.renameId.collectAsState()
    val confirmDeleteId by vm.confirmDeleteId.collectAsState()

    val p = atriaPalette(settings.darkTheme)
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snacks = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val ctx = LocalContext.current
    var showModel by remember { mutableStateOf(false) }

    val convo = remember(convos, activeId) { convos.firstOrNull { it.id == activeId } }
    val messages = convo?.messages.orEmpty()

    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(enabled = showSettings) { vm.closeSettings() }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is UiEvent.Toast -> snacks.showSnackbar(e.msg)
                is UiEvent.Share -> vm.shareIntent(ctx, e.title, e.markdown)
                UiEvent.OpenSettings -> vm.openSettings()
            }
        }
    }

    AtriaTheme(darkTheme = settings.darkTheme) {
        // Full-screen settings replaces the old dialog
        if (showSettings) {
            Box(modifier = Modifier.fillMaxSize().background(p.bg)) {
                SettingsScreen(
                    apiKey = settings.apiKey, model = settings.model, system = settings.system,
                    dark = settings.darkTheme, p = p,
                    onBack = vm::closeSettings,
                    onSave = { k, m, s, d -> vm.saveSettings(k, m, s, d) }
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawer,
                drawerContent = {
                    Box(modifier = Modifier.width(300.dp).fillMaxSize().background(p.surface1)) {
                        HistoryDrawer(
                            convos = convos,
                            activeId = activeId,
                            query = query,
                            p = p,
                            hasKey = settings.apiKey.isNotBlank(),
                            onQuery = vm::setSearch,
                            onNew = { vm.newChat(); scope.launch { drawer.close() } },
                            onOpen = { vm.select(it); scope.launch { drawer.close() } },
                            onRename = vm::askRename,
                            onDelete = vm::askDelete,
                            onShare = vm::shareConvo,
                            onSettings = vm::openSettings,
                            onToggleTheme = vm::toggleTheme,
                            dark = settings.darkTheme
                        )
                    }
                }
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snacks) },
                    containerColor = p.bg,
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawer.open() } }) {
                                    Icon(Icons.Filled.Menu, "Menu", tint = p.dim)
                                }
                            },
                            title = {
                                Text(
                                    convo?.title ?: "Atria Chat",
                                    color = p.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                                    fontFamily = DisplayFamily, maxLines = 1
                                )
                            },
                            actions = {
                                // Model pill now opens the ChatGPT-style model picker
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(p.surface2)
                                        .border(1.dp, p.border, RoundedCornerShape(99.dp))
                                        .clickable { showModel = true }
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(p.accent)
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(settings.model, color = p.dim, fontSize = 11.5.sp, fontFamily = BodyFamily, maxLines = 1)
                                    }
                                }
                                IconButton(onClick = vm::exportCurrent) {
                                    Icon(Icons.Filled.Download, "Export", tint = p.dim, modifier = Modifier.size(19.dp))
                                }
                                IconButton(onClick = vm::clearCurrent) {
                                    Icon(Icons.Filled.Delete, "Clear", tint = p.dim, modifier = Modifier.size(19.dp))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = p.bg)
                        )
                    },
                    bottomBar = {
                        ComposerBar(
                            p = p,
                            generating = generating,
                            onSend = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.send(it)
                            },
                            onStop = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.stop()
                            },
                            onCommand = vm::execCommand,
                            onToast = vm::toast
                        )
                    }
                ) { pad ->
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size, generating, messages.lastOrNull()?.content?.length) {
                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                    }
                    val showJump by remember {
                        derivedStateOf {
                            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            messages.isNotEmpty() && last < messages.size - 1
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().background(p.bg).padding(pad)) {
                        if (messages.isEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                item {
                                    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                    Welcome(greetingFor(h), p, onSuggest = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        vm.send(it)
                                    })
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                itemsIndexed(
                                    messages,
                                    key = { k, m -> "$k-${m.role}-${m.content.hashCode()}-${m.error}" }
                                ) { idx, m ->
                                    Box(Modifier.animateItem()) {
                                        if (m.role == "user") {
                                            UserBubble(
                                                msg = m, p = p,
                                                onCopy = {
                                                    clipboard.setText(AnnotatedString(m.content))
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onEditSave = { vm.saveEdit(idx, it) }
                                            )
                                        } else {
                                            val isLast = idx == messages.size - 1
                                            val streaming = generating && isLast
                                            AiMessage(
                                                msg = m, isLast = isLast, generating = generating,
                                                streaming = streaming, p = p,
                                                onCopy = {
                                                    clipboard.setText(AnnotatedString(m.content))
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onRegen = { vm.regenerate(idx) },
                                                onRetry = vm::retry,
                                                onShare = { vm.shareMessage(m.content) },
                                                onOpenSettings = vm::openSettings
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Scroll-to-latest FAB, ChatGPT-style placement
                        AnimatedVisibility(
                            visible = showJump && messages.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                        ) {
                            IconButton(
                                onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                                modifier = Modifier
                                    .shadow(8.dp, CircleShape)
                                    .background(p.surface2, CircleShape)
                                    .border(1.dp, p.border, CircleShape)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Filled.ArrowDownward, "Scroll to latest", tint = p.dim, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showModel) {
            ModelSheet(
                current = settings.model, p = p,
                onDismiss = { showModel = false },
                onPick = { vm.setModel(it); showModel = false }
            )
        }
        if (confirmClear) {
            ConfirmDialog(
                "Clear this chat?", "All messages in \"${convo?.title ?: ""}\" will be removed. This cannot be undone.",
                "Clear chat", p, onNo = vm::cancelClear, onYes = vm::confirmClearYes
            )
        }
        confirmDeleteId?.let { id ->
            val c = convos.firstOrNull { it.id == id }
            ConfirmDialog(
                "Delete this chat?", "\"${c?.title ?: ""}\" will be permanently removed.",
                "Delete", p, onNo = vm::cancelDelete, onYes = vm::confirmDelete
            )
        }
        renameId?.let { id ->
            val c = convos.firstOrNull { it.id == id }
            if (c != null) RenameDialog(c.title, p, onNo = vm::cancelRename, onYes = vm::commitRename)
        }
    }
}

@Composable
private fun ComposerBar(
    p: AtriaPalette,
    generating: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onCommand: (String) -> Unit,
    onToast: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var palSel by remember { mutableStateOf(0) }
    val showPalette = text.startsWith("/") && !generating
    val matches = remember(text) {
        if (!showPalette) emptyList()
        else {
            val q = text.drop(1).split(Regex("\\s")).first().lowercase()
            COMMANDS.filter { it.cmd.drop(1).startsWith(q) }
        }
    }

    // System voice input — no permission needed (delegates to recognizer UI)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heard = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!heard.isNullOrEmpty()) text = (text.trim() + " " + heard).trim()
        }
    }
    fun startVoice() {
        try {
            voiceLauncher.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message to Atria")
                }
            )
        } catch (_e: Exception) {
            onToast("Voice input not available on this device")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.bg)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 4.dp)
    ) {
        if (showPalette && matches.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.surface1)
                    .border(1.dp, p.borderStrong, RoundedCornerShape(16.dp))
                    .padding(6.dp)
            ) {
                matches.forEachIndexed { k, c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (k == (palSel % matches.size)) p.surface2 else p.surface1)
                            .clickable { onCommand(c.cmd); text = ""; palSel = 0 }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(p.accentSoft)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(c.cmd, color = p.accentStrong, fontFamily = MonoFamily, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(c.desc, color = p.dim, fontSize = 13.sp, fontFamily = BodyFamily)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ChatGPT-style pill composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(p.surface2)
                .border(1.dp, p.border, RoundedCornerShape(28.dp))
                .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it; palSel = 0 },
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                textStyle = TextStyle(color = p.text, fontSize = 15.5.sp, lineHeight = 22.sp, fontFamily = BodyFamily),
                cursorBrush = SolidColor(p.accent),
                maxLines = 6,
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Message Atria…", color = p.faint, fontSize = 15.5.sp, fontFamily = BodyFamily)
                    inner()
                }
            )
            if (!generating) {
                IconButton(onClick = ::startVoice, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Mic, "Voice input", tint = p.faint, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(2.dp))
            val enabled = generating || text.isNotBlank()
            val bg = if (!enabled) p.surface3 else p.accent
            val fg = if (!enabled) p.faint else p.onAccent
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .then(if (enabled) Modifier.clickable {
                        if (generating) onStop()
                        else {
                            val v = text.trim()
                            if (v.isNotEmpty()) {
                                if (showPalette && matches.isNotEmpty()) {
                                    onCommand(matches[palSel % matches.size].cmd)
                                } else onSend(v)
                                text = ""
                            }
                        }
                    } else Modifier)
                    .border(if (generating) 1.dp else 0.dp, p.borderStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (generating) Icons.Filled.Stop else Icons.Filled.ArrowUpward,
                    if (generating) "Stop" else "Send",
                    tint = fg, modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "Atria can make mistakes — verify important information.",
            color = p.faint, fontSize = 11.5.sp, fontFamily = BodyFamily,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
