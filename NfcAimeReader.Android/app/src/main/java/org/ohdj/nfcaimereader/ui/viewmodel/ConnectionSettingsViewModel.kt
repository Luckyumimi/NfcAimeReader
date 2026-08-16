package org.ohdj.nfcaimereader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.ohdj.nfcaimereader.data.repository.WebSocketRepository
import javax.inject.Inject

@HiltViewModel
class ConnectionSettingsViewModel @Inject constructor(
    private val repository: WebSocketRepository
) : ViewModel() {
    val retryConnectEnabled: StateFlow<Boolean> = repository.getRetryConnect()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setRetryConnect(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRetryConnect(enabled)
        }
    }
}
