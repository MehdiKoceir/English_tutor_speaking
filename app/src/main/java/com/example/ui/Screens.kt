package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.R
import com.example.data.ConversationSession
import com.example.data.VocabularyWord
import com.example.viewmodel.TutorViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.ExperimentalFoundationApi

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Chat : Screen("chat")
    object History : Screen("history")
    object Vocabulary : Screen("vocabulary")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(
    viewModel: TutorViewModel,
    onStartSpeechRecognizer: (onResult: (String) -> Unit) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }
    val currentSession by viewModel.currentSession.collectAsState()
    val isDark = isSystemInDarkTheme()

    var showPronunciationCoach by remember { mutableStateOf(false) }
    var targetPronunciationText by remember { mutableStateOf("") }

    val blob1Color = if (isDark) Color(0x2B8B5CF6) else Color(0x248B5CF6)
    val blob2Color = if (isDark) Color(0x244F46E5) else Color(0x1F4F46E5)
    val blob3Color = if (isDark) Color(0x1FE11D48) else Color(0x1AE11D48)

    if (showPronunciationCoach && targetPronunciationText.isNotEmpty()) {
        PronunciationCoachDialog(
            targetText = targetPronunciationText,
            onSpeak = onSpeak,
            onStartSpeechRecognizer = onStartSpeechRecognizer,
            onDismiss = {
                showPronunciationCoach = false
                targetPronunciationText = ""
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.Chat && currentScreen != Screen.Welcome) {
                NavigationBar(
                    modifier = Modifier
                        .testTag("navigation_bar")
                        .border(
                            1.dp,
                            if (isDark) Color(0x1AFFFFFF) else Color(0x2BFFFFFF),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 0.dp
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
                        indicatorColor = if (isDark) Color(0x268B5CF6) else Color(0xFFEDE9FE),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        label = { Text("Tutor") },
                        icon = { Icon(Icons.Default.School, contentDescription = "Tutor") },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.History,
                        onClick = { currentScreen = Screen.History },
                        label = { Text("History") },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_history")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Vocabulary,
                        onClick = { currentScreen = Screen.Vocabulary },
                        label = { Text("Vocabulary") },
                        icon = { Icon(Icons.Default.AutoStories, contentDescription = "Vocabulary") },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_vocabulary")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        label = { Text("Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        containerColor = Color.Transparent // Allow background draw to fill entire screen under system bars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .drawBehind {
                    // Blob 1: Top-Right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(blob1Color, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.width * 0.7f
                        )
                    )
                    // Blob 2: Bottom-Left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(blob2Color, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.85f),
                            radius = size.width * 0.8f
                        )
                    )
                    // Blob 3: Middle-Right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(blob3Color, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.5f),
                            radius = size.width * 0.5f
                        )
                    )
                }
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Welcome -> WelcomeScreen(
                    onEnterApp = {
                        currentScreen = Screen.Home
                    }
                )
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onStartChat = {
                        currentScreen = Screen.Chat
                    }
                )
                Screen.Chat -> {
                    if (currentSession != null) {
                        ChatScreen(
                            viewModel = viewModel,
                            onBack = {
                                onStopSpeaking()
                                currentScreen = Screen.Home
                            },
                            onStartSpeechRecognizer = onStartSpeechRecognizer,
                            onSpeak = onSpeak,
                            onPracticePronunciation = { text ->
                                targetPronunciationText = text
                                showPronunciationCoach = true
                            }
                        )
                    } else {
                        currentScreen = Screen.Home
                    }
                }
                Screen.History -> HistoryScreen(
                    viewModel = viewModel,
                    onOpenSession = { session ->
                        viewModel.loadSessionById(session.id)
                        currentScreen = Screen.Chat
                    },
                    onSpeak = onSpeak,
                    onPracticePronunciation = { text ->
                        targetPronunciationText = text
                        showPronunciationCoach = true
                    }
                )
                Screen.Vocabulary -> VocabularyScreen(
                    viewModel = viewModel,
                    onPracticePronunciation = { text ->
                        targetPronunciationText = text
                        showPronunciationCoach = true
                    }
                )
                Screen.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// 0. WELCOME / ONBOARDING SCREEN
// ==========================================
@Composable
fun WelcomeScreen(
    onEnterApp: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Brand/Logo Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "AuraTalk Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "AuraTalk AI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Hero Banner Illustration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = 300.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0x12000000)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_onboarding_hero),
                    contentDescription = "Onboarding Illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Welcoming Title & Slogan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Unlock Your Voice with AI",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Accelerate your fluency with tailored real-time speech tutoring, precise pronunciation analytics, and customized flashcard study sets.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Key Feature Cards Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Core Features",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                WelcomeFeatureItem(
                    icon = Icons.Default.Chat,
                    iconTint = Color(0xFF8B5CF6),
                    title = "AI Speech Conversations",
                    description = "Converse naturally with intelligent native voice tutors. Get real-time grammatical corrections."
                )

                WelcomeFeatureItem(
                    icon = Icons.Default.Mic,
                    iconTint = Color(0xFF10B981),
                    title = "Interactive Pronunciation Coach",
                    description = "Record your voice to receive instant syllable feedback and comparison against flawless TTS audio."
                )

                WelcomeFeatureItem(
                    icon = Icons.Default.AutoStories,
                    iconTint = Color(0xFF3B82F6),
                    title = "Personal Vocabulary Builder",
                    description = "Save any sentence or unfamiliar term during conversation instantly to your local database deck."
                )

                WelcomeFeatureItem(
                    icon = Icons.Default.EmojiEvents,
                    iconTint = Color(0xFFF59E0B),
                    title = "Challenge Quizzes & Streaks",
                    description = "Review your cards with quizzes, and lock in your daily active study streak values directly."
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Get Started Primary Action Button
            Button(
                onClick = onEnterApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .height(56.dp)
                    .testTag("get_started_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Forward arrow",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun WelcomeFeatureItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0x0FFFFFFF) else Color(0x08000000)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: TutorViewModel,
    onStartChat: () -> Unit
) {
    var selectedLevel by remember { mutableStateOf("Beginner") }
    var selectedTopic by remember { mutableStateOf("Free Talk") }
    val isDark = isSystemInDarkTheme()
    val currentSession by viewModel.currentSession.collectAsState()

    val levels = listOf("Beginner", "Intermediate", "Advanced")
    val topics = listOf(
        TopicItem("Free Talk", Icons.Default.Chat, "Practice natural, open conversation on any topic."),
        TopicItem("Job Interview", Icons.Default.Work, "Mock interview practice with typical professional questions."),
        TopicItem("Travel", Icons.Default.FlightTakeoff, "Practice ordering food, hotel booking, and asking directions."),
        TopicItem("Daily Life", Icons.Default.Home, "Talk about hobbies, routines, and daily activities."),
        TopicItem("Business", Icons.Default.BusinessCenter, "Formal discussions, networking, and presentation prep.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Custom Styled App Header (HTML replica)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "LingoTutor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AI POWERED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                // Account icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x66FFFFFF))
                        .border(
                            1.dp,
                            if (isDark) Color(0x15FFFFFF) else Color(0x99FFFFFF),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Hero Header Card (with beautiful gradient & online pulse)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Hello, Mohammed!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ready to practice your English speaking today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulse Online Badge
                    Row(
                        modifier = Modifier
                            .background(Color(0x26FFFFFF), RoundedCornerShape(100))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(100))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Text(
                            text = if (viewModel.useGeminiDirect.value) "GEMINI DIRECT READY" else "OLLAMA VPS ONLINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Section: Resume Active Session
        if (currentSession != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("resume_session_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0x1E8B5CF6) else Color(0xFFF5F3FF)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Active Session",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resume Conversation",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${currentSession!!.topic} • ${currentSession!!.level}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Button(
                                onClick = onStartChat,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("resume_active_session_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Resume", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Resume", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Daily Streak Tracker
        item {
            val dailyStreakRecord by viewModel.dailyStreak.collectAsState()
            val currentStreak = dailyStreakRecord?.currentStreak ?: 0
            val longestStreak = dailyStreakRecord?.longestStreak ?: 0
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val completedToday = dailyStreakRecord?.lastCompletedDate == today

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("daily_streak_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x12FFFFFF) else Color(0x06000000)
                ),
                border = BorderStroke(
                    1.dp,
                    if (completedToday) Color(0xFF10B981) else if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (completedToday) Color(0xFFD1FAE5) else if (currentStreak > 0) Color(0xFFFDE8E8) else Color(0x1F8B5CF6)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = "Streak Flame",
                                tint = if (completedToday) Color(0xFF10B981) else if (currentStreak > 0) Color(0xFFEF4444) else Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Daily Practice Streak",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (completedToday) "Today's goal completed! 🎉" else if (currentStreak > 0) "Keep practicing to keep your streak!" else "Complete your daily goal to start a streak!",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (completedToday) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$currentStreak Days",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = if (completedToday) Color(0xFF10B981) else if (currentStreak > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Best: $longestStreak d",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: Daily Practice Goal Tracker
        item {
            val practicedSeconds by viewModel.dailyPracticedSeconds.collectAsState()
            val goalMinutes by viewModel.dailyPracticeGoalMinutes.collectAsState()
            val practicedMinutes = practicedSeconds / 60
            val practicedSecPart = practicedSeconds % 60
            val progress = if (goalMinutes > 0) practicedSeconds.toFloat() / (goalMinutes * 60f) else 0f
            val percent = (progress * 100).toInt().coerceIn(0, 100)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("daily_goal_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x12FFFFFF) else Color(0x06000000)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Goal",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Daily Practice Goal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6)
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF8B5CF6),
                        trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x1F000000)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today: ${practicedMinutes}m ${practicedSecPart}s of ${goalMinutes}m target",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x15FFFFFF) else Color(0x12000000),
                        thickness = 1.dp
                    )

                    // Adjust Goal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adjust Daily Goal:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (goalMinutes > 5) viewModel.updatePracticeGoal(goalMinutes - 5) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isDark) Color(0x15FFFFFF) else Color(0x0F000000),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Goal",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "${goalMinutes} min",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("daily_goal_text")
                            )
                            IconButton(
                                onClick = { viewModel.updatePracticeGoal(goalMinutes + 5) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isDark) Color(0x15FFFFFF) else Color(0x0F000000),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Goal",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Select Level Dropdown
        item {
            var expanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Proficiency",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .testTag("level_dropdown_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0x1F000000) else Color(0x0F000000)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (expanded) Color(0xFF8B5CF6) else (if (isDark) Color(0x1AFFFFFF) else Color(0x12000000))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Dynamic Level Icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (selectedLevel) {
                                                "Beginner" -> Color(0xFF34D399).copy(alpha = 0.15f)
                                                "Intermediate" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                else -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (selectedLevel) {
                                            "Beginner" -> Icons.Default.StarBorder
                                            "Intermediate" -> Icons.Default.StarHalf
                                            else -> Icons.Default.Star
                                        },
                                        contentDescription = "Proficiency Icon",
                                        tint = when (selectedLevel) {
                                            "Beginner" -> Color(0xFF10B981)
                                            "Intermediate" -> Color(0xFF3B82F6)
                                            else -> Color(0xFF8B5CF6)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedLevel,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = when (selectedLevel) {
                                            "Beginner" -> "Simple words, slower conversations"
                                            "Intermediate" -> "Natural speed, everyday topics"
                                            else -> "Advanced idioms, complex ideas"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Dropdown",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // The actual DropdownMenu overlay
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        levels.forEach { level ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (level) {
                                                "Beginner" -> Icons.Default.StarBorder
                                                "Intermediate" -> Icons.Default.StarHalf
                                                else -> Icons.Default.Star
                                            },
                                            contentDescription = null,
                                            tint = if (selectedLevel == level) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = level,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (selectedLevel == level) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (selectedLevel == level) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = when (level) {
                                                    "Beginner" -> "A1-A2 level English"
                                                    "Intermediate" -> "B1-B2 level English"
                                                    else -> "C1-C2 level English"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedLevel = level
                                    expanded = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("level_chip_$level")
                            )
                        }
                    }
                }
            }
        }

        // Section: Topics Grid (Responsive 2x2 grid representation matching the HTML)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Daily Topics",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val chunkedTopics = topics.chunked(2)
                    chunkedTopics.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunk.forEach { topic ->
                                val isSelected = selectedTopic == topic.name
                                
                                // Define light/dark colors for the card backdrops and icons
                                val (iconBgColor, iconTint) = when (topic.name) {
                                    "Free Talk" -> Pair(
                                        if (isDark) Color(0x2BFF9800) else Color(0xFFFFF7ED),
                                        Color(0xFFEA580C)
                                    )
                                    "Travel" -> Pair(
                                        if (isDark) Color(0x2B2196F3) else Color(0xFFEFF6FF),
                                        Color(0xFF2563EB)
                                    )
                                    "Job Interview" -> Pair(
                                        if (isDark) Color(0x2B6366F1) else Color(0xFFEEF2FF),
                                        Color(0xFF4F46E5)
                                    )
                                    "Daily Life" -> Pair(
                                        if (isDark) Color(0x2BE91E63) else Color(0xFFFFF1F2),
                                        Color(0xFFE11D48)
                                    )
                                    "Business" -> Pair(
                                        if (isDark) Color(0x2B9C27B0) else Color(0xFFFAF5FF),
                                        Color(0xFF7C3AED)
                                    )
                                    else -> Pair(
                                        if (isDark) Color(0x2B8B5CF6) else Color(0xFFF5F3FF),
                                        Color(0xFF8B5CF6)
                                    )
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .clickable { selectedTopic = topic.name }
                                        .testTag("topic_card_${topic.name.replace(" ", "_")}"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            if (isDark) Color(0x4D8B5CF6) else Color(0xFFF5F3FF)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            Color(0xFF8B5CF6)
                                        } else {
                                            if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(iconBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = topic.icon,
                                                contentDescription = topic.name,
                                                tint = iconTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = if (topic.name == "Job Interview") "Job Prep" else topic.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = when (topic.name) {
                                                    "Free Talk" -> "Casual chat"
                                                    "Job Interview" -> "Interviews & career"
                                                    "Travel" -> "Airports & sightseeing"
                                                    "Daily Life" -> "Hobbies & food"
                                                    "Business" -> "Formal & networking"
                                                    else -> topic.description
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Fill remaining space if odd element
                            if (chunk.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Start Practice Button (Highly stylized gradient matching the design)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    viewModel.startNewSession(selectedLevel, selectedTopic)
                    onStartChat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5))
                        )
                    )
                    .testTag("start_chat_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Practice Session",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

data class TopicItem(
    val name: String,
    val icon: ImageVector,
    val description: String
)

// ==========================================
// 2. CHAT SCREEN (Active practice session)
// ==========================================
private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

private fun generateTranscriptText(
    session: com.example.data.ConversationSession?,
    messages: List<com.example.data.ChatMessage>,
    elapsedSeconds: Int
): String {
    val sb = StringBuilder()
    sb.append("=== LingoTutor Conversation Practice Transcript ===\n")
    if (session != null) {
        sb.append("Topic: ${session.topic}\n")
        sb.append("Proficiency Level: ${session.level}\n")
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault())
        sb.append("Date: ${sdf.format(java.util.Date(session.createdAt))}\n")
    }
    sb.append("Practice Duration: ${formatDuration(elapsedSeconds)}\n")
    sb.append("==================================================\n\n")

    val timeFormat = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
    messages.forEach { msg ->
        val senderLabel = if (msg.sender == "user") "User" else "Tutor"
        val timeStr = timeFormat.format(java.util.Date(msg.timestamp))
        sb.append("[$timeStr] $senderLabel: ${msg.text}\n")
        if (msg.sender == "user" && msg.correctedText != null) {
            sb.append("  * AI Grammar Feedback:\n")
            sb.append("    Correction: ${msg.correctedText}\n")
            if (msg.correctionsJson != null && msg.correctionsJson.isNotEmpty()) {
                sb.append("    Explanation: ${msg.correctionsJson}\n")
            }
            sb.append("\n")
        } else {
            sb.append("\n")
        }
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: TutorViewModel,
    onBack: () -> Unit,
    onStartSpeechRecognizer: (onResult: (String) -> Unit) -> Unit,
    onSpeak: (String) -> Unit,
    onPracticePronunciation: (String) -> Unit
) {
    val session by viewModel.currentSession.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isTutorThinking by viewModel.isTutorThinking.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val isDark = isSystemInDarkTheme()

    val context = androidx.compose.ui.platform.LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var showWordSelectDialog by remember { mutableStateOf(false) }
    var selectedMessageText by remember { mutableStateOf("") }

    var showChatOptionsDialog by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showEditMessageDialog by remember { mutableStateOf(false) }
    var editMessageText by remember { mutableStateOf("") }

    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(session?.id) {
        elapsedSeconds = 0
        while (true) {
            kotlinx.coroutines.delay(1000L)
            elapsedSeconds++
            viewModel.addPracticeSeconds(1)
        }
    }

    // Keep scrolling to the bottom when new messages arrive
    LaunchedEffect(messages.size, isTutorThinking) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session?.topic ?: "Conversation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Level: ${session?.level ?: "Beginner"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Timer",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = formatDuration(elapsedSeconds),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.testTag("conversation_timer")
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                      ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    
                    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
                    ) { uri ->
                        if (uri != null) {
                            try {
                                val transcript = generateTranscriptText(session, messages, elapsedSeconds)
                                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                    outputStream.write(transcript.toByteArray())
                                }
                                android.widget.Toast.makeText(context, "Transcript exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Failed to export transcript: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    // Export/Share transcript button
                    IconButton(
                        onClick = {
                            val defaultFileName = "LingoTutor_Transcript_${session?.topic?.replace(" ", "_") ?: "Conversation"}.txt"
                            createDocumentLauncher.launch(defaultFileName)
                        },
                        modifier = Modifier.testTag("export_transcript_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download transcript",
                            tint = Color(0xFF8B5CF6)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Quick Toggle for speech
                    IconButton(
                        onClick = {
                            viewModel.updateSettings(
                                viewModel.useGeminiDirect.value,
                                viewModel.ollamaUrl.value,
                                viewModel.ollamaApiKey.value,
                                viewModel.ollamaModel.value,
                                !ttsEnabled,
                                viewModel.correctionsEnabled.value
                            )
                        },
                        modifier = Modifier.testTag("tts_toggle")
                    ) {
                        Icon(
                            imageVector = if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle text to speech",
                            tint = if (ttsEnabled) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                modifier = Modifier.border(
                    1.dp,
                    if (isDark) Color(0x1AFFFFFF) else Color(0x1FFFFFFF),
                    RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat Message List
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onSpeak = onSpeak,
                        onLongClick = { msg ->
                            selectedMessage = msg
                            showChatOptionsDialog = true
                        },
                        onPracticePronunciation = onPracticePronunciation
                    )
                }

                if (isTutorThinking) {
                    item {
                        TutorThinkingIndicator()
                    }
                }
            }

            // Glassy Input Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isDark) Color(0x1AFFFFFF) else Color(0x33FFFFFF)
                        ),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Voice input microphone button with gorgeous backdrop tint
                    IconButton(
                        onClick = {
                            onStartSpeechRecognizer { transcribedText ->
                                textInput = transcribedText
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0x1A8B5CF6) else Color(0xFFEDE9FE)
                            )
                            .testTag("mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak using voice-to-text",
                            tint = Color(0xFF8B5CF6)
                        )
                    }

                    // Text Field input - custom styled and transparent
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input"),
                        placeholder = { Text("Type message in English...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = if (isDark) Color(0x2BFFFFFF) else Color(0x3D000000),
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.trim().isNotEmpty()) {
                                    viewModel.sendMessage(textInput, onSpeak)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )

                    // Send Button with premium purple/indigo linear gradient when active
                    val isEnabled = textInput.trim().isNotEmpty()
                    IconButton(
                        onClick = {
                            if (isEnabled) {
                                viewModel.sendMessage(textInput, onSpeak)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) {
                                    Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5)))
                                } else {
                                    androidx.compose.ui.graphics.SolidColor(if (isDark) Color(0x15FFFFFF) else Color(0x0F000000))
                                }
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    // Word selection modal on long-press
    if (showWordSelectDialog) {
        AlertDialog(
            onDismissRequest = { showWordSelectDialog = false },
            title = {
                Text(
                    "Save Vocabulary Word",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Tap on any word from this message to create a study card with an AI-generated definition:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Clean and extract unique English words
                    val wordsList = remember(selectedMessageText) {
                        selectedMessageText
                            .split(Regex("[\\s.,!?;:\"'()]+"))
                            .map { it.trim().lowercase(java.util.Locale.getDefault()) }
                            .filter { it.length > 2 && it.all { char -> char.isLetter() } }
                            .distinct()
                    }

                    var selectedWordInDialog by remember { mutableStateOf("") }
                    var wordDefinition by remember { mutableStateOf("") }
                    var isFetchingDef by remember { mutableStateOf(false) }

                    // Wrap layout or FlowRow for chips
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 75.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wordsList.size) { index ->
                                val word = wordsList[index]
                                val isChosen = selectedWordInDialog == word
                                FilterChip(
                                    selected = isChosen,
                                    onClick = {
                                        selectedWordInDialog = word
                                        isFetchingDef = true
                                        wordDefinition = "Fetching definition..."
                                        viewModel.fetchWordDefinition(word, selectedMessageText) { definition ->
                                            wordDefinition = definition
                                            isFetchingDef = false
                                        }
                                    },
                                    label = { Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF8B5CF6),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    if (selectedWordInDialog.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Text(
                            text = "Selected: \"$selectedWordInDialog\"",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6)
                        )

                        OutlinedTextField(
                            value = wordDefinition,
                            onValueChange = { wordDefinition = it },
                            label = { Text("Definition") },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_definition_input"),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            enabled = !isFetchingDef,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B5CF6)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showWordSelectDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedWordInDialog.isNotEmpty()) {
                                    viewModel.saveVocabularyWord(selectedWordInDialog, wordDefinition, selectedMessageText)
                                    showWordSelectDialog = false
                                    android.widget.Toast.makeText(context, "Saved to your Vocabulary!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = selectedWordInDialog.isNotEmpty() && !isFetchingDef && wordDefinition.isNotEmpty() && wordDefinition != "Fetching definition...",
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            modifier = Modifier.testTag("save_word_confirm_button")
                        ) {
                            Text("Save Flashcard")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Chat Options Modal on long-press
    if (showChatOptionsDialog && selectedMessage != null) {
        val msg = selectedMessage!!
        AlertDialog(
            onDismissRequest = { showChatOptionsDialog = false },
            title = {
                Text(
                    text = "Message Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose an action for this message:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Option 1: Save Word as Flashcard
                    ListItem(
                        headlineContent = { Text("Save Word as Flashcard") },
                        supportingContent = { Text("Select a word to add to your word bank") },
                        leadingContent = {
                            Icon(Icons.Default.School, contentDescription = "Flashcard", tint = Color(0xFF8B5CF6))
                        },
                        modifier = Modifier
                            .clickable {
                                selectedMessageText = msg.text
                                showChatOptionsDialog = false
                                showWordSelectDialog = true
                            }
                            .clip(RoundedCornerShape(12.dp))
                    )

                    // Option 2: Practice Pronunciation
                    ListItem(
                        headlineContent = { Text("Practice Pronunciation") },
                        supportingContent = { Text("Speak and compare with the TTS model") },
                        leadingContent = {
                            Icon(Icons.Default.Mic, contentDescription = "Practice", tint = Color(0xFF10B981))
                        },
                        modifier = Modifier
                            .clickable {
                                showChatOptionsDialog = false
                                onPracticePronunciation(msg.text)
                            }
                            .clip(RoundedCornerShape(12.dp))
                    )

                    // Option 3: Edit Message
                    ListItem(
                        headlineContent = { Text("Edit Message Text") },
                        supportingContent = { Text("Modify the text stored in your database") },
                        leadingContent = {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFF59E0B))
                        },
                        modifier = Modifier
                            .clickable {
                                editMessageText = msg.text
                                showChatOptionsDialog = false
                                showEditMessageDialog = true
                            }
                            .clip(RoundedCornerShape(12.dp))
                    )

                    // Option 4: Delete Message
                    ListItem(
                        headlineContent = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("Remove this message from the history", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) },
                        leadingContent = {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier
                            .clickable {
                                showChatOptionsDialog = false
                                viewModel.deleteMessage(msg.id)
                                android.widget.Toast.makeText(context, "Message deleted from database!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChatOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Message Dialog
    if (showEditMessageDialog && selectedMessage != null) {
        val msg = selectedMessage!!
        AlertDialog(
            onDismissRequest = { showEditMessageDialog = false },
            title = {
                Text(
                    text = "Edit Message Text",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Modify this message's text. This will update your conversation database entry directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editMessageText,
                        onValueChange = { editMessageText = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_message_input"),
                        label = { Text("Message Content") },
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editMessageText.trim().isNotEmpty()) {
                            viewModel.updateMessage(msg.id, editMessageText.trim(), msg.correctedText)
                            showEditMessageDialog = false
                            android.widget.Toast.makeText(context, "Message updated in database!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMessageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onSpeak: (String) -> Unit,
    onLongClick: (ChatMessage) -> Unit,
    onPracticePronunciation: (String) -> Unit
) {
    val isUser = message.sender == "user"
    val isDark = isSystemInDarkTheme()
    var showCorrectionsDetail by remember { mutableStateOf(false) }

    val scale = remember(message.id) { Animatable(0.9f) }
    val alpha = remember(message.id) { Animatable(0f) }
    val offsetY = remember(message.id) { Animatable(15f) }

    LaunchedEffect(message.id) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250)
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message_bubble" else "tutor_message_bubble")
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                translationY = offsetY.value
            },
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Tutor Icon",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Actual bubble container with premium gradient for user, glassy card for tutor
            Card(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isUser) 20.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 20.dp
                ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = if (isUser) null else BorderStroke(
                    1.dp,
                    if (isDark) Color(0x1AFFFFFF) else Color(0x80FFFFFF)
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = { if (!isUser) { onSpeak(message.text) } },
                        onLongClick = { onLongClick(message) }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isUser) {
                                Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5)))
                            } else {
                                Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(Color(0x1F2E2A3E), Color(0x1F211D2F))
                                    } else {
                                        listOf(Color(0xE6FFFFFF), Color(0xCCFFFFFF))
                                    }
                                )
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (!isUser) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onSpeak(message.text) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak Sentence",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Listen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                
                Row(
                    modifier = Modifier.clickable { 
                        onPracticePronunciation(message.text)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Practice Pronunciation",
                        tint = Color(0xFF10B981).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Practice",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981).copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Grammar correction display (friendly gold/amber AI suggestion tip)
        if (isUser && message.correctedText != null) {
            val amberBg = if (isDark) Color(0x33FFB300) else Color(0xFFFEF3C7)
            val amberBorder = if (isDark) Color(0x66FFB300) else Color(0xFFFDE68A)
            val amberText = if (isDark) Color(0xFFFFCA28) else Color(0xFF92400E)

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(amberBg)
                    .border(BorderStroke(1.dp, amberBorder), RoundedCornerShape(12.dp))
                    .clickable { showCorrectionsDetail = !showCorrectionsDetail }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Suggestion Star",
                    tint = amberText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Grammar feedback available",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = amberText
                )
            }

            AnimatedVisibility(visible = showCorrectionsDetail) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(top = 6.dp)
                        .border(BorderStroke(1.dp, amberBorder), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = amberBg)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Corrected sentence:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = amberText
                            )
                            Text(
                                text = message.correctedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFFFF3CD) else Color(0xFF78350F)
                            )
                        }
                        if (message.correctionsJson != null && message.correctionsJson.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "Explanation:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = amberText
                                )
                                Text(
                                    text = message.correctionsJson,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFFFF3CD).copy(alpha = 0.85f) else Color(0xFF78350F).copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Tutor Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.padding(4.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tutor is writing...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ==========================================
// 3. HISTORY SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TutorViewModel,
    onOpenSession: (ConversationSession) -> Unit,
    onSpeak: (String) -> Unit,
    onPracticePronunciation: (String) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState()
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val isDark = isSystemInDarkTheme()

    var sessionToEdit by remember { mutableStateOf<ConversationSession?>(null) }
    var editTopicText by remember { mutableStateOf("") }
    var selectedSessionForLog by remember { mutableStateOf<ConversationSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                modifier = Modifier.border(
                    1.dp,
                    if (isDark) Color(0x1AFFFFFF) else Color(0x1FFFFFFF),
                    RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = "No sessions",
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF8B5CF6).copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No history found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your conversation practice will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                items(sessions) { session ->
                    val iconBgColor = if (isDark) Color(0x2B8B5CF6) else Color(0xFFF5F3FF)
                    val iconTint = Color(0xFF8B5CF6)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSessionForLog = session }
                            .testTag("session_card_${session.id}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iconBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (session.topic.lowercase()) {
                                        "job interview", "job prep" -> Icons.Default.Work
                                        "travel" -> Icons.Default.FlightTakeoff
                                        "business" -> Icons.Default.BusinessCenter
                                        "daily life" -> Icons.Default.Home
                                        else -> Icons.Default.Chat
                                    },
                                    contentDescription = "Topic",
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.topic,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isDark) Color(0x268B5CF6) else Color(0xFFEDE9FE),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = session.level,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6)
                                            )
                                        )
                                    }
                                    Text(
                                        text = sdf.format(Date(session.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Edit Topic button
                                IconButton(
                                    onClick = {
                                        sessionToEdit = session
                                        editTopicText = session.topic
                                    },
                                    modifier = Modifier.testTag("edit_session_topic_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit topic",
                                        tint = Color(0xFFF59E0B)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.testTag("delete_session_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete conversation",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (sessionToEdit != null) {
        val s = sessionToEdit!!
        AlertDialog(
            onDismissRequest = { sessionToEdit = null },
            title = {
                Text(
                    text = "Rename Session Topic",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Change the title/topic for this conversation practice session:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editTopicText,
                        onValueChange = { editTopicText = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_session_topic_input"),
                        label = { Text("Topic Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTopicText.trim().isNotEmpty()) {
                            viewModel.updateSessionTopic(s.id, editTopicText.trim())
                            sessionToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Rename", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedSessionForLog != null) {
        val s = selectedSessionForLog!!
        ConversationHistoryLogViewer(
            session = s,
            viewModel = viewModel,
            onDismiss = { selectedSessionForLog = null },
            onSpeak = onSpeak,
            onPracticePronunciation = onPracticePronunciation,
            onOpenSession = { onOpenSession(s) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryLogViewer(
    session: ConversationSession,
    viewModel: TutorViewModel,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onPracticePronunciation: (String) -> Unit,
    onOpenSession: () -> Unit
) {
    val messages by viewModel.getMessagesForSession(session.id).collectAsState(initial = emptyList())
    val isDark = isSystemInDarkTheme()
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val context = androidx.compose.ui.platform.LocalContext.current

    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                val transcript = generateTranscriptText(session, messages, 0)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(transcript.toByteArray())
                }
                android.widget.Toast.makeText(context, "Transcript exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: java.lang.Exception) {
                android.widget.Toast.makeText(context, "Failed to export transcript: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = session.topic,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${session.level} • ${sdf.format(Date(session.createdAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("log_viewer_close_button")) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val defaultFileName = "LingoTutor_Transcript_${session.topic.replace(" ", "_")}.txt"
                                    createDocumentLauncher.launch(defaultFileName)
                                },
                                modifier = Modifier.testTag("log_viewer_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download transcript",
                                    tint = Color(0xFF8B5CF6)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = onOpenSession,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("log_viewer_resume_button")
                            ) {
                                Text("Resume", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                onSpeak = onSpeak,
                                onLongClick = {},
                                onPracticePronunciation = onPracticePronunciation
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SETTINGS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TutorViewModel
) {
    val useGeminiDirect by viewModel.useGeminiDirect.collectAsState()
    val url by viewModel.ollamaUrl.collectAsState()
    val apiKey by viewModel.ollamaApiKey.collectAsState()
    val model by viewModel.ollamaModel.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val correctionsEnabled by viewModel.correctionsEnabled.collectAsState()
    val isDark = isSystemInDarkTheme()

    var inputUrl by remember(url) { mutableStateOf(url) }
    var inputApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var inputModel by remember(model) { mutableStateOf(model) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                modifier = Modifier.border(
                    1.dp,
                    if (isDark) Color(0x1AFFFFFF) else Color(0x1FFFFFFF),
                    RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // General Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Text-to-speech toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(
                                        useGeminiDirect,
                                        inputUrl,
                                        inputApiKey,
                                        inputModel,
                                        !ttsEnabled,
                                        correctionsEnabled
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0x1A8B5CF6) else Color(0xFFF5F3FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Text-to-Speech (TTS)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Read tutor's message aloud", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            Switch(
                                checked = ttsEnabled,
                                onCheckedChange = {
                                    viewModel.updateSettings(useGeminiDirect, inputUrl, inputApiKey, inputModel, it, correctionsEnabled)
                                },
                                modifier = Modifier.testTag("setting_tts_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF8B5CF6)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Grammar correction toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(
                                        useGeminiDirect,
                                        inputUrl,
                                        inputApiKey,
                                        inputModel,
                                        ttsEnabled,
                                        !correctionsEnabled
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0x1A10B981) else Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Spellcheck, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Grammar Corrections", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Get instant correction suggestions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            Switch(
                                checked = correctionsEnabled,
                                onCheckedChange = {
                                    viewModel.updateSettings(useGeminiDirect, inputUrl, inputApiKey, inputModel, ttsEnabled, it)
                                },
                                modifier = Modifier.testTag("setting_corrections_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF8B5CF6)
                                )
                            )
                        }
                    }
                }
            }

            // Daily Streak Management Card
            item {
                val dailyStreak by viewModel.dailyStreak.collectAsState()
                val streakVal = dailyStreak?.currentStreak ?: 0
                val longestStreak = dailyStreak?.longestStreak ?: 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Daily Streak Manager",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Manage your database streak values directly. Handily adjust or reset your streaks if needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Streak", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text("$streakVal days", style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Longest Streak", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text("$longestStreak days", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetStreak() },
                                modifier = Modifier.weight(1f).testTag("reset_streak_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reset Streak", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.setStreak(streakVal + 1) },
                                modifier = Modifier.weight(1f).testTag("increment_streak_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                            ) {
                                Text("Add +1 Day", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Engine Selection Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Tutor Engine Service",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Gemini Direct Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(
                                        true,
                                        inputUrl,
                                        inputApiKey,
                                        inputModel,
                                        ttsEnabled,
                                        correctionsEnabled
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = useGeminiDirect,
                                    onClick = {
                                        viewModel.updateSettings(true, inputUrl, inputApiKey, inputModel, ttsEnabled, correctionsEnabled)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.testTag("engine_gemini_radio")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Gemini Direct API (Recommended)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("No setup needed. Runs immediately in-app.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Ollama VPS Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(
                                        false,
                                        inputUrl,
                                        inputApiKey,
                                        inputModel,
                                        ttsEnabled,
                                        correctionsEnabled
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = !useGeminiDirect,
                                    onClick = {
                                        viewModel.updateSettings(false, inputUrl, inputApiKey, inputModel, ttsEnabled, correctionsEnabled)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.testTag("engine_vps_radio")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Custom Ollama VPS Backend", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Connects to your self-hosted API", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Custom Server Configuration Fields (Shown if VPS selected)
            if (!useGeminiDirect) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x99FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "VPS Connection Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8B5CF6)
                            )

                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                label = { Text("Ollama Proxy URL") },
                                placeholder = { Text("e.g. http://123.45.67.89:11434") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vps_url_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    unfocusedBorderColor = if (isDark) Color(0x2BFFFFFF) else Color(0x3D000000)
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = inputApiKey,
                                onValueChange = { inputApiKey = it },
                                label = { Text("X-API-Key") },
                                placeholder = { Text("Leave blank if none") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vps_key_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    unfocusedBorderColor = if (isDark) Color(0x2BFFFFFF) else Color(0x3D000000)
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = inputModel,
                                onValueChange = { inputModel = it },
                                label = { Text("Model Name") },
                                placeholder = { Text("e.g. llama3.1:8b") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vps_model_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    unfocusedBorderColor = if (isDark) Color(0x2BFFFFFF) else Color(0x3D000000)
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    viewModel.updateSettings(
                                        false,
                                        inputUrl,
                                        inputApiKey,
                                        inputModel,
                                        ttsEnabled,
                                        correctionsEnabled
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5)))
                                    )
                                    .testTag("save_vps_settings_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save VPS Config", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // Help instructions for VPS setup
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, if (isDark) Color(0x10FFFFFF) else Color(0x40FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "VPS Host & Ollama Setup Helper",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Deploy the Python FastAPI server on your VPS.\n" +
                                        "2. Ensure Ollama is running and has the llama3.1:8b model (ollama run llama3.1:8b).\n" +
                                        "3. Secure your VPS endpoint with your custom API key (X-API-Key).\n" +
                                        "4. Enter the VPS Address and API Key in the fields above to practice entirely on your self-hosted LLM!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class QuizQuestion(
    val word: VocabularyWord,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val selectedOptionIndex: Int? = null,
    val isWordToDefinition: Boolean
)

fun generateQuizQuestions(allWords: List<VocabularyWord>): List<QuizQuestion> {
    if (allWords.size < 3) return emptyList()
    
    val quizWords = allWords.shuffled().take(5)
    return quizWords.map { word ->
        val isWordToDefinition = java.util.Random().nextBoolean()
        val correctOption: String
        val questionText: String
        val distractorOptions: List<String>
        
        if (isWordToDefinition) {
            questionText = "What is the definition of \"${word.word}\"?"
            correctOption = word.definition
            distractorOptions = allWords
                .filter { it.word != word.word }
                .shuffled()
                .take(3)
                .map { it.definition }
        } else {
            questionText = "Which word matches this definition:\n\"${word.definition}\"?"
            correctOption = word.word
            distractorOptions = allWords
                .filter { it.word != word.word }
                .shuffled()
                .take(3)
                .map { it.word }
        }
        
        val optionsList = (distractorOptions + correctOption).distinct().shuffled()
        val correctIndex = optionsList.indexOf(correctOption)
        
        QuizQuestion(
            word = word,
            questionText = questionText,
            options = optionsList,
            correctOptionIndex = correctIndex,
            isWordToDefinition = isWordToDefinition
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    viewModel: TutorViewModel,
    onPracticePronunciation: (String) -> Unit
) {
    val words by viewModel.allVocabularyWords.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Flashcards, 1 = Word Bank
    
    // Quick Quiz States
    var showQuizDialog by remember { mutableStateOf(false) }
    var showNotEnoughWordsDialog by remember { mutableStateOf(false) }
    var quizQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var quizScore by remember { mutableStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    // Edit Vocabulary States
    var showEditWordDialog by remember { mutableStateOf(false) }
    var wordToEdit by remember { mutableStateOf<VocabularyWord?>(null) }
    var editWordValue by remember { mutableStateOf("") }
    var editWordDefinition by remember { mutableStateOf("") }
    var editWordExampleSentence by remember { mutableStateOf("") }

    val reviewWords = words.filter { !it.isLearned }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header with Quick Quiz action
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Vocabulary Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Vocabulary Builder",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${words.size} words saved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color(0xFF8B5CF6)
                    )
                }
            }

            // Quick Quiz Button
            Button(
                onClick = {
                    if (words.size < 3) {
                        showNotEnoughWordsDialog = true
                    } else {
                        quizQuestions = generateQuizQuestions(words)
                        currentQuestionIndex = 0
                        selectedOptionIndex = null
                        quizScore = 0
                        quizFinished = false
                        showQuizDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("start_quick_quiz_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Play Quiz",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Quick Quiz", 
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Custom M3 Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF8B5CF6),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF8B5CF6)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Practice Flashcards", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_flashcards")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Word Bank", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_word_bank")
            )
        }

        if (selectedTab == 0) {
            // Flashcard study mode
            if (reviewWords.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x12FFFFFF) else Color(0x0A000000)),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Menu Book",
                                tint = Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "All Caught Up!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "You have reviewed all saved words, or haven't saved any words yet.\n\nLong-press on words or sentences in your chat practice to save them as flashcards!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                var currentCardIndex by remember { mutableStateOf(0) }
                // Coerce index safely
                val cardIndex = currentCardIndex.coerceIn(0, maxOf(0, reviewWords.size - 1))
                val activeWord = reviewWords[cardIndex]
                
                var isFlipped by remember { mutableStateOf(false) }
                
                // Reset flip state when card changes
                LaunchedEffect(cardIndex) {
                    isFlipped = false
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Card ${cardIndex + 1} of ${reviewWords.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Flashcard Card
                    Card(
                        onClick = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(280.dp)
                            .testTag("flashcard_item"),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(
                            2.dp,
                            Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5)))
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isDark) {
                                        Brush.verticalGradient(listOf(Color(0xFF1E1A2F), Color(0xFF151221)))
                                    } else {
                                        Brush.verticalGradient(listOf(Color(0xFFFBF9FF), Color(0xFFF3EFFF)))
                                    }
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isFlipped) {
                                // FRONT OF CARD
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = activeWord.word,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF8B5CF6),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.testTag("flashcard_front_text")
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flip,
                                            contentDescription = "Flip icon",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Tap to view definition",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            } else {
                                // BACK OF CARD
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = activeWord.word,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF8B5CF6),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    HorizontalDivider(color = Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                    
                                    Text(
                                        text = activeWord.definition,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.testTag("flashcard_back_definition")
                                    )
                                    
                                    if (activeWord.exampleSentence.isNotEmpty()) {
                                        Text(
                                            text = "\"${activeWord.exampleSentence}\"",
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.1f))
                                                .clickable { onPracticePronunciation(activeWord.exampleSentence) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Practice Pronunciation",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Practice",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateVocabularyWordStatus(activeWord.word, true)
                                if (reviewWords.size > 1) {
                                    if (cardIndex >= reviewWords.size - 1) {
                                        currentCardIndex = 0
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("mark_learned_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Learned", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I know it!", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (reviewWords.size > 1) {
                                    currentCardIndex = (cardIndex + 1) % reviewWords.size
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("next_card_button")
                        ) {
                            Text("Next Card", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        } else {
            // My Word Bank List Mode
            if (words.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved words in your database yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(words) { wordItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vocab_item_${wordItem.word}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0x12FFFFFF) else Color(0x06000000)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (wordItem.isLearned) Color(0x3310B981) else if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = wordItem.word,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (wordItem.isLearned) Color(0xFF10B981) else Color(0xFF8B5CF6)
                                        )
                                        if (wordItem.isLearned) {
                                            SuggestionChip(
                                                onClick = { viewModel.updateVocabularyWordStatus(wordItem.word, false) },
                                                label = { Text("Learned", style = MaterialTheme.typography.labelSmall) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                                                    labelColor = Color(0xFF10B981)
                                                ),
                                                border = null
                                            )
                                        } else {
                                            SuggestionChip(
                                                onClick = { viewModel.updateVocabularyWordStatus(wordItem.word, true) },
                                                label = { Text("Reviewing", style = MaterialTheme.typography.labelSmall) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                                                    labelColor = Color(0xFF8B5CF6)
                                                ),
                                                border = null
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = wordItem.definition,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (wordItem.exampleSentence.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "\"${wordItem.exampleSentence}\"",
                                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { onPracticePronunciation(wordItem.exampleSentence) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Practice",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Edit vocab word details
                                    IconButton(
                                        onClick = {
                                            wordToEdit = wordItem
                                            editWordValue = wordItem.word
                                            editWordDefinition = wordItem.definition
                                            editWordExampleSentence = wordItem.exampleSentence
                                            showEditWordDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_vocab_${wordItem.word}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Word",
                                            tint = Color(0xFFF59E0B)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteVocabularyWord(wordItem.word) },
                                        modifier = Modifier.testTag("delete_vocab_${wordItem.word}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Word",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
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

    // --- EDIT VOCABULARY WORD DIALOG ---
    if (showEditWordDialog && wordToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditWordDialog = false },
            title = {
                Text(
                    text = "Edit Vocabulary Card",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editWordValue,
                        onValueChange = { editWordValue = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_vocab_word_input"),
                        label = { Text("Word/Phrase") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    OutlinedTextField(
                        value = editWordDefinition,
                        onValueChange = { editWordDefinition = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_vocab_def_input"),
                        label = { Text("Definition") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    OutlinedTextField(
                        value = editWordExampleSentence,
                        onValueChange = { editWordExampleSentence = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_vocab_sentence_input"),
                        label = { Text("Example Sentence") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editWordValue.trim().isNotEmpty() && editWordDefinition.trim().isNotEmpty()) {
                            if (wordToEdit!!.word != editWordValue.trim().lowercase()) {
                                viewModel.deleteVocabularyWord(wordToEdit!!.word)
                            }
                            viewModel.saveVocabularyWord(
                                word = editWordValue.trim(),
                                definition = editWordDefinition.trim(),
                                exampleSentence = editWordExampleSentence.trim()
                            )
                            showEditWordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- QUICK QUIZ DIALOGS ---
    if (showNotEnoughWordsDialog) {
        AlertDialog(
            onDismissRequest = { showNotEnoughWordsDialog = false },
            title = {
                Text(
                    "Unlock Quick Quiz",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "You need to save at least 3 vocabulary words to start a Quick Quiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Currently, you have ${words.size} word(s) saved.\n\nTip: You can easily save new words by long-pressing on any message bubble in your chat sessions!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotEnoughWordsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Got it")
                }
            }
        )
    }

    if (showQuizDialog && quizQuestions.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { 
                // Do not dismiss on click outside
            },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            modifier = Modifier.testTag("quiz_dialog"),
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!quizFinished) {
                        val currentQuestion = quizQuestions[currentQuestionIndex]
                        
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Quick Quiz",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8B5CF6)
                            )
                            IconButton(
                                onClick = { showQuizDialog = false },
                                modifier = Modifier.testTag("quiz_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Quiz",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Progress
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Question ${currentQuestionIndex + 1} of ${quizQuestions.size}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Score: $quizScore",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1).toFloat() / quizQuestions.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF8B5CF6),
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }

                        // Question card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF231E39) else Color(0xFFF5F0FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentQuestion.questionText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 24.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Options List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentQuestion.options.forEachIndexed { idx, option ->
                                val isSelected = selectedOptionIndex == idx
                                val isCorrect = idx == currentQuestion.correctOptionIndex
                                val isAnswered = selectedOptionIndex != null
                                
                                val containerColor = when {
                                    isAnswered && isCorrect -> Color(0xFFD1FAE5)
                                    isAnswered && isSelected && !isCorrect -> Color(0xFFFEE2E2)
                                    isSelected -> Color(0xFFEDE9FE)
                                    else -> if (isDark) Color(0x0EFFFFFF) else Color(0x06000000)
                                }
                                
                                val contentColor = when {
                                    isAnswered && isCorrect -> Color(0xFF065F46)
                                    isAnswered && isSelected && !isCorrect -> Color(0xFF991B1B)
                                    isSelected -> Color(0xFF6D28D9)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                
                                val borderColor = when {
                                    isAnswered && isCorrect -> Color(0xFF10B981)
                                    isAnswered && isSelected && !isCorrect -> Color(0xFFEF4444)
                                    isSelected -> Color(0xFF8B5CF6)
                                    else -> Color.Transparent
                                }

                                Card(
                                    onClick = {
                                        if (selectedOptionIndex == null) {
                                            selectedOptionIndex = idx
                                            val updatedQuestion = currentQuestion.copy(selectedOptionIndex = idx)
                                            quizQuestions = quizQuestions.toMutableList().apply {
                                                this[currentQuestionIndex] = updatedQuestion
                                            }
                                            if (idx == currentQuestion.correctOptionIndex) {
                                                quizScore++
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = containerColor,
                                        contentColor = contentColor
                                    ),
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_option_$idx")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        if (isAnswered) {
                                            if (isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Correct",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = "Incorrect",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions (Next)
                        Button(
                            onClick = {
                                if (currentQuestionIndex < quizQuestions.size - 1) {
                                    currentQuestionIndex++
                                    selectedOptionIndex = null
                                } else {
                                    quizFinished = true
                                }
                            },
                            enabled = selectedOptionIndex != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("quiz_next_button")
                        ) {
                            Text(
                                text = if (currentQuestionIndex == quizQuestions.size - 1) "View Results" else "Next Question",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // QUIZ FINISHED RESULTS SCREEN
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Quiz Results",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF8B5CF6)
                                )
                                IconButton(
                                    onClick = { showQuizDialog = false },
                                    modifier = Modifier.testTag("quiz_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Quiz",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Score display
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$quizScore",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "out of ${quizQuestions.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            val scorePercentage = (quizScore.toFloat() / quizQuestions.size) * 100
                            val feedbackText = when {
                                scorePercentage >= 100f -> "Masterclass! Perfect score! 🌟"
                                scorePercentage >= 60f -> "Great job! You're making awesome progress! 🚀"
                                else -> "Keep studying! Practice makes perfect! 💪"
                            }

                            Text(
                                text = feedbackText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Brief Question Summary
                            Text(
                                text = "Question Summary",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                            ) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(quizQuestions.size) { idx ->
                                        val question = quizQuestions[idx]
                                        val userChoice = question.selectedOptionIndex
                                        val wasCorrect = userChoice == question.correctOptionIndex

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (wasCorrect) Color(0xFF10B981).copy(alpha = 0.06f)
                                                    else Color(0xFFEF4444).copy(alpha = 0.06f)
                                                )
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (wasCorrect) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = if (wasCorrect) "Correct" else "Incorrect",
                                                    tint = if (wasCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = question.word.word,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (wasCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                                                )
                                            }
                                            Text(
                                                text = if (wasCorrect) "Correct" else "Wrong",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (wasCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Footer Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showQuizDialog = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("quiz_close_button")
                                ) {
                                    Text("Close")
                                }
                                Button(
                                    onClick = {
                                        // Restart Quiz
                                        quizQuestions = generateQuizQuestions(words)
                                        currentQuestionIndex = 0
                                        selectedOptionIndex = null
                                        quizScore = 0
                                        quizFinished = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                        .testTag("quiz_restart_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restart",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Try Again", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

// ==========================================
// PRONUNCIATION COACH IMPLEMENTATION
// ==========================================

data class WordPronunciationResult(
    val word: String,
    val accuracy: Int // 2 = Perfect, 1 = Close, 0 = Incorrect/Missed
)

fun evaluatePronunciation(targetText: String, spokenText: String): Pair<Int, List<WordPronunciationResult>> {
    val cleanTarget = targetText.lowercase(java.util.Locale.US)
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        
    val cleanSpoken = spokenText.lowercase(java.util.Locale.US)
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }

    if (cleanTarget.isEmpty()) {
        return Pair(0, emptyList())
    }

    var correctMatches = 0
    val results = mutableListOf<WordPronunciationResult>()

    for (targetWord in cleanTarget) {
        var bestAccuracy = 0 // 0 = missed, 1 = close, 2 = perfect
        for (spokenWord in cleanSpoken) {
            if (targetWord == spokenWord) {
                bestAccuracy = 2
                break
            } else {
                val distance = levenshteinDistance(targetWord, spokenWord)
                val maxLen = maxOf(targetWord.length, spokenWord.length)
                if (maxLen > 0 && distance.toFloat() / maxLen.toFloat() <= 0.3f) {
                    bestAccuracy = maxOf(bestAccuracy, 1)
                }
            }
        }
        if (bestAccuracy == 2) {
            correctMatches += 2
        } else if (bestAccuracy == 1) {
            correctMatches += 1
        }
        results.add(WordPronunciationResult(targetWord, bestAccuracy))
    }

    val maxPoints = cleanTarget.size * 2
    val scorePercent = ((correctMatches.toFloat() / maxPoints.toFloat()) * 100).toInt().coerceIn(0, 100)

    return Pair(scorePercent, results)
}

fun levenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) {
        dp[i][0] = i
    }
    for (j in 0..s2.length) {
        dp[0][j] = j
    }
    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            if (s1[i - 1] == s2[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1]
            } else {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + 1
                )
            }
        }
    }
    return dp[s1.length][s2.length]
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PronunciationCoachDialog(
    targetText: String,
    onSpeak: (String) -> Unit,
    onStartSpeechRecognizer: (onResult: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var spokenText by remember { mutableStateOf("") }
    var score by remember { mutableStateOf<Int?>(null) }
    var wordResults by remember { mutableStateOf<List<WordPronunciationResult>>(emptyList()) }
    var isRecording by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(targetText) {
        onSpeak(targetText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pronunciation_coach_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Coach",
                        tint = Color(0xFF10B981)
                    )
                    Text(
                        text = "Pronunciation Coach",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Listen to the synthetic model, then record yourself to compare pronunciation accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF231E39) else Color(0xFFF5F0FF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Target Sentence:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6)
                        )

                        if (score != null && wordResults.isNotEmpty()) {
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                wordResults.forEach { result ->
                                    val wordColor = when (result.accuracy) {
                                        2 -> Color(0xFF10B981)
                                        1 -> Color(0xFFF59E0B)
                                        else -> Color(0xFFEF4444)
                                    }
                                    
                                    val wordBg = when (result.accuracy) {
                                        2 -> if (isDark) Color(0x3310B981) else Color(0xFFD1FAE5)
                                        1 -> if (isDark) Color(0x33F59E0B) else Color(0xFFFEF3C7)
                                        else -> if (isDark) Color(0x33EF4444) else Color(0xFFFEE2E2)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(wordBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = result.word,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = wordColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = targetText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clickable { onSpeak(targetText) }
                                .background(
                                    if (isDark) Color(0x338B5CF6) else Color(0xFFEDE9FE),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Play Model TTS",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Listen to Speech Model",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8B5CF6)
                            )
                        }
                    }
                }

                if (score != null) {
                    val scoreColor = when {
                        score!! >= 85 -> Color(0xFF10B981)
                        score!! >= 60 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }

                    val feedbackText = when {
                        score!! >= 90 -> "Excellent! Native-like pronunciation! 🌟"
                        score!! >= 75 -> "Great job! Very clear pronunciation! 🚀"
                        score!! >= 60 -> "Good attempt! Keep practicing! 👍"
                        else -> "Keep studying! Try listening to the synthetic model again! 💪"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = scoreColor.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(64.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { score!!.toFloat() / 100f },
                                    color = scoreColor,
                                    strokeWidth = 6.dp,
                                    modifier = Modifier.size(64.dp),
                                    trackColor = scoreColor.copy(alpha = 0.15f)
                                )
                                Text(
                                    text = "$score%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                    color = scoreColor
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accuracy Score",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scoreColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = feedbackText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You said:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (spokenText.isEmpty()) "\"(No speech detected)\"" else "\"$spokenText\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (isRecording) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x06000000))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Recording active... Speak now!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8B5CF6)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E1A2F) else Color(0xFFF3F0FA))
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val bars = 30
                        val barWidth = width / (bars * 1.5f)
                        val gap = barWidth * 0.5f
                        
                        val random = java.util.Random(targetText.hashCode().toLong())
                        for (i in 0 until bars) {
                            val activeVal = if (isRecording) {
                                (Math.sin(System.currentTimeMillis().toDouble() / 150.0 + i) + 1.0) / 2.0 * height * 0.8
                            } else if (score != null) {
                                random.nextFloat() * height * 0.7f * (score!!.toFloat() / 100f)
                            } else {
                                (random.nextFloat() * 4.dp.toPx()) + 2.dp.toPx()
                            }
                            
                            val barHeight = activeVal.toFloat().coerceIn(2.dp.toPx(), height * 0.9f)
                            val x = i * (barWidth + gap) + gap
                            val y = (height - barHeight) / 2f
                            
                            drawRoundRect(
                                color = if (isRecording) Color(0xFFEF4444) else if (score != null) Color(0xFF10B981) else Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                topLeft = androidx.compose.ui.geometry.Offset(x, y.toFloat()),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight.toFloat()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isRecording = true
                    onStartSpeechRecognizer { transcribed ->
                        isRecording = false
                        spokenText = transcribed
                        val evaluation = evaluatePronunciation(targetText, transcribed)
                        score = evaluation.first
                        wordResults = evaluation.second
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (score != null) Color(0xFF8B5CF6) else Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pronunciation_record_btn")
            ) {
                Icon(
                    imageVector = if (score != null) Icons.Default.Refresh else Icons.Default.Mic,
                    contentDescription = "Mic"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (score != null) "Re-record to Compare" else "Start Pronunciation Match",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {}
    )
}

