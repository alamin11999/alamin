package com.example.ui.screens

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.db.ConversationEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisCoreWidget
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JarvisViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val conversations by viewModel.conversationHistory.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val isBangla by viewModel.isBangla.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showLogsDrawer by remember { mutableStateOf(false) }

    // Fetch live battery
    val batteryPct = remember {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            88
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground),
        topBar = {
            // Immersive HUD Header matching the design exactly
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisBackground)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Profile Greeting with Gradient Ring Avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(JarvisPrimary.copy(alpha = 0.05f))
                                .border(1.dp, JarvisPrimary.copy(alpha = 0.3f), CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(JarvisPrimary, JarvisSecondary)
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AUTHORIZED USER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedSlate,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = userName.uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Right Side Telemetry System Status & active model
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(JarvisTertiary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYSTEM ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTertiary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MODEL: ${activeModel.uppercase()}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal,
                            color = MutedSlate,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Diagnostic Telemetry Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurface)
                        .border(1.dp, JarvisSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryItem(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "POWER CORE",
                        value = "$batteryPct%",
                        color = JarvisSecondary
                    )
                    TelemetryItem(
                        icon = Icons.Default.NetworkWifi,
                        label = "AI CONTEXT",
                        value = activeProvider.uppercase(),
                        color = JarvisPrimary
                    )
                    TelemetryItem(
                        icon = Icons.Default.Translate,
                        label = "LANGUAGE",
                        value = if (isBangla) "BANGLA" else "ENGLISH",
                        color = JarvisPrimary,
                        onClick = { viewModel.updateLanguage(!isBangla) }
                    )
                }
            }
        },
        containerColor = JarvisBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Hologram HUD Core Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // JARVIS AI Holographic rotating center widget
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    JarvisCoreWidget(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        rmsDb = rmsDb,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // AI Status subtitle indicator text and waveform visualizer (Immersive UI: Symmetrical waveform, pulsing subtitles)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    val statusText = when {
                        isListening -> "I'M LISTENING, SIR..."
                        isSpeaking -> "JARVIS SPEECH BROADCAST ACTIVE"
                        else -> "AWAITING SYSTEM INSTRUCTION..."
                    }
                    val statusColor = if (isListening || isSpeaking) JarvisPrimary else MutedSlate

                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.animateContentSize()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Immersive waveform
                    SymmetricalWaveform(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        rmsDb = rmsDb
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Panel Container (Controls and Chat overlays)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Floating scrollable Terminal logs list
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "CENTRAL INTELLIGENCE LOGS",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisPrimary
                                )
                                Text(
                                    "REALTIME DECODE",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MutedSlate
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = JarvisPrimary.copy(alpha = 0.1f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            val listState = rememberLazyListState()
                            LaunchedEffect(conversations.size) {
                                if (conversations.isNotEmpty()) {
                                    listState.animateScrollToItem(conversations.size - 1)
                                }
                            }

                            if (conversations.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No tactical transmission. Tap mic below to initiate voice link, Sir.",
                                        color = MutedSlate.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(conversations) { msg ->
                                        LogMessageRow(msg, onSpeakClick = { text ->
                                            viewModel.speak(text)
                                        })
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Command Manual Input Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(30.dp))
                            .background(JarvisSurface)
                            .border(1.dp, JarvisSecondary.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = JarvisSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.sendTextMessage(textInput)
                                        textInput = ""
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (textInput.isEmpty()) {
                                    Text(
                                        "Broadcast manual command...",
                                        color = MutedSlate.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendTextMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = JarvisPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Immersive Bottom Voice Controls & Action Bar matching the design HTML
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left squared action button: navigates to settings
                        IconButton(
                            onClick = { navController.navigate("settings") },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(JarvisSurface.copy(alpha = 0.5f))
                                .border(1.dp, JarvisPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = JarvisPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Wide glowing Voice Activation central key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            if (isListening) JarvisPrimary else JarvisPrimary.copy(alpha = 0.2f),
                                            if (isListening) JarvisSecondary else JarvisSecondary.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isListening) JarvisPrimary else JarvisPrimary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.toggleListening() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isListening) "JARVIS ACTIVATED" else "ENGAGE VOICE CORE",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }

                        // Right squared action button: navigates to memory logs
                        IconButton(
                            onClick = { navController.navigate("memory_settings") },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(JarvisSurface.copy(alpha = 0.5f))
                                .border(1.dp, JarvisPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Memory Logs",
                                tint = JarvisPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MutedSlate
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
fun SymmetricalWaveform(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    
    // 7 symmetrical bars representing the listening/speaking waveform
    val baseHeights = listOf(6.dp, 12.dp, 20.dp, 32.dp, 20.dp, 12.dp, 6.dp)
    
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(vertical = 4.dp)
    ) {
        baseHeights.forEachIndexed { index, baseHeight ->
            val activeMultiplier by if (isListening || isSpeaking) {
                val duration = 250 + (index * 80)
                infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "waveform_bar_$index"
                )
            } else {
                remember { mutableStateOf(0.2f) }
            }
            
            // Central bars are responsive to RMS DB in real-time
            val rmsScale = if (isListening && (index in 2..4)) {
                1f + (rmsDb.coerceIn(0f, 12f) / 12f) * 1.5f
            } else {
                1f
            }
            
            val barHeight = baseHeight * activeMultiplier * rmsScale
            val opacity = when (index) {
                0, 6 -> 0.4f
                1, 5 -> 0.6f
                2, 4 -> 0.8f
                else -> 1.0f
            }
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.5.dp)
                    .width(4.dp)
                    .height(barHeight.coerceIn(4.dp, 36.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                JarvisPrimary.copy(alpha = opacity),
                                JarvisSecondary.copy(alpha = opacity * 0.5f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun LogMessageRow(msg: ConversationEntity, onSpeakClick: ((String) -> Unit)? = null) {
    val isUser = msg.role == "user"
    val promptPrefix = if (isUser) ">> USER: " else ">> JARVIS: "
    val prefixColor = if (isUser) JarvisPrimary else JarvisSecondary
    val contentColor = if (isUser) Color.White else IceBlueText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = promptPrefix,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = prefixColor
        )
        Text(
            text = msg.message,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        if (!isUser && onSpeakClick != null) {
            IconButton(
                onClick = { onSpeakClick(msg.message) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speak",
                    tint = JarvisSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun QuickOptionButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isActive) JarvisPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
            .border(
                width = 1.dp,
                color = if (isActive) JarvisPrimary else MutedSlate.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isActive) JarvisPrimary else MutedSlate
        )
    }
}

@Composable
fun MicrophonePulsingButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_glow")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val neonGlow = JarvisPrimary

    Box(
        modifier = Modifier
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background rings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            neonGlow.copy(alpha = if (isListening) 0.4f else 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = neonGlow.copy(alpha = if (isListening) 0.7f else 0.2f),
                    shape = CircleShape
                )
        )

        // Center primary physical button
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isListening) neonGlow else JarvisSurfaceElevated)
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Voice Assistant Activator",
                tint = if (isListening) Color.Black else neonGlow,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
