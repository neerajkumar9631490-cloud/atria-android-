package com.atria.chat.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
    val ctx = LocalContext.current

    val convo = remember(convos, activeId) { convos.firstOrNull { it.id == activeId } }
    val messages = convo?.messages.orEmpty()

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
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                Box(modifier = Modifier.width(300.dp).fillMaxSize().background(p.surface1)) {
                    HistoryDrawer(
                        convos = convos,
                        activeId = activeId,
                        query = query,
                        p = p,
                        onQuery = vm::setSearch,
                        onNew = { vm.newChat(); scope.launch { drawer.close() } },
                        onOpen = { vm.select(it); scope.launch { drawer.close() } },
                        onRename = vm::askRename,
                        onDelete = vm::askDelete,
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
                                color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                            )
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(p.surface2)
                                    .border(1.dp, p.border, RoundedCornerShape(99.dp))
                                    .clickable { vm.openSettings() }
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
                                    Text(settings.model, color = p.dim, fontSize = 11.5.sp, maxLines = 1)
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
                        onSend = vm::send,
                        onStop = vm::stop,
                        onCommand = vm::execCommand
                    )
                }
            ) { pad ->
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size, generating, messages.lastOrNull()?.content?.length) {
                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                }
                if (messages.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(p.bg).padding(pad).padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            Welcome(greetingFor(h), p, onSuggest = vm::send)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(p.bg).padding(pad),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        itemsIndexed(messages, key = { k, m -> "$k-${m.role}-${m.content.hashCode()}-${m.error}" }) { idx, m ->
                            if (m.role == "user") {
                                UserBubble(
                                    msg = m, p = p,
                                    onCopy = { clipboard.setText(AnnotatedString(m.content)) },
                                    onEditSave = { vm.saveEdit(idx, it) }
                                )
                            } else {
                                val isLast = idx == messages.size - 1
                                val streaming = generating && isLast
                                AiMessage(
                                    msg = m, isLast = isLast, generating = generating,
                                    streaming = streaming, p = p,
                                    onCopy = { clipboard.setText(AnnotatedString(m.content)) },
                                    onRegen = { vm.regenerate(idx) },
                                    onRetry = vm::retry,
                                    onOpenSettings = vm::openSettings
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsDialog(
                apiKey = settings.apiKey, model = settings.model, system = settings.system, p = p,
                onDismiss = vm::closeSettings,
                onSave = { k, m, s -> vm.saveSettings(k, m, s) }
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
    onCommand: (String) -> Unit
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.surface1)
                    .border(1.dp, p.borderStrong, RoundedCornerShape(14.dp))
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
                        Text(c.desc, color = p.dim, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(p.surface2)
                .border(1.dp, p.border, RoundedCornerShape(20.dp))
                .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it; palSel = 0 },
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                textStyle = TextStyle(color = p.text, fontSize = 15.sp, lineHeight = 22.sp),
                cursorBrush = SolidColor(p.accent),
                maxLines = 6,
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Message Atria…", color = p.faint, fontSize = 15.sp)
                    inner()
                }
            )
            Spacer(Modifier.width(10.dp))
            val enabled = generating || text.isNotBlank()
            val bg = if (!enabled) p.surface3 else p.accent
            val fg = if (!enabled) p.faint else p.onAccent
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                    .border(if (generating) 1.dp else 0.dp, p.borderStrong, RoundedCornerShape(14.dp)),
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
            color = p.faint, fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
