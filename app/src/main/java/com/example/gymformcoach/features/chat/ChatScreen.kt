package com.example.gymformcoach.features.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.core.data.ChatMessage
import com.example.gymformcoach.core.data.ChatRole
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Surface
import com.example.gymformcoach.core.designsystem.TextSecondary

private val STARTER_PROMPTS = listOf(
    "Why did my squat depth drop off?",
    "Suggest a warm-up for deadlifts",
    "Explain my last session's feedback"
)

/**
 * Coach Chat: free-form Q&A, separate from the automatic post-set analysis card and the
 * §17D plan Coach's proposal/diff flow - this screen never mutates a routine directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val streaming by viewModel.streaming.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val totalItems = messages.size + if (streaming != StreamingState.Idle) 1 else 0
    LaunchedEffect(totalItems, (streaming as? StreamingState.Streaming)?.partial) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }, enabled = messages.isNotEmpty()) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear conversation")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (messages.isEmpty() && streaming == StreamingState.Idle) {
                EmptyState(
                    modifier = Modifier.weight(1f),
                    onPromptSelected = { viewModel.send(it) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onRetry = { viewModel.retryLastReply() }.takeIf {
                                message.role == ChatRole.SYSTEM && message.id == messages.lastOrNull { m -> m.role == ChatRole.SYSTEM }?.id
                            }
                        )
                    }
                    when (val current = streaming) {
                        is StreamingState.Connecting -> item(key = "typing") { TypingBubble() }
                        is StreamingState.Streaming -> item(key = "streaming") { StreamingBubble(current.partial) }
                        StreamingState.Idle -> Unit
                    }
                }
            }

            HorizontalDivider(color = Surface)
            InputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.send(inputText)
                        inputText = ""
                    }
                }
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear conversation?") },
            text = { Text("This deletes every message in this chat. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearConversation()
                    showClearConfirm = false
                }) { Text("Clear", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onPromptSelected: (String) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Ask your coach anything",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Grounded in your profile and recent sessions.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        STARTER_PROMPTS.forEach { prompt ->
            SuggestionChip(
                onClick = { onPromptSelected(prompt) },
                label = { Text(prompt) },
                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Surface)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onRetry: (() -> Unit)?) {
    when (message.role) {
        ChatRole.USER -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BubbleSurface(color = Primary, modifier = Modifier.widthIn(max = 300.dp)) {
                Text(message.content, color = Color.Black)
            }
        }
        ChatRole.SYSTEM -> Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BubbleSurface(color = Surface, modifier = Modifier.widthIn(max = 320.dp)) {
                Text(message.content, style = MaterialTheme.typography.bodySmall, color = Error)
            }
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Retry", color = Primary) }
            }
        }
        else -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            BubbleSurface(color = Surface, modifier = Modifier.widthIn(max = 340.dp)) {
                Text(message.content)
            }
        }
    }
}

@Composable
private fun StreamingBubble(partial: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        BubbleSurface(color = Surface, modifier = Modifier.widthIn(max = 340.dp)) {
            Text(partial.ifEmpty { " " })
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        BubbleSurface(color = Surface) {
            val transition = rememberInfiniteTransition(label = "typing")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(TextSecondary.copy(alpha = alpha), shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleSurface(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = color,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { content() }
    }
}

@Composable
private fun InputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask your coach...") },
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                cursorColor = Primary
            )
        )
        FilledIconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}
