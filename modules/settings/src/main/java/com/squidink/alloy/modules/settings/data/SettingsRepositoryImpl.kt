package com.squidink.alloy.modules.settings.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
) : ISettingsRepository {
    
    override fun getSettingss(): Flow<List<Any>> {
        TODO("Implement this method")
    }
    
    override fun getSettingsById(id: String): Flow<Any?> {
        TODO("Implement this method")
    }
    
    override suspend fun insertSettings(settings: Any) {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
    
    override suspend fun updateSettings(settings: Any) {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
    
    override suspend fun deleteSettings(id: String) {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
    
    override suspend fun deleteAllSettingss() {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
}
