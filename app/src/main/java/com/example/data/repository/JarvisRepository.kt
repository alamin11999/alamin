package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class JarvisRepository(private val db: JarvisDatabase) {
    private val memoryDao = db.memoryDao()
    private val conversationDao = db.conversationDao()
    private val appAliasDao = db.appAliasDao()
    private val settingDao = db.settingDao()

    // --- Memories ---
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesFlow()

    suspend fun addMemory(fact: String, category: String = "general") = withContext(Dispatchers.IO) {
        memoryDao.insertMemory(MemoryEntity(fact = fact, category = category))
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> = withContext(Dispatchers.IO) {
        memoryDao.searchMemories(query)
    }

    suspend fun deleteMemory(id: Int) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteMemoriesByCategory(category: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemoriesByCategory(category)
    }

    suspend fun clearAllMemories() = withContext(Dispatchers.IO) {
        memoryDao.clearAllMemories()
    }

    // --- Conversations ---
    val conversationHistory: Flow<List<ConversationEntity>> = conversationDao.getAllConversationsFlow()

    suspend fun addMessage(role: String, message: String) = withContext(Dispatchers.IO) {
        conversationDao.insertMessage(ConversationEntity(role = role, message = message))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        conversationDao.clearHistory()
    }

    // --- App Aliases ---
    val allAppAliases: Flow<List<AppAliasEntity>> = appAliasDao.getAllAliasesFlow()

    suspend fun getEnabledAliases(): List<AppAliasEntity> = withContext(Dispatchers.IO) {
        appAliasDao.getEnabledAliases()
    }

    suspend fun addAlias(alias: AppAliasEntity) = withContext(Dispatchers.IO) {
        appAliasDao.insertAlias(alias)
    }

    suspend fun addAliases(aliases: List<AppAliasEntity>) = withContext(Dispatchers.IO) {
        appAliasDao.insertAliases(aliases)
    }

    suspend fun updateAlias(packageName: String, alias: String) = withContext(Dispatchers.IO) {
        appAliasDao.updateAlias(packageName, alias)
    }

    suspend fun setAppFavorite(packageName: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        appAliasDao.setFavorite(packageName, isFavorite)
    }

    suspend fun setAppEnabled(packageName: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        appAliasDao.setEnabled(packageName, isEnabled)
    }

    suspend fun deleteAlias(packageName: String) = withContext(Dispatchers.IO) {
        appAliasDao.deleteAlias(packageName)
    }

    // --- Settings / Preferences ---
    suspend fun getSetting(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        settingDao.getSetting(key) ?: defaultValue
    }

    fun getSettingFlow(key: String, defaultValue: String): Flow<String?> {
        return settingDao.getSettingFlow(key)
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        settingDao.insertSetting(SettingEntity(key, value))
    }
}
