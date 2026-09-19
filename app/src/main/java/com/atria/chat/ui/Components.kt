package com.atria.chat.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atria.chat.data.ChatMessage
import com.atria.chat.data.Conversation
import com.atria.chat.data.bucketOf

// ---------------------------------------------------------------------------
// Small atoms
// ---------------------------------------------------------------------------

@Composable
fun AtriaMark(p: AtriaPalette, size: Int = 32, iconSize: Int = 16) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(p.accentSoft)
            .border(1.dp, p.accent.copy(alpha = 0.38f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("A", color = p.accentStrong, fontWeight = FontWeight.Bold, fontSize = iconSize.sp)
    }
}

@Composable
fun ThinkingDots(p: AtriaPalette) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { k ->
            val a by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550, delayMillis = k * 180, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "d$k"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(a)
                    .clip(RoundedCornerShape(99.dp))
                    .background(p.accent)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("Atria is thinking", color = p.dim, fontSize = 14.sp)
    }
}

@Composable
fun StreamingCaret(p: AtriaPalette) {
    val t = rememberInfiniteTransition(label = "caret")
    val a by t.animateFloat(1f, 0f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "blink")
    Box(
        modifier = Modifier
            .alpha(a)
            .size(width = 8.dp, height = 16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(p.accent)
    )
}

// ---------------------------------------------------------------------------
// Messages
// ---------------------------------------------------------------------------

@Composable
fun UserBubble(msg: ChatMessage, p: AtriaPalette, onCopy: () -> Unit, onEditSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(msg.content) { mutableStateOf(msg.content) }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        if (!editing) {
            SelectionContainer {
                Text(
                    text = msg.content,
                    color = p.text,
                    fontSize = 15.sp,
                    fontFamily = BodyFamily,
                    lineHeight = 23.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                        .background(p.surface2)
                        .border(1.dp, p.border, RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                )
            }
            Row {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, "Copy", tint = p.faint, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { editing = true; draft = msg.content }) {
                    Icon(Icons.Filled.Edit, "Edit", tint = p.faint, modifier = Modifier.size(17.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                    focusedTextColor = p.text, unfocusedTextColor = p.text,
                    cursorColor = p.accent
                ),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { editing = false }) { Text("Cancel", color = p.dim) }
                Button(
                    onClick = { editing = false; onEditSave(draft) },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Save & submit") }
            }
        }
    }
}

@Composable
fun AiMessage(
    msg: ChatMessage,
    isLast: Boolean,
    generating: Boolean,
    streaming: Boolean,
    p: AtriaPalette,
    onCopy: () -> Unit,
    onRegen: () -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var vote by remember(msg.content, msg.error) { mutableStateOf(0) } // 0 none · 1 up · -1 down
    Row(modifier = Modifier.fillMaxWidth()) {
        AtriaMark(p)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Atria", color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFamily = BodyFamily)
                val meta = buildList {
                    msg.metaModel?.takeIf { it.isNotBlank() }?.let { add(it) }
                    msg.metaMs?.let { add("%.1fs".format(it / 1000.0)) }
                    if (msg.stopped) add("stopped")
                }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = p.faint, fontSize = 12.sp, fontFamily = BodyFamily)
            }
            Spacer(Modifier.height(6.dp))
            when {
                msg.content.isBlank() && msg.error == null && generating ->
                    ThinkingDots(p)
                msg.content.isBlank() && msg.error != null ->
                    ErrorCard(msg.error, p, onRetry, onOpenSettings)
                else -> {
                    ProMarkdown(msg.content, p)
                    if (streaming) {
                        Spacer(Modifier.height(4.dp))
                        StreamingCaret(p)
                    }
                    if (!msg.error.isNullOrBlank() && msg.content.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        ErrorCard(msg.error, p, onRetry, onOpenSettings)
                    }
                    if (!generating) {
                        // ChatGPT-style ghost action row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.ContentCopy, "Copy", tint = p.faint, modifier = Modifier.size(16.dp))
                            }
                            if (!msg.local) {
                                IconButton(
                                    onClick = { vote = if (vote == 1) 0 else 1 },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ThumbUp, "Good response",
                                        tint = if (vote == 1) p.accentStrong else p.faint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { vote = if (vote == -1) 0 else -1 },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ThumbDown, "Bad response",
                                        tint = if (vote == -1) p.accentStrong else p.faint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Share, "Share", tint = p.faint, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (isLast && !msg.local) {
                                IconButton(onClick = onRegen, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Refresh, "Regenerate", tint = p.faint, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorCard(msg: String, p: AtriaPalette, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AtriaDanger.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .background(AtriaDanger.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Warning, null, tint = AtriaDanger, modifier = Modifier.size(16.dp))
            Text(msg, color = AtriaDanger, fontSize = 13.5.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.surface2, contentColor = p.text)
            ) { Text("Retry", fontSize = 12.5.sp) }
            TextButton(onClick = onOpenSettings) { Text("Open settings", color = p.dim, fontSize = 12.5.sp) }
        }
    }
}

// ---------------------------------------------------------------------------
// Welcome
// ---------------------------------------------------------------------------

@Composable
fun Welcome(greeting: String, p: AtriaPalette, onSuggest: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        AtriaMark(p, size = 52, iconSize = 26)
        Spacer(Modifier.height(18.dp))
        Text(greeting, color = p.text, fontSize = 31.sp, fontWeight = FontWeight.SemiBold, fontFamily = DisplayFamily, letterSpacing = (-0.5).sp, lineHeight = 37.sp)
        Spacer(Modifier.height(6.dp))
        Text("What should we work on today?", color = p.dim, fontSize = 16.sp, fontFamily = BodyFamily)
        Spacer(Modifier.height(22.dp))
        val cards = listOf(
            Triple(Icons.Filled.Lightbulb, "Explain simply", "How HTTPS keeps your data safe") to
                "Explain how HTTPS keeps my data private — like I'm a curious beginner.",
            Triple(Icons.Filled.Code, "Write some code", "Sort a messy downloads folder automatically") to
                "Write a Python script that organizes the files in my Downloads folder by type.",
            Triple(Icons.Filled.Edit, "Draft a message", "A polite email to reschedule a meeting") to
                "Draft a short, friendly email moving tomorrow's meeting to next Thursday afternoon.",
            Triple(Icons.Filled.AutoAwesome, "Brainstorm ideas", "Side projects with a one-line pitch") to
                "Brainstorm ten unconventional side-project ideas for a web developer, with a one-line pitch each."
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (meta, prompt) ->
                        val (icon, title, sub) = meta
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(p.surface1)
                                .border(1.dp, p.border, RoundedCornerShape(14.dp))
                                .clickable { onSuggest(prompt) }
                                .padding(15.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(p.accentSoft)
                                        .border(1.dp, p.accent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = p.accentStrong, modifier = Modifier.size(17.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, fontFamily = BodyFamily)
                                    Spacer(Modifier.height(3.dp))
                                    Text(sub, color = p.dim, fontSize = 12.5.sp, fontFamily = BodyFamily, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Drawer (chat history)
// ---------------------------------------------------------------------------

@Composable
fun HistoryDrawer(
    convos: List<Conversation>,
    activeId: String?,
    query: String,
    p: AtriaPalette,
    hasKey: Boolean,
    onQuery: (String) -> Unit,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    onShare: (String) -> Unit,
    onSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    dark: Boolean
) {
    val filtered = remember(convos, query) {
        val q = query.trim().lowercase()
        val sorted = convos.sortedByDescending { it.updated }
        if (q.isBlank()) sorted else sorted.filter {
            it.title.lowercase().contains(q) || it.messages.any { m -> m.content.lowercase().contains(q) }
        }
    }
    Column(modifier = Modifier.background(p.surface1)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            AtriaMark(p, 34, 17)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Atria", color = p.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = DisplayFamily)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, p.border, RoundedCornerShape(99.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text("PRO", color = p.faint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = BodyFamily) }
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (hasKey) AtriaSuccess else p.faint)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasKey) "API key ready" else "No API key — open Settings",
                        color = p.faint, fontSize = 11.5.sp, fontFamily = BodyFamily
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.surface2, contentColor = p.text)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("New chat", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (android.os.Build.VERSION.SDK_INT > 0) "Ctrl K" else "", color = p.faint, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(p.surface2)
                .border(1.dp, p.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = p.faint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = p.text, fontSize = 13.sp),
                cursorBrush = SolidColor(p.accent),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search chats", color = p.faint, fontSize = 13.sp)
                    inner()
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f)) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        if (query.isBlank()) "No chats yet — start a conversation." else "No chats match your search.",
                        color = p.faint, fontSize = 13.sp,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            } else {
                var lastBucket = ""
                filtered.forEach { c ->
                    val b = bucketOf(c.updated)
                    if (b != lastBucket) {
                        lastBucket = b
                        item(key = "h-$b") {
                            Text(
                                b.uppercase(),
                                color = p.faint, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp)
                            )
                        }
                    }
                    item(key = c.id) {
                        val sel = c.id == activeId
                        var menu by remember(c.id) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) p.surface2 else Color.Transparent)
                                .clickable { onOpen(c.id) }
                                .padding(start = 14.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (sel) Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 20.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(p.accent)
                            )
                            if (sel) Spacer(Modifier.width(8.dp))
                            Text(
                                c.title, color = if (sel) p.text else p.dim, fontSize = 13.5.sp,
                                fontFamily = BodyFamily,
                                maxLines = 1, modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Filled.MoreVert, "Chat options", tint = p.faint, modifier = Modifier.size(16.dp))
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = menu,
                                    onDismissRequest = { menu = false },
                                    modifier = Modifier.background(p.surface2)
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Rename", color = p.text, fontSize = 13.5.sp, fontFamily = BodyFamily) },
                                        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = p.dim, modifier = Modifier.size(16.dp)) },
                                        onClick = { menu = false; onRename(c.id) }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Share", color = p.text, fontSize = 13.5.sp, fontFamily = BodyFamily) },
                                        leadingIcon = { Icon(Icons.Filled.Share, null, tint = p.dim, modifier = Modifier.size(16.dp)) },
                                        onClick = { menu = false; onShare(c.id) }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Delete", color = AtriaDanger, fontSize = 13.5.sp, fontFamily = BodyFamily) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = AtriaDanger, modifier = Modifier.size(16.dp)) },
                                        onClick = { menu = false; onDelete(c.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Settings, null, tint = p.dim, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Settings", color = p.dim)
            }
            IconButton(onClick = onToggleTheme) {
                Text(if (dark) "☾" else "☀", color = p.dim, fontSize = 17.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings dialog
// ---------------------------------------------------------------------------

@Composable
fun SettingsDialog(
    apiKey: String,
    model: String,
    system: String,
    p: AtriaPalette,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var k by remember(apiKey) { mutableStateOf(apiKey) }
    var m by remember(model) { mutableStateOf(model) }
    var s by remember(system) { mutableStateOf(system) }
    var show by remember { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(k, m, s) },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = p.dim) } },
        title = { Text("Settings", color = p.text, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("API KEY", color = p.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = k, onValueChange = { k = it },
                    placeholder = { Text("Paste your Atria API key", color = p.faint, fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { show = !show }) {
                            Text(if (show) "Hide" else "Show", color = p.accentStrong, fontSize = 12.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                        focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                    ),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
                )
                Text("Stored only on this device, sent straight to api.atria-asi.ai — nowhere else.",
                    color = p.faint, fontSize = 12.sp)
                Text("MODEL", color = p.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = m, onValueChange = { m = it }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                        focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                    ),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
                )
                Text("SYSTEM PROMPT (OPTIONAL)", color = p.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = s, onValueChange = { s = it }, minLines = 2, maxLines = 4,
                    placeholder = { Text("e.g. You are a concise, senior engineering assistant.", color = p.faint, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                        focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                    ),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = p.surface1
    )
}

@Composable
fun ConfirmDialog(title: String, msg: String, confirm: String, p: AtriaPalette, onNo: () -> Unit, onYes: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onNo,
        confirmButton = {
            Button(
                onClick = onYes,
                colors = ButtonDefaults.buttonColors(containerColor = AtriaDanger, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onNo) { Text("Cancel", color = p.dim) } },
        title = { Text(title, color = p.text, fontWeight = FontWeight.SemiBold) },
        text = { Text(msg, color = p.dim, fontSize = 14.sp) },
        containerColor = p.surface1
    )
}

@Composable
fun RenameDialog(initial: String, p: AtriaPalette, onNo: () -> Unit, onYes: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onNo,
        confirmButton = {
            Button(
                onClick = { onYes(v) },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp)); Text("Rename")
            }
        },
        dismissButton = {
            IconButton(onClick = onNo) { Icon(Icons.Filled.Close, null, tint = p.dim) }
        },
        title = { Text("Rename chat", color = p.text, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = v, onValueChange = { v = it }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                    focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                ),
                shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = p.surface1
    )
}

// ---------------------------------------------------------------------------
// Model picker bottom sheet — ChatGPT-style
// ---------------------------------------------------------------------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    current: String,
    p: AtriaPalette,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var custom by remember(current) { mutableStateOf(current) }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = p.surface1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(p.borderStrong)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Choose a model", color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, fontFamily = DisplayFamily)
            Spacer(Modifier.height(4.dp))
            Text("Trade off speed vs quality per conversation.", color = p.dim, fontSize = 13.sp, fontFamily = BodyFamily)
            Spacer(Modifier.height(16.dp))
            ModelRow(
                name = "Atria Dawn", desc = "Smartest · default reasoning", id = "Atria-Dawn-Preview",
                selected = current == "Atria-Dawn-Preview", p = p,
                onClick = { onPick("Atria-Dawn-Preview") }
            )
            Spacer(Modifier.height(8.dp))
            Text("CUSTOM MODEL ID", color = p.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFamily)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = custom, onValueChange = { custom = it }, singleLine = true,
                placeholder = { Text("e.g. Atria-Flash", color = p.faint, fontSize = 13.5.sp, fontFamily = BodyFamily) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                    focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val v = custom.trim()
                    if (v.isNotEmpty()) onPick(v)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent)
            ) { Text("Use this model", fontWeight = FontWeight.SemiBold, fontFamily = BodyFamily) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModelRow(name: String, desc: String, id: String, selected: Boolean, p: AtriaPalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) p.accentSoft else p.surface2)
            .border(1.dp, if (selected) p.accent.copy(alpha = 0.45f) else p.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(p.accentSoft)
                .border(1.dp, p.accent.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = p.accentStrong, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, fontFamily = BodyFamily)
            Text("$desc · $id", color = p.dim, fontSize = 12.sp, fontFamily = BodyFamily)
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = p.accentStrong, modifier = Modifier.size(19.dp))
    }
}

// ---------------------------------------------------------------------------
// Settings full screen — sectioned, premium
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    apiKey: String,
    model: String,
    system: String,
    dark: Boolean,
    p: AtriaPalette,
    onBack: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var k by remember(apiKey) { mutableStateOf(apiKey) }
    var m by remember(model) { mutableStateOf(model) }
    var s by remember(system) { mutableStateOf(system) }
    var d by remember(dark) { mutableStateOf(dark) }
    var show by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().background(p.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = p.text)
            }
            Text("Settings", color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFamily = DisplayFamily)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onSave(k, m, s, d) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent)
            ) { Text("Save", fontFamily = BodyFamily) }
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                SettingSection("ACCOUNT", p) {
                    Text("API KEY", color = p.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFamily)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = k, onValueChange = { k = it },
                        placeholder = { Text("Paste your Atria API key", color = p.faint, fontSize = 13.5.sp, fontFamily = BodyFamily) },
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { show = !show }) {
                                Text(if (show) "Hide" else "Show", color = p.accentStrong, fontSize = 12.sp, fontFamily = BodyFamily)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                            focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                        ),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingHint(
                        if (k.isBlank()) "No key yet — the API will reject requests until you add one."
                        else "Stored only on this device, sent only to api.atria-asi.ai.",
                        p
                    )
                }
            }
            item {
                SettingSection("MODEL", p) {
                    OutlinedTextField(
                        value = m, onValueChange = { m = it }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                            focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                        ),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingHint("Default: Atria-Dawn-Preview. Tip: type /model <id> in chat to switch fast.", p)
                }
            }
            item {
                SettingSection("ASSISTANT", p) {
                    Text("SYSTEM PROMPT", color = p.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFamily)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = s, onValueChange = { s = it }, minLines = 2, maxLines = 4,
                        placeholder = { Text("e.g. You are a concise, senior engineering assistant.", color = p.faint, fontSize = 13.5.sp, fontFamily = BodyFamily) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                            focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
                        ),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingHint("Quietly prepended to every conversation. Optional.", p)
                }
            }
            item {
                SettingSection("APPEARANCE", p) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeChoice("Dark", selected = d, p = p, modifier = Modifier.weight(1f)) { d = true }
                        ThemeChoice("Light", selected = !d, p = p, modifier = Modifier.weight(1f)) { d = false }
                    }
                }
            }
            item {
                SettingSection("ABOUT", p) {
                    SettingHint("Atria for Android · v1.0.0 · api.atria-asi.ai", p)
                }
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, p: AtriaPalette, content: @Composable () -> Unit) {
    Column {
        Text(title, color = p.faint, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = BodyFamily)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingHint(text: String, p: AtriaPalette) {
    Text(text, color = p.faint, fontSize = 12.5.sp, fontFamily = BodyFamily, lineHeight = 18.sp)
}

@Composable
private fun ThemeChoice(label: String, selected: Boolean, p: AtriaPalette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) p.accentSoft else p.surface2)
            .border(1.dp, if (selected) p.accent.copy(alpha = 0.5f) else p.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = if (selected) p.accentStrong else p.dim,
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = BodyFamily
        )
    }
}
