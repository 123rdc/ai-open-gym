package com.example.gymformcoach.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.analysis.AiCoachApiClient
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.Surface
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.utils.PreferenceManager
import com.example.gymformcoach.core.utils.WeightUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    var unit by remember { mutableStateOf(prefs.weightUnit) }
    var apiUrl by remember { mutableStateOf(prefs.aiCoachApiUrl) }
    var apiKey by remember { mutableStateOf(prefs.aiCoachApiKey) }
    var model by remember { mutableStateOf(prefs.aiCoachModel) }
    var showApiKey by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf<Boolean?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Text(text = "Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            UnitSegmentedControl(selected = unit, onSelect = {
                unit = it
                prefs.weightUnit = it
            })

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "AI Coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Point this at any OpenAI-compatible chat-completions endpoint for post-set analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value = apiUrl,
                onValueChange = {
                    apiUrl = it
                    prefs.aiCoachApiUrl = it
                    isConnected = null
                    connectionStatus = null
                },
                label = { Text("API endpoint URL") },
                placeholder = { Text("https://your-api.example.com/v1/chat/completions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    prefs.aiCoachApiKey = it
                },
                label = { Text("API key (optional)") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "Hide" else "Show", color = TextSecondary)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    prefs.aiCoachModel = it
                },
                label = { Text("Model (optional)") },
                placeholder = { Text("e.g. gpt-4o-mini") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    isTesting = true
                    connectionStatus = null
                    coroutineScope.launch {
                        when (val result = AiCoachApiClient.testConnection(apiUrl, apiKey, model)) {
                            is AiCoachApiClient.ConnectionResult.Connected -> {
                                isConnected = true
                                connectionStatus = "Connected"
                            }
                            is AiCoachApiClient.ConnectionResult.Unreachable -> {
                                isConnected = false
                                connectionStatus = result.message
                            }
                        }
                        isTesting = false
                    }
                },
                enabled = !isTesting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Text(if (isTesting) "Testing..." else "Test Connection")
            }
            connectionStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected == true) Primary else Error
                )
            }
        }
    }
}

@Composable
fun UnitSegmentedControl(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
    ) {
        listOf(WeightUnit.KG, WeightUnit.LBS).forEach { unit ->
            val isSelected = unit == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(unit) }
                    .background(if (isSelected) Primary else Color.Transparent, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unit.uppercase(),
                    color = if (isSelected) Background else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
