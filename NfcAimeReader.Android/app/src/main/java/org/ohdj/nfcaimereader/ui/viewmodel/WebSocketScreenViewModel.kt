package org.ohdj.nfcaimereader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.ohdj.nfcaimereader.data.repository.WebSocketRepository
import org.ohdj.nfcaimereader.model.ConnectionState
import org.ohdj.nfcaimereader.model.WebSocketServerInfo
import javax.inject.Inject

@HiltViewModel
class WebSocketScreenViewModel @Inject constructor(
    private val repository: WebSocketRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState()
        )

    val savedServers: StateFlow<List<WebSocketServerInfo>> = repository.getSavedServers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lastServerInfo: StateFlow<WebSocketServerInfo?> = repository.getLastServerInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableStateFlow<List<WebSocketServerInfo>>(emptyList())
    val scanResults: StateFlow<List<WebSocketServerInfo>> = _scanResults.asStateFlow()

    init {
        // 启动时只读取一次自动连接配置。持续 collect 会在连接保存服务器信息后
        // 再次触发 connect，形成断开、重连的反馈循环。
        viewModelScope.launch {
            val serverInfo = repository.getLastServerInfo().first()
            if (serverInfo != null && serverInfo.isAutoConnect) {
                repository.connectToServer(serverInfo)
            }
        }
    }

    suspend fun getLastServerInfo(): WebSocketServerInfo? {
        return repository.getLastServerInfo().stateIn(viewModelScope).value
    }

    fun connectToServer(serverInfo: WebSocketServerInfo) {
        viewModelScope.launch {
            repository.connectToServer(serverInfo)
        }
    }

    fun connectToSavedServer() {
        viewModelScope.launch {
            repository.getLastServerInfo().first()?.let { repository.connectToServer(it) }
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    suspend fun saveServerInfo(serverInfo: WebSocketServerInfo) {
        repository.saveServerInfo(serverInfo)
    }

    suspend fun deleteServer(serverId: String) {
        repository.deleteServer(serverId)
    }

    suspend fun scanNetwork(port: Int) {
        _isScanning.value = true
        _scanResults.value = emptyList()

        try {
            val results = repository.scanNetwork(port)
            _scanResults.value = results
        } finally {
            _isScanning.value = false
        }
    }
}
