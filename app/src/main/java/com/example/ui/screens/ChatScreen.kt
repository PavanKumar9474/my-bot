package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.database.ChatMessageEntity
import com.example.ui.viewmodel.ChatViewModel
import org.json.JSONArray
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val messages by viewModel.chatMessages.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, viewModel.isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Image picker contract
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.selectedImage = bitmap
                Toast.makeText(context, "Image loaded for upload!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission request contract for Speech-To-Text
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening(context)
        } else {
            Toast.makeText(context, "Microphone permission required for Voice assistant!", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Active Chat Title / Details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.img_chatatm_logo),
                contentDescription = "ChatATM Logo",
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ChatATM AI Workspace",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Powered by ${if (viewModel.selectedModel.contains("pro")) "Gemini Pro Ultra" else "Gemini Flash Turbo"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Clear button
            IconButton(
                onClick = {
                    viewModel.selectedChatId.value?.let { id ->
                        viewModel.clearChatHistory(id)
                        Toast.makeText(context, "History cleared!", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Session History",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Messages scrolling view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !viewModel.isSending) {
                // Empty state view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.img_chatatm_logo),
                        contentDescription = "ChatATM Logo",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Meet ChatATM",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your high-fidelity multilingual AI companion. Type in any language or press the microphone for continuous voice chatting.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Helpful starter templates
                    Text(
                        text = "Try these starters:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val starters = listOf(
                        "Explain quantum physics in German",
                        "Write a welcoming email in Japanese",
                        "Debug Kotlin OutOfMemoryError code"
                    )

                    starters.forEach { suggestion ->
                        Card(
                            onClick = { viewModel.sendPreloadedPrompt(suggestion) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Idea",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageBubble(
                            message = message,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(message.content))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            onSpeak = {
                                viewModel.speakText(message.content)
                            }
                        )
                    }

                    if (viewModel.isSending) {
                        item {
                            TypingIndicatorBubble()
                        }
                    }
                }
            }
        }

        // Active image preview container
        viewModel.selectedImage?.let { img ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = img,
                        contentDescription = "Selected image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attachment ready",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "This image will be analyzed with your prompt",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.selectedImage = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Input container
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .navigationBarsPadding()
            ) {
                // Real-time Speech-to-text listener text drawer
                if (viewModel.isSpeechListening || viewModel.speechResultText.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.isSpeechListening) Icons.Default.Mic else Icons.Default.CheckCircle,
                                contentDescription = "Voice status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = viewModel.speechResultText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.speechResultText = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Attachment / Camera Scan Button
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Select Image Attachment",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Text Input field
                    OutlinedTextField(
                        value = viewModel.textInput,
                        onValueChange = { viewModel.textInput = it },
                        placeholder = { Text("ChatATM in any language...") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (viewModel.textInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.textInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Voice Recognition / Send Message Toggle
                    if (viewModel.textInput.isEmpty()) {
                        IconButton(
                            onClick = {
                                if (viewModel.isSpeechListening) {
                                    viewModel.stopListening()
                                } else {
                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .background(
                                    color = if (viewModel.isSpeechListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (viewModel.isSpeechListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice Input Assistant",
                                tint = if (viewModel.isSpeechListening) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = !viewModel.isSending,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Message",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.role == "user"
    var showNlpDetails by remember { mutableStateOf(false) }

    val bubbleTextColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeStr = remember(message.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Message Container Card
        val bubbleModifier = if (isUser) {
            Modifier
                .fillMaxWidth(0.85f)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            Color(0xFFE91E63) // Vibrant pink for maximum color polish!
                        )
                    ),
                    shape = bubbleShape
                )
        } else {
            Modifier
                .fillMaxWidth(0.85f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = bubbleShape
                )
        }

        Box(
            modifier = bubbleModifier.padding(14.dp)
        ) {
            Column {
                // Custom Markdown Render
                MarkdownTextRenderer(
                    text = message.content,
                    textColor = bubbleTextColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = bubbleTextColor.copy(alpha = 0.6f)
                    )

                    Row(horizontalArrangement = Arrangement.End) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Response",
                                tint = bubbleTextColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak Response",
                                tint = bubbleTextColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expanded NLP Analytics Drawer for model responses
        if (!isUser) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { showNlpDetails = !showNlpDetails },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showNlpDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle NLP details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "NLP Insights (${message.detectedLanguage} • ${message.sentimentEmoji} ${message.sentiment})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = showNlpDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(start = 8.dp, top = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ADVANCED NLP METRICS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NlpMetricItem("Language", message.detectedLanguage)
                            NlpMetricItem("Sentiment", "${message.sentimentEmoji} ${message.sentiment}")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NlpMetricItem("Topic", message.category)
                            NlpMetricItem("Spam Check", if (message.isSpam) "Spam Detected ⚠️" else "Safe/No Spam ✅")
                        }

                        // Entities list
                        if (message.entitiesJson.isNotEmpty()) {
                            val jsonArray = remember(message.entitiesJson) {
                                try {
                                    JSONArray(message.entitiesJson)
                                } catch (e: Exception) {
                                    JSONArray()
                                }
                            }
                            if (jsonArray.length() > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "EXTRACTED NAMED ENTITIES:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (i in 0 until jsonArray.length()) {
                                        val entity = jsonArray.getString(i)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = entity,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
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
fun NlpMetricItem(label: String, value: String) {
    Column(modifier = Modifier.width(130.dp)) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ChatATM is thinking...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Beautiful render parser for custom Markdown structures (headings, bold, lists, and monospaced code blocks).
 */
@Composable
fun MarkdownTextRenderer(text: String, textColor: Color) {
    // Split lines to detect code blocks, headers, bullet lists, etc.
    val lines = remember(text) { text.split("\n") }
    
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()

    Column {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Output complete code block
                    CodeBlockItem(codeBlockBuilder.toString())
                    codeBlockBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockBuilder.append(line).append("\n")
                continue
            }

            // Parse headers
            if (trimmed.startsWith("#")) {
                val depth = trimmed.takeWhile { it == '#' }.length
                val headerText = trimmed.drop(depth).trim()
                val fontSize = when(depth) {
                    1 -> 20.sp
                    2 -> 18.sp
                    else -> 16.sp
                }
                Text(
                    text = headerText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            // Parse bullet points
            else if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) {
                val bulletText = trimmed.drop(2)
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(end = 8.dp))
                    Text(text = bulletText, fontSize = 14.sp, color = textColor)
                }
            }
            // Standard body paragraph
            else {
                if (line.isNotEmpty()) {
                    Text(
                        text = line,
                        fontSize = 14.sp,
                        color = textColor,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Fallback for unclosed code block
        if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
            CodeBlockItem(codeBlockBuilder.toString())
        }
    }
}

@Composable
fun CodeBlockItem(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2E)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code Block",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF89DCEB),
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code.trim()))
                        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = code.trim(),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFCDD6F4),
                lineHeight = 16.sp
            )
        }
    }
}
