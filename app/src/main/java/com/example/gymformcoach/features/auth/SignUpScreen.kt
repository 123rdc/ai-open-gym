package com.example.gymformcoach.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.example.gymformcoach.core.auth.GoogleAuthManager
import com.example.gymformcoach.core.designsystem.Background
import com.example.gymformcoach.core.designsystem.Error
import com.example.gymformcoach.core.designsystem.Primary
import com.example.gymformcoach.core.designsystem.components.GoogleSignInButton
import com.example.gymformcoach.core.designsystem.components.PrimaryButton
import com.example.gymformcoach.core.utils.PreferenceManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
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
                    onSignUpSuccess()
                }
                is GoogleAuthManager.Result.Cancelled -> Unit
                is GoogleAuthManager.Result.Error -> googleError = result.message
            }
            isGoogleLoading = false
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
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = "Join us to start your fitness journey",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name") },
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email or Mobile Number") },
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
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "By continuing, you agree to Terms of Use and Privacy Policy.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Sign Up",
            onClick = onSignUpSuccess,
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
            Text(text = "Already have an account?", color = Color.White.copy(alpha = 0.7f))
            TextButton(onClick = onBack) {
                Text(text = "Log in", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SocialIcon(icon: ImageVector, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = Color.White,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Black)
        }
    }
}
