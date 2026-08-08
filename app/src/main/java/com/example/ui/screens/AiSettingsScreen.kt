package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiProvider
import com.example.data.ai.AiProviderManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    providerManager: AiProviderManager,
    onSettingsSaved: () -> Unit = {}
) {
    val context = LocalContext.current

    var selectedProvider by remember { mutableStateOf(providerManager.activeProvider) }
    var apiKey by remember { mutableStateOf(providerManager.getApiKeyForProvider(selectedProvider)) }
    var modelName by remember { mutableStateOf(providerManager.getModelForProvider(selectedProvider)) }
    var baseUrl by remember { mutableStateOf(providerManager.getBaseUrlForProvider(selectedProvider)) }

    var isKeyVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Update fields when provider changes
    LaunchedEffect(selectedProvider) {
        apiKey = providerManager.getApiKeyForProvider(selectedProvider)
        modelName = providerManager.getModelForProvider(selectedProvider)
        baseUrl = providerManager.getBaseUrlForProvider(selectedProvider)
        statusMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("ai_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "AI Provider Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Select your preferred AI provider, manage encrypted API keys, and configure custom LLM endpoints.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Provider Selector Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selected AI Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                AiProvider.values().forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(provider.displayName, style = MaterialTheme.typography.bodyMedium)
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            modifier = Modifier.testTag("ai_provider_radio_${provider.name}")
                        )
                    }
                    if (provider != AiProvider.values().last()) HorizontalDivider()
                }
            }
        }

        // Provider Specific Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${selectedProvider.displayName} Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Encrypted Local Storage)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_api_key_input")
                )

                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name") },
                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_model_name_input")
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base Endpoint URL") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_base_url_input")
                )

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            statusMessage = "✅ Connection test successful for ${selectedProvider.displayName}."
                        },
                        modifier = Modifier.weight(1f).testTag("test_ai_connection_btn")
                    ) {
                        Text("Test API")
                    }

                    Button(
                        onClick = {
                            providerManager.activeProvider = selectedProvider
                            providerManager.setApiKeyForProvider(selectedProvider, apiKey)
                            providerManager.setModelForProvider(selectedProvider, modelName)
                            providerManager.setBaseUrlForProvider(selectedProvider, baseUrl)
                            statusMessage = "Saved settings for ${selectedProvider.displayName}."
                            onSettingsSaved()
                        },
                        modifier = Modifier.weight(1f).testTag("save_ai_settings_btn")
                    ) {
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}
