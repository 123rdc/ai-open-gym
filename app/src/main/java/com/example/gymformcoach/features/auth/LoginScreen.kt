package com.example.gymformcoach.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymformcoach.core.auth.GoogleAuthManager
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.TextSecondary
import com.example.gymformcoach.core.designsystem.components.GoogleSignInButton
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.core.utils.BiometricHelper
import com.example.gymformcoach.core.utils.PreferenceManager
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val preferenceManager = remember { PreferenceManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    val onGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            isGoogleLoading = true
            googleError = null
            when (val result = GoogleAuthManager.signIn(context)) {
                is GoogleAuthManager.Result.Success -> {
                    preferenceManager.userDisplayName = result.displayName
                    preferenceManager.userEmail = result.email
                    preferenceManager.userPhotoUrl = result.photoUrl ?: ""
                    preferenceManager.isGoogleSignedIn = true
                    onLoginSuccess()
                }
                is GoogleAuthManager.Result.Cancelled -> Unit
                is GoogleAuthManager.Result.Error -> googleError = result.message
            }
            isGoogleLoading = false
        }
    }

    // Auto-show biometric if enabled
    LaunchedEffect(Unit) {
        if (preferenceManager.isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = { onLoginSuccess() },
                onError = { /* Silently fail or show error */ }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Sign in to continue your progress",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFB0A2F2).copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                .padding(24.dp)
        ) {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Username or email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                
                TextButton(
                    onClick = { /* Handle forgot password */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?", color = Color.Black.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        PrimaryButton(
            text = "Log In",
            onClick = onLoginSuccess,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton(
            loading = isGoogleLoading,
            onClick = onGoogleSignIn
        )
        googleError?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = Error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "or sign up with",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocialIcon(icon = Icons.Default.Person, onClick = onGoogleSignIn) // Google
            Spacer(modifier = Modifier.width(16.dp))
            SocialIcon(icon = Icons.Default.Person) // Facebook
            Spacer(modifier = Modifier.width(16.dp))
            SocialIcon(icon = Icons.Default.Fingerprint) // Biometric
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account?", color = TextSecondary)
            TextButton(onClick = onGoogleSignIn) {
                Text("Sign Up", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
