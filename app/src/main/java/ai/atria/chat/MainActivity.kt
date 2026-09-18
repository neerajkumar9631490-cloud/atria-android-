package ai.atria.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0B0E13)
private val SurfaceOne = Color(0xFF0F1319)
private val SurfaceTwo = Color(0xFF161B23)
private val BorderColor = Color(0xFF232B36)
private val TextPrimary = Color(0xFFE9EDF3)
private val TextDim = Color(0xFF9AA5B4)
private val Accent = Color(0xFFE2A45C)
private val OnAccent = Color(0xFF1A1106)

data class ChatMessage(
    val role: String,
    val text: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AtriaApp()
        }
    }
}

@Composable
fun AtriaApp() {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "assistant",
                text = "Atria professional Android shell is ready. Next milestone: live Atria streaming chat."
            )
        )
    }

    var input by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Bg
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar()

            Messages(
                messages = messages,
                modifier = Modifier.weight(1f)
            )

            Composer(
                input = input,
                onInput = { input = it },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        messages.add(ChatMessage(role = "user", text = text))
                        messages.add(
                            ChatMessage(
                                role = "assistant",
                                text = "This is the professional native shell. Live Atria streaming will be connected next."
                            )
                        )
                        input = ""
                    }
                }
            )
        }
    }
}

@Composable
fun TopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent.copy(alpha = 0.13f))
                    .border(
                        width = 1.dp,
                        color = Accent.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Atria Chat",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Professional",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            Pill(text = "Atria-Dawn-Preview")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderColor)
        )
    }
}

@Composable
fun Pill(text: String) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = BorderColor,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Accent)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = text,
                color = TextDim,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun Messages(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(messages) { message ->
            MessageItem(message = message)
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    if (message.role == "user") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceTwo)
                    .border(
                        width = 1.dp,
                        color = BorderColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent.copy(alpha = 0.13f))
                    .border(
                        width = 1.dp,
                        color = Accent.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Atria",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceOne)
                        .border(
                            width = 1.dp,
                            color = BorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Composer(
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceTwo)
                .border(
                    width = 1.dp,
                    color = BorderColor,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Message Atria…",
                        color = TextDim
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Accent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            )

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onSend,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = OnAccent
                )
            ) {
                Text(
                    text = "Send",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Atria can make mistakes — verify important information.",
            color = TextDim,
            fontSize = 11.sp
        )
    }
}
