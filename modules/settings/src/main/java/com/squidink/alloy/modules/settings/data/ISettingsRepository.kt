package com.squidink.alloy.modules.settings.data

import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    fun getSettingss(): Flow<List<Any>>
    fun getSettingsById(id: String): Flow<Any?>
    suspend fun insertSettings(settings: Any)
    suspend fun updateSettings(settings: Any)
    suspend fun deleteSettings(id: String)
    suspend fun deleteAllSettingss()
}
