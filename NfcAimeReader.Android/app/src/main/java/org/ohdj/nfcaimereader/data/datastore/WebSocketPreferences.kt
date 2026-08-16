package org.ohdj.nfcaimereader.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.ohdj.nfcaimereader.model.WebSocketServerInfo
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "websocket_preferences")

@Singleton
class WebSocketPreferences @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val LAST_SERVER_IP = stringPreferencesKey("last_server_ip")
        val LAST_SERVER_PORT = intPreferencesKey("last_server_port")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val RETRY_CONNECT = booleanPreferencesKey("retry_connect")
        val SAVED_SERVERS = stringPreferencesKey("saved_servers")
    }

    suspend fun saveServerInfo(serverInfo: WebSocketServerInfo) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SERVER_IP] = serverInfo.ip
            preferences[LAST_SERVER_PORT] = serverInfo.port
            // 启动自动连接状态与最近使用的服务器一起保存。
            // getLastServerInfo() 从该独立字段读取开关状态。
            preferences[AUTO_CONNECT] = serverInfo.isAutoConnect

            // 更新保存的服务器列表
            val savedServersJson = preferences[SAVED_SERVERS] ?: "[]"
            val savedServers = try {
                json.decodeFromString<List<WebSocketServerInfo>>(savedServersJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // 检查是否已存在相同 IP 和端口的服务器
            val existingIndex = savedServers.indexOfFirst {
                it.ip == serverInfo.ip && it.port == serverInfo.port
            }

            if (existingIndex >= 0) {
                savedServers[existingIndex] = serverInfo
            } else {
                savedServers.add(serverInfo)
            }

            preferences[SAVED_SERVERS] = json.encodeToString(savedServers)
        }
    }

    suspend fun setAutoConnect(autoConnect: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CONNECT] = autoConnect
        }
    }

    suspend fun setRetryConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RETRY_CONNECT] = enabled
        }
    }

    fun getRetryConnect(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[RETRY_CONNECT] ?: false
        }
    }

    fun getLastServerInfo(): Flow<WebSocketServerInfo?> {
        return context.dataStore.data.map { preferences ->
            val ip = preferences[LAST_SERVER_IP]
            val port = preferences[LAST_SERVER_PORT]

            if (ip != null && port != null) {
                WebSocketServerInfo(
                    ip = ip,
                    port = port,
                    isAutoConnect = preferences[AUTO_CONNECT] ?: false
                )
            } else {
                null
            }
        }
    }

    fun getSavedServers(): Flow<List<WebSocketServerInfo>> {
        return context.dataStore.data.map { preferences ->
            val savedServersJson = preferences[SAVED_SERVERS] ?: "[]"
            try {
                json.decodeFromString<List<WebSocketServerInfo>>(savedServersJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun deleteServer(serverId: String) {
        context.dataStore.edit { preferences ->
            val savedServersJson = preferences[SAVED_SERVERS] ?: "[]"
            val savedServers = try {
                json.decodeFromString<List<WebSocketServerInfo>>(savedServersJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            savedServers.removeIf { it.id == serverId }
            preferences[SAVED_SERVERS] = json.encodeToString(savedServers)
        }
    }
}
