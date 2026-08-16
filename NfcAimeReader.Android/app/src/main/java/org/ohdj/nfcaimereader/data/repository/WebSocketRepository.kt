package org.ohdj.nfcaimereader.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ohdj.nfcaimereader.data.datastore.WebSocketPreferences
import org.ohdj.nfcaimereader.data.websocket.WebSocketClient
import org.ohdj.nfcaimereader.model.WebSocketServerInfo
import org.ohdj.nfcaimereader.utils.NetworkScanner
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketRepository @Inject constructor(
    private val preferences: WebSocketPreferences,
    private val webSocketClient: WebSocketClient,
    private val networkScanner: NetworkScanner
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val connectionState = webSocketClient.connectionState

    init {
        repositoryScope.launch {
            preferences.getRetryConnect().collectLatest { enabled ->
                if (!enabled) return@collectLatest

                while (currentCoroutineContext().isActive) {
                    val state = connectionState.value
                    if (!state.isConnected && !state.isConnecting) {
                        preferences.getLastServerInfo().first()?.let(webSocketClient::connect)
                    }
                    delay(2_000)
                }
            }
        }
    }

    suspend fun connectToServer(serverInfo: WebSocketServerInfo) {
        webSocketClient.connect(serverInfo)
        preferences.saveServerInfo(serverInfo)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun getLastServerInfo(): Flow<WebSocketServerInfo?> {
        return preferences.getLastServerInfo()
    }

    fun getSavedServers(): Flow<List<WebSocketServerInfo>> {
        return preferences.getSavedServers()
    }

    suspend fun saveServerInfo(serverInfo: WebSocketServerInfo) {
        preferences.saveServerInfo(serverInfo)
    }

    suspend fun deleteServer(serverId: String) {
        preferences.deleteServer(serverId)
    }

    suspend fun setAutoConnect(autoConnect: Boolean) {
        preferences.setAutoConnect(autoConnect)
    }

    fun getRetryConnect(): Flow<Boolean> = preferences.getRetryConnect()

    suspend fun setRetryConnect(enabled: Boolean) {
        preferences.setRetryConnect(enabled)
    }

    suspend fun scanNetwork(port: Int): List<WebSocketServerInfo> {
        return networkScanner.scanForWebSocketServers(port)
    }

    fun sendCardId(hexCardId: String): Boolean {
        return webSocketClient.sendCardId(hexCardId)
    }
}
