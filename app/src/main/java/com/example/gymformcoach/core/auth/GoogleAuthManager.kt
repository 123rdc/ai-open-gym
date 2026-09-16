package com.example.gymformcoach.core.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom

/**
 * Wraps Credential Manager's "Sign in with Google" button flow:
 * https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
 *
 * There's no backend here to verify the ID token against, so this only reads the
 * already-parsed profile fields off GoogleIdTokenCredential (name/email/photo) for
 * display - it's a profile-info fetch, not a security boundary.
 */
object GoogleAuthManager {

    private const val TAG = "GoogleAuthManager"

    // OAuth "Web application" client ID. The same Google Cloud project must also have an
    // Android client for com.example.gymformcoach + this build's signing SHA-1, or sign-in
    // fails with "[16] Account reauth failed".
    private const val WEB_CLIENT_ID = "639862476437-ph1vcssquc6qt36d063up67qs5qhi6kk.apps.googleusercontent.com"

    sealed class Result {
        data class Success(val displayName: String, val email: String, val photoUrl: String?) : Result()
        object Cancelled : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun signIn(context: Context): Result {
        val activity = context.findActivity()
            ?: return Result.Error("Sign-in needs an activity context")

        val option = GetSignInWithGoogleOption.Builder(serverClientId = WEB_CLIENT_ID)
            .setNonce(generateNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            // Per Google's guidance, use a MutableContextWrapper around the foreground
            // Activity so Credential Manager can update it across configuration changes
            // without leaking the original Activity.
            val mutableContext = android.content.MutableContextWrapper(activity)
            val response = CredentialManager.create(context).getCredential(
                request = request,
                context = mutableContext
            )
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                // This googleid version only exposes the email via the deprecated `id`
                // getter (newer releases add a dedicated `email`/`uniqueId` split, but
                // those require androidx.credentials 1.6.0+, which needs compileSdk 35).
                val email = googleCredential.id
                Result.Success(
                    displayName = googleCredential.displayName?.takeIf { it.isNotBlank() } ?: email,
                    email = email,
                    photoUrl = googleCredential.profilePictureUri?.toString()
                )
            } else {
                Result.Error("Unexpected credential type from Credential Manager")
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Invalid Google ID token response", e)
            Result.Error("Couldn't read the Google account response")
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Sign-in cancelled: ${e.type} ${e.message}")
            // Play Services reports a misconfigured OAuth client as a "[16] Account reauth
            // failed" cancellation after the account is picked, not as a real error.
            when {
                WEB_CLIENT_ID.startsWith("YOUR_") ->
                    Result.Error("Google sign-in isn't configured yet (missing OAuth Web client ID).")
                e.message?.contains("[16]") == true ->
                    Result.Error("Google rejected this app's sign-in setup. Check the OAuth client IDs and SHA-1 in Google Cloud.")
                else -> Result.Cancelled
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in failed: ${e.type} ${e.message}", e)
            Result.Error(
                if (WEB_CLIENT_ID.startsWith("YOUR_")) {
                    "Google sign-in isn't configured yet (missing OAuth Web client ID)."
                } else {
                    e.message ?: "Google sign-in failed"
                }
            )
        }
    }

    /** Clears any Credential Manager-held session so the next sign-in offers full account choice. */
    suspend fun signOut(context: Context) {
        try {
            CredentialManager.create(context)
                .clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Best-effort: local profile data is cleared by the caller regardless.
        }
    }

    private fun generateNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
