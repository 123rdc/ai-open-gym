package com.example.gymformcoach

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.gymformcoach.core.navigation.NavGraph
import com.example.gymformcoach.core.designsystem.GymFormCoachTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.gymformcoach.core.utils.PreferenceManager
import com.example.gymformcoach.core.utils.BiometricHelper

class MainActivity : FragmentActivity() {
    private var isAuthSuccessful by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val preferenceManager = PreferenceManager(this)
        
        // If biometric is enabled and setup is complete, force auth on every start
        if (preferenceManager.isBiometricEnabled && preferenceManager.isSetupComplete) {
            authenticateUser()
        } else {
            isAuthSuccessful = true
        }

        setContent {
            GymFormCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAuthSuccessful) {
                        val navController = rememberNavController()
                        NavGraph(navController = navController)
                    } else {
                        // Show a loading/lock screen while authenticating
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    private fun authenticateUser() {
        if (BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.showBiometricPrompt(
                activity = this,
                onSuccess = { isAuthSuccessful = true },
                onError = { /* Keep locked or show retry button */ }
            )
        } else {
            isAuthSuccessful = true
        }
    }
}
