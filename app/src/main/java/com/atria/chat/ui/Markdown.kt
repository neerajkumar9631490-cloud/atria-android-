package com.atria.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    data class Ul(val items: List<String>) : Block
    data class Ol(val items: List<String>) : Block
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block
    data object Hr : Block
}

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
        if (t.matches(Regex("^([-*•]\\s+).*"))) {
            flush()
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().matches(Regex("^([-*•]\\s+).*"))) {
                items.add(lines[i].trim().drop(2).trim()); i++
            }
            out.add(Block.Ul(items))
            continue
        }
        if (t.matches(Regex("^(\\d+[.)]\\s+).*"))) {
            flush()
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().matches(Regex("^(\\d+[.)]\\s+).*"))) {
                items.add(lines[i].trim().replaceFirst(Regex("^\\d+[.)]\\s+"), "")); i++
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

/** Inline: `code`, **bold**, *italic*, [text](url) stripped to text + style. */
@Composable
fun inline(text: String, base: Color, accent: Color, monoBg: Color, fontSize: Int = 15): AnnotatedString {
    return remember(text, base, accent) {
        buildAnnotatedString {
            var idx = 0
            // token regex: code | bold | italic | link
            val re = Regex("(`[^`]+`)|(\\*\\*[^*]+\\*\\*)|(\\*[^*\\n]+\\*)|(\\[[^\\]]+\\]\\([^)]+\\))")
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
                    tok.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(tok.trim('*'))
                    }
                    tok.startsWith("[") -> {
                        val label = tok.substringAfter("[").substringBefore("]")
                        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) { append(label) }
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
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = (size + 6).sp
                        )
                    }
                    is Block.Para -> {
                        Text(
                            text = inline(b.text, p.text, p.accentStrong, p.surface3),
                            color = p.text,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )
                    }
                    is Block.Quote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(44.dp)
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
                                Row {
                                    Text("•  ", color = p.faint, fontSize = 15.sp)
                                    Text(
                                        text = inline(item, p.text, p.accentStrong, p.surface3),
                                        color = p.text,
                                        fontSize = 15.sp,
                                        lineHeight = 23.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    is Block.Ol -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            b.items.forEachIndexed { k, item ->
                                Row {
                                    Text("${k + 1}.  ", color = p.faint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = inline(item, p.text, p.accentStrong, p.surface3),
                                        color = p.text,
                                        fontSize = 15.sp,
                                        lineHeight = 23.sp,
                                        modifier = Modifier.weight(1f)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, p.border, RoundedCornerShape(12.dp))
            .background(p.surface1)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(0.dp)
        ) {
            Row(modifier = Modifier.background(p.surface2)) {
                b.header.forEach { h ->
                    Text(
                        text = h,
                        color = p.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        modifier = Modifier
                            .width(150.dp)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            b.rows.forEach { row ->
                Row {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            color = p.dim,
                            fontSize = 13.5.sp,
                            modifier = Modifier
                                .width(150.dp)
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
