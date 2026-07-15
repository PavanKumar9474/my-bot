package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChatViewModel

data class WritingTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val field1Label: String,
    val field1Placeholder: String,
    val field2Label: String,
    val field2Placeholder: String,
    val promptBuilder: (String, String) -> String
)

@Composable
fun WritingToolsScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedTool by remember { mutableStateOf<WritingTool?>(null) }
    var field1Text by remember { mutableStateOf("") }
    var field2Text by remember { mutableStateOf("") }

    val toolsList = listOf(
        WritingTool(
            title = "AI Resume Builder",
            description = "Draft structured professional experience bullet points",
            icon = Icons.Default.Description,
            category = "Career",
            field1Label = "Your Role & Company",
            field1Placeholder = "Senior Android Developer at Google",
            field2Label = "Key Accomplishments",
            field2Placeholder = "Led team of 4 to optimize APK compile size by 40% with Gradle Kotlin DSL"
        ) { role, details ->
            "Help me write high-impact professional resume bullet points for a '$role' role. Here are the details of my accomplishments:\n\n$details"
        },
        WritingTool(
            title = "Cover Letter Writer",
            description = "Generate a personalized professional cover letter",
            icon = Icons.Default.Work,
            category = "Career",
            field1Label = "Target Job Title & Company",
            field1Placeholder = "Staff AI Engineer at OpenAI",
            field2Label = "Your Qualifications & Passion",
            field2Placeholder = "5 years developing LLM APIs, passionate about conversational UX design"
        ) { job, qual ->
            "Write a highly professional and compelling cover letter for the role of '$job'. Here are my key qualifications and why I am excited about the role:\n\n$qual"
        },
        WritingTool(
            title = "Email Writer",
            description = "Draft perfect work, business, or casual emails",
            icon = Icons.Default.Email,
            category = "Productivity",
            field1Label = "Email Subject or Purpose",
            field1Placeholder = "Requesting extension on project release deadline",
            field2Label = "Key Points to include",
            field2Placeholder = "Encountered emulator bugs, need 3 extra days for testing, assure high quality"
        ) { subject, points ->
            "Draft a professional corporate email. Subject: '$subject'. Ensure the tone is respectful and clear, addressing the following key points:\n\n$points"
        },
        WritingTool(
            title = "Code Debugging & Optimization",
            description = "Find bug errors and optimize logic in any language",
            icon = Icons.Default.Code,
            category = "Development",
            field1Label = "Programming Language",
            field1Placeholder = "Kotlin, Java, Python, TypeScript",
            field2Label = "Code snippet with bug",
            field2Placeholder = "paste code here..."
        ) { lang, code ->
            "I have an issue in my $lang code. Please debug the following snippet, explain the cause of the bug, and provide a clean, highly optimized version of the code:\n\n```$lang\n$code\n```"
        },
        WritingTool(
            title = "SQL Generator",
            description = "Convert natural language instructions to SQL queries",
            icon = Icons.Default.Storage,
            category = "Development",
            field1Label = "Database Dialect",
            field1Placeholder = "PostgreSQL, SQLite, MySQL",
            field2Label = "Describe what query should do",
            field2Placeholder = "Get all users who registered in last 30 days and spent more than $100"
        ) { db, request ->
            "Generate a highly efficient $db SQL query based on this description. Write standard code, and explain how the indexes should be aligned:\n\n$request"
        },
        WritingTool(
            title = "Story & Content Generator",
            description = "Spark ideas and draft creative writing or blogs",
            icon = Icons.Default.AutoAwesome,
            category = "Creative",
            field1Label = "Topic / Headline",
            field1Placeholder = "A world where AI chatbots became self-aware through ATMs",
            field2Label = "Style or Tone",
            field2Placeholder = "Suspenseful sci-fi thriller, detailed, evocative prose"
        ) { topic, tone ->
            "Write a creative piece on the topic '$topic'. Use a '$tone' style. Establish strong characters and clear world-building."
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (selectedTool == null) {
            // Main Tools Grid View
            Text(
                text = "AI Coding & Writing Tools",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Select a specialized template below to draft content with ChatATM",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(toolsList) { tool ->
                    Card(
                        onClick = {
                            selectedTool = tool
                            field1Text = ""
                            field2Text = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )

                            Column {
                                Text(
                                    text = tool.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tool.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Form Screen for the Selected Tool
            val tool = selectedTool!!

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { selectedTool = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tool.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = tool.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Input Field 1
                Text(
                    text = tool.field1Label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = field1Text,
                    onValueChange = { field1Text = it },
                    placeholder = { Text(tool.field1Placeholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Input Field 2
                Text(
                    text = tool.field2Label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = field2Text,
                    onValueChange = { field2Text = it },
                    placeholder = { Text(tool.field2Placeholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        if (field1Text.trim().isEmpty() || field2Text.trim().isEmpty()) {
                            Toast.makeText(context, "Please complete both fields first!", Toast.LENGTH_SHORT).show()
                        } else {
                            val compiledPrompt = tool.promptBuilder(field1Text, field2Text)
                            viewModel.sendPreloadedPrompt(compiledPrompt)
                            viewModel.activeTab = "chat" // Redirect back to chat tab
                            selectedTool = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Draft with ChatATM")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Draft with ChatATM")
                }
            }
        }
    }
}
