package com.example.gymformcoach.features.setup.fingerprint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ProfileRepository
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.core.utils.BiometricHelper
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.launch

@Composable
fun SetFingerprintScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val preferenceManager = remember { PreferenceManager(context) }
    val profileRepository = remember { ProfileRepository(AppDatabase.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()
    var message by remember { mutableStateOf("Secure your account using the system's biometric lock.") }

    val handleContinue = {
        coroutineScope.launch {
            profileRepository.syncFromPreferences(preferenceManager)
            preferenceManager.isSetupComplete = true
            onContinue()
        }
        Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Set Your Fingerprint",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Fingerprint Illustration Placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFFB0A2F2).copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color(0xFFB0A2F2)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = {
                    preferenceManager.isBiometricEnabled = false
                    handleContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Skip", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = "Continue",
                onClick = {
                    if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
                        BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            onSuccess = { 
                                preferenceManager.isBiometricEnabled = true
                                handleContinue()
                            },
                            onError = { err -> message = err }
                        )
                    } else {
                        handleContinue()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
