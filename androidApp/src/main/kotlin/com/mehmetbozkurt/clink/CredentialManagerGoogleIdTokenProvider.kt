package com.mehmetbozkurt.clink

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.mehmetbozkurt.clink.account.GoogleIdToken
import com.mehmetbozkurt.clink.account.GoogleIdTokenProvider

/**
 * Google sign-in on Android through Credential Manager: shows the account picker and returns the ID token for
 * Firebase. The server client id comes from google-services.json (the project's web OAuth client).
 */
class CredentialManagerGoogleIdTokenProvider(private val activity: Activity) : GoogleIdTokenProvider {

    override suspend fun request(): GoogleIdToken {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdToken.Ok(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleIdToken.Failed("Unexpected credential type: ${credential.type}")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleIdToken.Cancelled
        } catch (_: NoCredentialException) {
            GoogleIdToken.NoAccount
        } catch (e: GetCredentialException) {
            GoogleIdToken.Failed(e.message ?: e.type)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleIdToken.Failed(e.message ?: "Could not read the Google ID token")
        }
    }
}
