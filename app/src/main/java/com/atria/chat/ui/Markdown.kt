package com.atria.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Professional Markdown — headers, lists, tables, quotes, code, bold/italic
// No external dependency: deterministic, fast, selectable.
// ---------------------------------------------------------------------------

private sealed interface Block {
    data class Para(val text: String) : Block
    data class H(val level: Int, val text: String) : Block
    data class Code(val lang: String, val code: String) : Block
    data class Quote(val text: String) : Block
    data class Ul(val items: List<Item>) : Block
    data class Ol(val items: List<Item>) : Block
    data class Task(val items: List<TaskItem>) : Block
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block
    data object Hr : Block
}

data class Item(val depth: Int, val text: String)
data class TaskItem(val done: Boolean, val text: String)

private fun parseBlocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val lines = src.replace("\r\n", "\n").split("\n")
    var i = 0
    val para = mutableListOf<String>()
    fun flush() {
        if (para.isNotEmpty()) {
            out.add(Block.Para(para.joinToString("\n")))
            para.clear()
        }
    }
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        if (t.startsWith("```")) {
            flush()
            val lang = t.removePrefix("```").trim().take(24)
            val buf = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                buf.add(lines[i]); i++
            }
            i++ // skip closing fence
            out.add(Block.Code(lang, buf.joinToString("\n")))
            continue
        }
        if (t.isEmpty()) { flush(); i++; continue }
        if (t.matches(Regex("^#{1,4}\\s+.*"))) {
            flush()
            val level = t.takeWhile { it == '#' }.length
            out.add(Block.H(level, t.drop(level).trim()))
            i++; continue
        }
        if (t.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$"))) { flush(); out.add(Block.Hr); i++; continue }
        if (t.startsWith(">")) {
            flush()
            val buf = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                buf.add(lines[i].trim().removePrefix(">").trim()); i++
            }
            out.add(Block.Quote(buf.joinToString("\n")))
            continue
        }
        if (t.startsWith("|") && i + 1 < lines.size && lines[i + 1].contains(Regex("\\|?\\s*:?-{3,}:?\\s*\\|"))) {
            flush()
            fun splitRow(r: String) = r.trim().trim('|').split("|").map { it.trim() }
            val header = splitRow(t)
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                rows.add(splitRow(lines[i].trim())); i++
            }
            out.add(Block.Table(header, rows))
            continue
        }
        val taskM = Regex("^[-*•]\\s+\\[([ xX])\\]\\s+(.*)").find(t)
        if (taskM != null) {
            flush()
            val items = mutableListOf<TaskItem>()
            while (i < lines.size) {
                val m = Regex("^[-*•]\\s+\\[([ xX])\\]\\s+(.*)").find(lines[i].trim()) ?: break
                items.add(TaskItem(m.groupValues[1].lowercase() == "x", m.groupValues[2].trim())); i++
            }
            out.add(Block.Task(items))
            continue
        }
        if (t.matches(Regex("^([-*•]\\s+).*"))) {
            flush()
            val items = mutableListOf<Item>()
            while (i < lines.size) {
                val lt = lines[i]
                val ltt = lt.trim()
                if (!ltt.matches(Regex("^([-*•]\\s+).*"))) break
                val depth = (lt.length - lt.trimStart().length) / 2
                items.add(Item(depth.coerceIn(0, 4), ltt.drop(2).trim())); i++
            }
            if (items.isNotEmpty()) out.add(Block.Ul(items))
            continue
        }
        if (t.matches(Regex("^(\\d+[.)]\\s+).*"))) {
            flush()
            val items = mutableListOf<Item>()
            while (i < lines.size && lines[i].trim().matches(Regex("^(\\d+[.)]\\s+).*"))) {
                val lt = lines[i]
                val depth = (lt.length - lt.trimStart().length) / 2
                items.add(Item(depth.coerceIn(0, 4), lt.trim().replaceFirst(Regex("^\\d+[.)]\\s+"), ""))); i++
            }
            out.add(Block.Ol(items))
            continue
        }
        para.add(line)
        i++
    }
    flush()
    return out
}

/** Inline: `code`, **bold**, ~~strike~~, *italic*, [text](url), bare URLs. */
@Composable
fun inline(text: String, base: Color, accent: Color, monoBg: Color, fontSize: Int = 15): AnnotatedString {
    return remember(text, base, accent) {
        buildAnnotatedString {
            var idx = 0
            // token regex: code | bold | strike | italic | mdlink | url
            val re = Regex("(`[^`\\n]+`)|(\\*\\*[^*\\n]+\\*\\*)|(~~[^~\\n]+~~)|(\\*[^*\\n]+\\*)|(\\[[^\\]]+\\]\\([^)]+\\))|(https?://[^\\s<\"]+)")
            for (m in re.findAll(text)) {
                if (m.range.first > idx) append(text.substring(idx, m.range.first))
                val tok = m.value
                when {
                    tok.startsWith("`") -> withStyle(SpanStyle(color = accent, fontFamily = MonoFamily, fontSize = (fontSize - 1).sp, background = monoBg)) {
                        append(tok.trim('`'))
                    }
                    tok.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(tok.removePrefix("**").removeSuffix("**"))
                    }
                    tok.startsWith("~~") -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(tok.removePrefix("~~").removeSuffix("~~"))
                    }
                    tok.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(tok.trim('*'))
                    }
                    tok.startsWith("[") -> {
                        val label = tok.substringAfter("[").substringBefore("]")
                        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) { append(label) }
                    }
                    else -> {
                        // bare URL — strip trailing punctuation
                        var url = tok
                        var tail = ""
                        while (url.isNotEmpty() && url.last() in ".,;:!?") {
                            tail = url.last() + tail
                            url = url.dropLast(1)
                        }
                        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) { append(url) }
                        if (tail.isNotEmpty()) append(tail)
                    }
                }
                idx = m.range.last + 1
            }
            if (idx < text.length) append(text.substring(idx))
        }
    }
}

private val LANG_NAMES = mapOf(
    "js" to "JavaScript", "javascript" to "JavaScript", "ts" to "TypeScript",
    "typescript" to "TypeScript", "py" to "Python", "python" to "Python",
    "html" to "HTML", "css" to "CSS", "json" to "JSON", "bash" to "Bash",
    "sh" to "Shell", "shell" to "Shell", "md" to "Markdown", "sql" to "SQL",
    "java" to "Java", "kotlin" to "Kotlin", "c" to "C", "cpp" to "C++",
    "cs" to "C#", "go" to "Go", "rs" to "Rust", "yaml" to "YAML", "xml" to "XML"
)

@Composable
fun ProMarkdown(text: String, p: AtriaPalette) {
    val blocks = remember(text) { parseBlocks(text) }
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            blocks.forEach { b ->
                when (b) {
                    is Block.H -> {
                        val size = when (b.level) { 1 -> 21; 2 -> 19; else -> 17 }
                        Text(
                            text = inline(b.text, p.text, p.accentStrong, p.surface3, size),
                            color = p.text,
                            fontSize = size.sp,
                            fontFamily = DisplayFamily,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = (size + 6).sp,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    is Block.Para -> {
                        Text(
                            text = inline(b.text, p.text, p.accentStrong, p.surface3),
                            color = p.text,
                            fontSize = 15.sp,
                            fontFamily = BodyFamily,
                            lineHeight = 24.sp
                        )
                    }
                    is Block.Quote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(p.accent, RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = inline(b.text, p.dim, p.accentStrong, p.surface3),
                                color = p.dim,
                                fontSize = 14.5.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is Block.Ul -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            b.items.forEach { item ->
                                Row(modifier = Modifier.padding(start = (item.depth * 18).dp)) {
                                    Text("•  ", color = p.faint, fontSize = 15.sp, fontFamily = BodyFamily)
                                    Text(
                                        text = inline(item.text, p.text, p.accentStrong, p.surface3),
                                        color = p.text,
                                        fontSize = 15.sp,
                                        fontFamily = BodyFamily,
                                        lineHeight = 23.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    is Block.Ol -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            var n = 0
                            b.items.forEach { item ->
                                if (item.depth == 0) n++
                                Row(modifier = Modifier.padding(start = (item.depth * 18).dp)) {
                                    Text(
                                        if (item.depth == 0) "$n.  " else "–  ",
                                        color = p.faint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                        fontFamily = BodyFamily
                                    )
                                    Text(
                                        text = inline(item.text, p.text, p.accentStrong, p.surface3),
                                        color = p.text,
                                        fontSize = 15.sp,
                                        fontFamily = BodyFamily,
                                        lineHeight = 23.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    is Block.Task -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            b.items.forEach { item ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 3.dp)
                                            .size(17.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .border(1.5.dp, if (item.done) p.accent else p.borderStrong, RoundedCornerShape(5.dp))
                                            .background(if (item.done) p.accent else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.done) Text("✓", color = p.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = inline(item.text, p.text, p.accentStrong, p.surface3),
                                        color = if (item.done) p.faint else p.text,
                                        fontSize = 15.sp,
                                        fontFamily = BodyFamily,
                                        lineHeight = 23.sp,
                                        style = if (item.done) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics {
                                                stateDescription = if (item.done) "Completed" else "Not completed"
                                            }
                                    )
                                }
                            }
                        }
                    }
                    is Block.Table -> ProTable(b, p)
                    is Block.Hr -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(p.border)
                    )
                    is Block.Code -> CodeBlockCard(b, p)
                }
            }
        }
    }
}

@Composable
private fun ProTable(b: Block.Table, p: AtriaPalette) {
    val scroll = rememberScrollState()
    val tableWidth = (b.header.size.coerceAtLeast(1) * 128).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, p.border, RoundedCornerShape(12.dp))
            .background(p.surface1)
    ) {
        Column(
            modifier = Modifier
                .width(tableWidth)
                .horizontalScroll(scroll)
        ) {
            Row(modifier = Modifier.background(p.surface2)) {
                b.header.forEach { h ->
                    Text(
                        text = h,
                        color = p.text,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = BodyFamily,
                        fontSize = 13.5.sp,
                        modifier = Modifier
                            .width(128.dp)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(p.borderStrong))
            b.rows.forEachIndexed { ri, row ->
                Row(modifier = Modifier.background(if (ri % 2 == 1) p.surface2.copy(alpha = 0.45f) else Color.Transparent)) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            color = p.dim,
                            fontFamily = BodyFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier
                                .width(128.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(b: Block.Code, p: AtriaPalette) {
    val clipboard = LocalClipboardManager.current
    val label = LANG_NAMES[b.lang.lowercase()].let {
        if (it != null) it else if (b.lang.isBlank()) "text" else b.lang
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF2A3038), RoundedCornerShape(14.dp))
            .background(p.codeBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x14FFFFFF))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = p.accentStrong,
                fontFamily = MonoFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { clipboard.setText(AnnotatedString(b.code)) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", tint = Color(0xFF8B949E))
            }
        }
        Text(
            text = b.code,
            color = Color(0xFFE6EDF3),
            fontFamily = MonoFamily,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }
}
