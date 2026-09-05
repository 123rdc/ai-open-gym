package com.example.gymformcoach.features.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.BodyWeightRepository
import com.example.gymformcoach.core.data.BodyWeightSource
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import kotlinx.coroutines.launch

/**
 * §3.1: optional body-weight prompt at session start, pre-filled with the last
 * logged value. Skipping is a first-class action — equal visual weight, not a
 * grudging "no thanks" link.
 */
@Composable
fun BodyWeightPrompt(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BodyWeightRepository(AppDatabase.getInstance(context)) }
    var weightText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.getLatest()?.let { weightText = it.weightKg.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log body weight?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Optional — you can skip this and start straight away.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = weightText.toFloatOrNull()
                    if (weight != null) {
                        scope.launch {
                            repository.record(weight, source = BodyWeightSource.SESSION_PROMPT)
                            onDismiss()
                        }
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Save", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
