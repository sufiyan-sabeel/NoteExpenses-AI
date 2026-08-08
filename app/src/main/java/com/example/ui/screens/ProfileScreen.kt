package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.UserSession
import com.example.data.remote.FirebaseAuthManager
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userSession: UserSession,
    authManager: FirebaseAuthManager,
    onNavigateToAuth: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This action is permanent and will delete your Firebase user account. Enter password to reauthenticate:")
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reauth_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        coroutineScope.launch {
                            isDeleting = true
                            val reauthRes = authManager.reauthenticate(reauthPassword)
                            if (reauthRes.isSuccess) {
                                val deleteRes = authManager.deleteAccount()
                                if (deleteRes.isSuccess) {
                                    showDeleteDialog = false
                                    onNavigateToAuth()
                                } else {
                                    snackbarMsg = "Failed to delete account: ${deleteRes.exceptionOrNull()?.message}"
                                }
                            } else {
                                snackbarMsg = "Reauthentication failed. Check password."
                            }
                            isDeleting = false
                        }
                    },
                    enabled = !isDeleting && reauthPassword.isNotBlank(),
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Avatar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userSession.displayName.ifBlank { "Notes Expenses User" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = userSession.email.ifBlank { "Guest Account" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Verification Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (userSession.isEmailVerified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (userSession.isEmailVerified) Icons.Default.Verified else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (userSession.isEmailVerified) "Email Verified" else "Email Unverified",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!userSession.isEmailVerified && userSession.isAuthenticated) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                authManager.sendEmailVerification()
                                snackbarMsg = "Verification email sent!"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Resend Email Verification")
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                    HorizontalDivider()
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (userSession.email.isNotBlank()) {
                                authManager.sendForgotPassword(userSession.email)
                                snackbarMsg = "Password reset email sent to ${userSession.email}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Send Password Reset Email")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                HorizontalDivider()

                if (userSession.isAuthenticated) {
                    TextButton(
                        onClick = {
                            authManager.signOut()
                            onNavigateToAuth()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("logout_btn")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    HorizontalDivider()

                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("delete_account_btn")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delete Account", color = MaterialTheme.colorScheme.error)
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Button(
                        onClick = onNavigateToAuth,
                        modifier = Modifier.fillMaxWidth().testTag("signin_profile_btn")
                    ) {
                        Text("Sign In or Register Account")
                    }
                }
            }
        }

        if (!snackbarMsg.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = snackbarMsg ?: "", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
}
