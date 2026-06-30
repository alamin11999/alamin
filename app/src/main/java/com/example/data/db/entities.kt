package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fact: String,
    val category: String = "general", // "profile", "preference", "general"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "jarvis"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_aliases")
data class AppAliasEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val alias: String,
    val isFavorite: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
