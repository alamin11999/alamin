package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.JarvisViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelSettingsScreen(
    viewModel: JarvisViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val activeProvider by viewModel.activeProvider.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val testStatus by viewModel.testStatus.collectAsState()

    // Key states (loaded dynamically per selected provider)
    val providers = listOf("Gemini", "OpenAI", "DeepSeek", "Claude", "Custom")
    var selectedProviderIndex by remember { mutableStateOf(providers.indexOfFirst { it.lowercase() == activeProvider.lowercase() }.coerceAtLeast(0)) }
    val currentProviderTab = providers[selectedProviderIndex]

    var apiKeyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    // Custom API Fields
    var customUrlInput by remember { mutableStateOf("") }

    // Load keys when selected tab changes
    LaunchedEffect(currentProviderTab) {
        apiKeyInput = viewModel.getApiKey(currentProviderTab)
        if (currentProviderTab == "Custom") {
            customUrlInput = viewModel.getCustomUrl()
        }
    }

    // Available models catalog per provider
    val modelsCatalog = when (currentProviderTab) {
        "Gemini" -> listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-2.5-flash-native-audio-preview-12-2025")
        "OpenAI" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
        "DeepSeek" -> listOf("deepseek-chat", "deepseek-reasoner")
        "Claude" -> listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022")
        else -> listOf("default")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "AI PROVIDER SETTINGS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = JarvisBackground
                )
            )
        },
        containerColor = JarvisBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal Provider Tab Selector
            ScrollableTabRow(
                selectedTabIndex = selectedProviderIndex,
                containerColor = JarvisSurface,
                contentColor = JarvisPrimary,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, JarvisPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                providers.forEachIndexed { index, name ->
                    Tab(
                        selected = selectedProviderIndex == index,
                        onClick = { selectedProviderIndex = index },
                        text = {
                            Text(
                                text = name.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sub title
            Text(
                "CONFIGURATION PANEL",
                color = JarvisSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            // Dynamic Form based on selected provider
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "${currentProviderTab.uppercase()} INTEGRATION CORE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    // 1. Endpoint input for custom
                    if (currentProviderTab == "Custom") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "DESTINATION URL",
                                fontSize = 10.sp,
                                color = JarvisSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = {
                                    customUrlInput = it
                                    viewModel.saveCustomUrl(it)
                                },
                                placeholder = { Text("https://my-custom-api.com/v1/chat", color = MutedSlate.copy(alpha = 0.5f), fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = JarvisPrimary,
                                    unfocusedBorderColor = JarvisSecondary.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.key == Key.V && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed))
                                        ) {
                                            clipboardManager.getText()?.text?.let { text ->
                                                val trimmed = text.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    customUrlInput = trimmed
                                                    viewModel.saveCustomUrl(trimmed)
                                                }
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }
                    }

                    // 2. API Key input (except when it's Custom and might not need keys)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "API AUTHENTICATION KEY",
                            fontSize = 10.sp,
                            color = JarvisSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                viewModel.saveApiKey(currentProviderTab, it)
                            },
                            placeholder = { 
                                val hint = if (currentProviderTab == "Gemini") "Uses AI Studio default if blank..." else "Enter API Secret Key..."
                                Text(hint, color = MutedSlate.copy(alpha = 0.5f), fontSize = 12.sp) 
                            },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(imageVector = image, contentDescription = null, tint = JarvisPrimary)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisPrimary,
                                unfocusedBorderColor = JarvisSecondary.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.key == Key.V && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed))
                                    ) {
                                        clipboardManager.getText()?.text?.let { text ->
                                            val trimmed = text.trim()
                                            if (trimmed.isNotEmpty()) {
                                                apiKeyInput = trimmed
                                                viewModel.saveApiKey(currentProviderTab, trimmed)
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )

                        // Smart Paste shortcut helper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    clipboardManager.getText()?.text?.let { text ->
                                        val trimmed = text.trim()
                                        if (trimmed.isNotEmpty()) {
                                            apiKeyInput = trimmed
                                            viewModel.saveApiKey(currentProviderTab, trimmed)
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = JarvisPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PASTE FROM CLIPBOARD",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisPrimary
                                )
                            }
                        }
                    }

                    // 3. Model selector list
                    if (currentProviderTab != "Custom") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "INTELLIGENCE ENGINE MODEL",
                                fontSize = 10.sp,
                                color = JarvisSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            // Quick Model Chips Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                modelsCatalog.forEach { modelName ->
                                    val isSelected = activeModel == modelName && activeProvider.lowercase() == currentProviderTab.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) JarvisPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) JarvisPrimary else JarvisSecondary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                viewModel.updateAiProvider(currentProviderTab)
                                                viewModel.updateAiModel(modelName)
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = modelName.substringAfterLast("-"),
                                            color = if (isSelected) JarvisPrimary else MutedSlate,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Diagnostic Actions (Set Default, Test connection)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Set Active Button
                        val isCurrentlyActive = activeProvider.lowercase() == currentProviderTab.lowercase()
                        Button(
                            onClick = { viewModel.updateAiProvider(currentProviderTab) },
                            enabled = !isCurrentlyActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisPrimary,
                                contentColor = Color.Black,
                                disabledContainerColor = JarvisSecondary.copy(alpha = 0.1f),
                                disabledContentColor = JarvisSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isCurrentlyActive) "ACTIVE MAINFRAME ROUTE" else "ROUTE SYSTEM TO THIS PROVIDER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        // Test Connection Button
                        val status = testStatus[currentProviderTab] ?: "UNTESTED CONNECTION CORE"
                        val isOnline = status.contains("ONLINE")
                        val statusColor = if (isOnline) JarvisSecondary else if (status.contains("LINK")) NeonRed else JarvisPrimary

                        OutlinedButton(
                            onClick = { viewModel.testConnection(currentProviderTab) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = JarvisPrimary
                            ),
                            border = BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "TEST LINK PROTOCOL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Link Status Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.08f))
                                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = status.uppercase(),
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Tech hint block
            Text(
                "NOTE: For the default prototype path in AI Studio, you do not need to add any API keys for Gemini. JARVIS will automatically load the platform's secure key if left empty, Sir.",
                color = MutedSlate,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}
