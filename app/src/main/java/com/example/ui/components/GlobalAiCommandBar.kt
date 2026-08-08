package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun GlobalAiCommandBar(
    onExecuteCommand: (String) -> Unit,
    onStartVoiceInput: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var commandText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    val presetCommands = listOf(
        "Add ₹250 grocery",
        "Show this month's expenses",
        "Create ₹5000 food budget",
        "Remind me to pay rent",
        "Export June report",
        "Show biggest expenses",
        "Search pizza expenses"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("global_ai_command_bar"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                OutlinedTextField(
                    value = commandText,
                    onValueChange = {
                        commandText = it
                        if (it.isNotEmpty()) isExpanded = true
                    },
                    placeholder = {
                        Text(
                            "Ask Notes AI or type 'Add ₹250 grocery'...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commandText.isNotBlank()) {
                            onExecuteCommand(commandText)
                            commandText = ""
                            isExpanded = false
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_command_input")
                )

                IconButton(
                    onClick = onStartVoiceInput,
                    modifier = Modifier.testTag("ai_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        if (commandText.isNotBlank()) {
                            onExecuteCommand(commandText)
                            commandText = ""
                            isExpanded = false
                        }
                    },
                    enabled = commandText.isNotBlank(),
                    modifier = Modifier.testTag("ai_command_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Command",
                        tint = if (commandText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            // Quick Preset Command Chips
            AnimatedVisibility(visible = isExpanded || commandText.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCommands.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                onExecuteCommand(preset)
                                commandText = ""
                            },
                            label = {
                                Text(
                                    preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("preset_chip_${preset.take(8)}")
                        )
                    }
                }
            }
        }
    }
}
