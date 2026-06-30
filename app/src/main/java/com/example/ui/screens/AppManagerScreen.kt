package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.db.AppAliasEntity
import com.example.ui.JarvisViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    viewModel: JarvisViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.appAliases.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Filter apps based on search
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.alias.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "APP MANAGER CORE",
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
                .background(JarvisBackground),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurface)
                    .border(1.dp, JarvisPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = JarvisPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search installed apps...",
                                color = MutedSlate.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MutedSlate,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }

            // Stats Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYNCHRONIZED APPLICATIONS",
                    color = JarvisSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${filteredApps.size} ACTIVE",
                    color = MutedSlate,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Installed apps scrolling list
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sir, no applications found in mainframe.",
                        color = MutedSlate,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppManagerRow(app = app, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppManagerRow(
    app: AppAliasEntity,
    viewModel: JarvisViewModel
) {
    var aliasText by remember(app.alias) { mutableStateOf(app.alias) }
    var isEditingAlias by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, JarvisSecondary.copy(alpha = if (app.isFavorite) 0.4f else 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Logo Placeholder icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisSecondary.copy(alpha = 0.1f))
                        .border(1.dp, JarvisSecondary.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        color = JarvisSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 8.sp,
                        color = MutedSlate,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Favorite star toggle
                IconButton(
                    onClick = { viewModel.toggleAppFavorite(app.packageName, app.isFavorite) }
                ) {
                    Icon(
                        imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (app.isFavorite) JarvisTertiary else MutedSlate.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Active status switch
                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = { viewModel.toggleAppEnabled(app.packageName, app.isEnabled) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisSecondary,
                        checkedTrackColor = JarvisSecondary.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            Divider(color = JarvisSecondary.copy(alpha = 0.1f), thickness = 1.dp)

            // Voice Alias interactive row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.SettingsVoice,
                        contentDescription = null,
                        tint = JarvisPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOICE ALIAS: ",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate
                    )

                    if (isEditingAlias) {
                        BasicTextField(
                            value = aliasText,
                            onValueChange = { aliasText = it },
                            textStyle = TextStyle(
                                color = JarvisPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, JarvisPrimary, RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        )
                    } else {
                        Text(
                            text = "\"$aliasText\"",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = JarvisPrimary,
                            modifier = Modifier.clickable { isEditingAlias = true }
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isEditingAlias) {
                            viewModel.updateAppAlias(app.packageName, aliasText)
                            isEditingAlias = false
                        } else {
                            isEditingAlias = true
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isEditingAlias) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = null,
                        tint = JarvisPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
