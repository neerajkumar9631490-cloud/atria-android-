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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtriaHome(vm: ChatViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val convos by vm.convos.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val streamingConvoId by vm.streamingConvoId.collectAsStateWithLifecycle()
    val activeId by vm.activeId.collectAsStateWithLifecycle()
    val query by vm.search.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val showSettings by vm.showSettings.collectAsStateWithLifecycle()
    val confirmClear by vm.confirmClear.collectAsStateWithLifecycle()
    val renameId by vm.renameId.collectAsStateWithLifecycle()
    val confirmDeleteId by vm.confirmDeleteId.collectAsStateWithLifecycle()

    val p = atriaPalette(settings.darkTheme)
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snacks = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val ctx = LocalContext.current
    val configuration = LocalConfiguration.current
    val compactTopBar = configuration.screenWidthDp < 380
    val compactHeight = configuration.screenHeightDp < 500
    var showModel by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

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
        if (!ready) {
            Box(
                modifier = Modifier.fillMaxSize().background(p.bg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AtriaMark(p, markSize = 52)
                    Spacer(Modifier.height(18.dp))
                    CircularProgressIndicator(
                        color = p.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Loading your conversations…", color = p.dim, fontSize = 13.sp, fontFamily = BodyFamily)
                }
            }
        } else if (showSettings) {
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
                    Box(
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(p.surface1)
                    ) {
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
                            onSettings = {
                                scope.launch {
                                    drawer.close()
                                    vm.openSettings()
                                }
                            },
                            onToggleTheme = vm::toggleTheme,
                            dark = settings.darkTheme,
                            modifier = Modifier.fillMaxSize()
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
                                    fontFamily = DisplayFamily, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            actions = {
                                if (compactTopBar) {
                                    IconButton(onClick = { showModel = true }) {
                                        Icon(
                                            Icons.Filled.AutoAwesome,
                                            "Choose model: ${settings.model}",
                                            tint = p.dim,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(p.surface2)
                                            .border(1.dp, p.border, RoundedCornerShape(99.dp))
                                            .clickable(role = Role.Button) { showModel = true }
                                            .padding(horizontal = 11.dp, vertical = 7.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .background(p.accent)
                                            )
                                            Spacer(Modifier.width(7.dp))
                                            Text(
                                                settings.model, color = p.dim, fontSize = 11.5.sp,
                                                fontFamily = BodyFamily, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 96.dp)
                                            )
                                        }
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showTopMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, "More options", tint = p.dim)
                                    }
                                    DropdownMenu(
                                        expanded = showTopMenu,
                                        onDismissRequest = { showTopMenu = false },
                                        modifier = Modifier.background(p.surface2)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("New chat", color = p.text) },
                                            leadingIcon = { Icon(Icons.Filled.Add, null, tint = p.dim) },
                                            onClick = { showTopMenu = false; vm.newChat() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Choose model", color = p.text) },
                                            leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, tint = p.dim) },
                                            onClick = { showTopMenu = false; showModel = true }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export as Markdown", color = p.text) },
                                            leadingIcon = { Icon(Icons.Filled.Share, null, tint = p.dim) },
                                            enabled = messages.isNotEmpty(),
                                            onClick = { showTopMenu = false; vm.exportCurrent() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Clear chat", color = p.danger) },
                                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = p.danger) },
                                            enabled = messages.isNotEmpty(),
                                            onClick = { showTopMenu = false; vm.clearCurrent() }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = p.bg)
                        )
                    },
                    bottomBar = {
                        key(activeId) {
                            ComposerBar(
                                p = p,
                                generating = generating,
                                compactHeight = compactHeight,
                                draftFlow = vm.composerDraft,
                                onTextChange = vm::setComposerDraft,
                                canSend = settings.apiKey.isNotBlank(),
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
                    }
                ) { pad ->
                    val listState = rememberLazyListState()
                    var previousCount by remember(convo?.id) { mutableIntStateOf(messages.size) }
                    val tailThresholdPx = with(LocalDensity.current) { 80.dp.roundToPx() }

                    // Open each conversation at its true bottom. While a response grows,
                    // follow only when the reader was already geometrically near the tail.
                    LaunchedEffect(convo?.id) {
                        previousCount = 0
                        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex, Int.MAX_VALUE)
                    }
                    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
                        if (messages.isEmpty()) {
                            previousCount = 0
                            return@LaunchedEffect
                        }
                        val info = listState.layoutInfo
                        val lastVisible = info.visibleItemsInfo.lastOrNull()
                        val atTail = lastVisible != null && lastVisible.index == messages.lastIndex &&
                            (!listState.canScrollForward ||
                                lastVisible.offset + lastVisible.size >= info.viewportEndOffset - tailThresholdPx)
                        val grewNearTail = messages.size > previousCount &&
                            (previousCount <= 2 || atTail || (lastVisible?.index ?: -1) >= previousCount - 2)
                        if (grewNearTail || atTail) {
                            listState.scrollToItem(messages.lastIndex, Int.MAX_VALUE)
                        }
                        previousCount = messages.size
                    }
                    val showJump by remember(convo?.id, messages.size, tailThresholdPx) {
                        derivedStateOf {
                            if (messages.isEmpty()) return@derivedStateOf false
                            val info = listState.layoutInfo
                            val lastVisible = info.visibleItemsInfo.lastOrNull()
                            lastVisible == null || lastVisible.index != messages.lastIndex ||
                                (listState.canScrollForward &&
                                    lastVisible.offset + lastVisible.size < info.viewportEndOffset - tailThresholdPx)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(p.bg).padding(pad),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Readability cap on wide screens (tablet / landscape)
                        Column(modifier = Modifier.fillMaxHeight().widthIn(max = ThreadMaxWidth).fillMaxWidth()) {
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
                                    key = { _, message -> message.id }
                                ) { idx, m ->
                                    Box {
                                        if (m.role == "user") {
                                            UserBubble(
                                                msg = m, p = p,
                                                onCopy = {
                                                    clipboard.setText(AnnotatedString(m.content))
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    vm.toast("Copied to clipboard")
                                                },
                                                onEditSave = { vm.saveEdit(idx, it) }
                                            )
                                        } else {
                                            val isLast = idx == messages.size - 1
                                            val streaming = generating && isLast && streamingConvoId == convo?.id
                                            AiMessage(
                                                msg = m, isLast = isLast,
                                                generating = generating && streamingConvoId == convo?.id,
                                                streaming = streaming, p = p,
                                                onCopy = {
                                                    clipboard.setText(AnnotatedString(m.content))
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    vm.toast("Copied to clipboard")
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
                        }
                        // Scroll-to-latest FAB, ChatGPT-style placement
                        AnimatedVisibility(
                            visible = showJump && messages.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                        ) {
                            IconButton(
                                onClick = { scope.launch { listState.animateScrollToItem(messages.lastIndex, Int.MAX_VALUE) } },
                                modifier = Modifier
                                    .shadow(8.dp, CircleShape)
                                    .background(p.surface2, CircleShape)
                                    .border(1.dp, p.border, CircleShape)
                                    .size(48.dp)
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
    compactHeight: Boolean,
    draftFlow: kotlinx.coroutines.flow.StateFlow<String>,
    onTextChange: (String) -> Unit,
    canSend: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onCommand: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val text by draftFlow.collectAsStateWithLifecycle()
    var palSel by remember { mutableStateOf(0) }
    val showPalette = text.startsWith("/") && !generating
    val paletteMaxHeight = if (compactHeight) 96.dp else 248.dp
    val matches = remember(text) {
        if (!showPalette) emptyList()
        else {
            val q = text.drop(1).split(Regex("\\s")).first().lowercase()
            COMMANDS.filter { it.cmd.drop(1).startsWith(q) }.take(5)
        }
    }

    fun submit() {
        if (generating) return
        val value = text.trim()
        if (value.isEmpty()) return
        if (showPalette && matches.isNotEmpty()) {
            onCommand(matches[palSel % matches.size].cmd)
            onTextChange("")
        } else {
            onSend(value)
            if (canSend) onTextChange("")
        }
        palSel = 0
    }

    // System voice input — no permission needed (delegates to recognizer UI)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heard = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!heard.isNullOrEmpty()) onTextChange((text.trim() + " " + heard).trim())
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

    Box(
        modifier = Modifier.fillMaxWidth().background(p.bg),
        contentAlignment = Alignment.Center
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = ThreadMaxWidth).fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 4.dp)
    ) {
        if (showPalette && matches.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = paletteMaxHeight)
                    .verticalScroll(rememberScrollState())
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
                            .clickable { onCommand(c.cmd); onTextChange(""); palSel = 0 }
                            .heightIn(min = 48.dp)
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
                        Text(
                            c.desc, color = p.dim, fontSize = 13.sp, fontFamily = BodyFamily,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
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
                onValueChange = { onTextChange(it); palSel = 0 },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                textStyle = TextStyle(color = p.text, fontSize = 15.5.sp, lineHeight = 22.sp, fontFamily = BodyFamily),
                cursorBrush = SolidColor(p.accent),
                maxLines = if (compactHeight) 3 else 6,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Message Atria…", color = p.faint, fontSize = 15.5.sp, fontFamily = BodyFamily)
                    inner()
                }
            )
            if (!generating) {
                IconButton(onClick = ::startVoice, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Mic, "Voice input", tint = p.faint, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(2.dp))
            val enabled = generating || text.isNotBlank()
            val bg = if (!enabled) p.surface3 else p.accent
            val fg = if (!enabled) p.faint else p.onAccent
            IconButton(
                enabled = enabled,
                onClick = {
                    if (generating) onStop() else submit()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(if (generating) 1.dp else 0.dp, p.borderStrong, CircleShape)
            ) {
                Icon(
                    if (generating) Icons.Filled.Stop else Icons.Filled.ArrowUpward,
                    if (generating) "Stop" else "Send",
                    tint = fg,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (!compactHeight) {
            Spacer(Modifier.height(7.dp))
            Text(
                "Atria can make mistakes — verify important information.",
                color = p.faint, fontSize = 11.5.sp, fontFamily = BodyFamily,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            )
        }
    }
    }
}
