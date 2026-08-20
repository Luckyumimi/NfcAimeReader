package org.ohdj.nfcaimereader.ui.screen.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.os.Build
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import org.ohdj.nfcaimereader.ThemeMode
import org.ohdj.nfcaimereader.data.datastore.FelicaPreferenceViewModel
import org.ohdj.nfcaimereader.data.datastore.UserPreferenceViewModel
import org.ohdj.nfcaimereader.ui.screen.setting.component.SettingSwitchItem
import org.ohdj.nfcaimereader.ui.screen.setting.component.SettingThemeItem
import org.ohdj.nfcaimereader.ui.viewmodel.ConnectionSettingsViewModel

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val supportsDynamicTheming: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
)

data class felicaState(
    val felicaCompatibilityMode: Boolean = true,
)

@Composable
fun SettingScreen(
    userPrefViewModel: UserPreferenceViewModel,
    felicaViewModel: FelicaPreferenceViewModel,
    connectionSettingsViewModel: ConnectionSettingsViewModel = hiltViewModel()
) {
    val uiState by userPrefViewModel.settingsUiState.collectAsState(
        initial = SettingsUiState()
    )
    val retryConnectEnabled by connectionSettingsViewModel.retryConnectEnabled.collectAsState()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingScreenContent(
            uiState = uiState,
            onThemeSelected = { userPrefViewModel.updateThemeMode(it) },
            onDynamicColorChanged = { userPrefViewModel.updateDynamicColorEnabled(it) }
        )
        FelicaSettingSection(viewModel = felicaViewModel)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "连接",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingSwitchItem(
                title = "反复尝试连接",
                description = "在未连接状态每 2 秒尝试连接一次",
                checked = retryConnectEnabled,
                onCheckedChange = connectionSettingsViewModel::setRetryConnect
            )
        }
        DonationSection()
    }
}

private const val DonationAddress = "TF3TK5jT6dBVqY3JTpGJaVmzhBzryq8DhN"

@Composable
private fun DonationSection() {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "捐赠支持",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UDST（TRON）",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = DonationAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { copyDonationAddress(context) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "复制捐赠地址"
                )
            }
        }
    }
}

private fun copyDonationAddress(context: Context) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("UDST（TRON）", DonationAddress))
    Toast.makeText(context, "捐赠地址已复制", Toast.LENGTH_SHORT).show()
}

@Composable
fun SettingScreenContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "主题",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingSwitchItem(
            title = "动态取色",
            description = "使用系统提供的配色方案",
            checked = uiState.dynamicColorEnabled,
            enabled = uiState.supportsDynamicTheming,
            errorMessage = if (!uiState.supportsDynamicTheming) "此功能需要 Android 12+" else null,
            onCheckedChange = onDynamicColorChanged
        )
        SettingThemeItem(
            currentTheme = uiState.themeMode,
            onThemeSelected = onThemeSelected
        )
    }
}

// Android 12+
@Preview(name = "Android 12+", showBackground = true)
@Composable
fun SettingScreenPreviewAboveAndroid12() {
    SettingScreenContent(
        uiState = SettingsUiState(
            themeMode = ThemeMode.SYSTEM,
            dynamicColorEnabled = false,
            supportsDynamicTheming = true
        ),
        onThemeSelected = {},
        onDynamicColorChanged = {}
    )
}

// Android 12-
@Preview(name = "Android 12-", showBackground = true)
@Composable
fun SettingScreenPreviewBelowAndroid12() {
    SettingScreenContent(
        uiState = SettingsUiState(
            themeMode = ThemeMode.SYSTEM,
            dynamicColorEnabled = false,
            supportsDynamicTheming = false
        ),
        onThemeSelected = {},
        onDynamicColorChanged = {}
    )
}
