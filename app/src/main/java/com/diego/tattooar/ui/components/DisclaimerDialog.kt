package com.diego.tattooar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable

@Composable
fun DisclaimerDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Force explicit user confirmation */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Disclaimer",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Preview Disclaimer",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "TattooPreview provides visual 2D and AR overlays for preview and planning purposes only.\n\n" +
                        "Actual tattoo appearance may vary based on skin elasticity, individual skin tone, precise placement, lighting conditions, and tattoo artist execution.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "I Understand")
            }
        }
    )
}
