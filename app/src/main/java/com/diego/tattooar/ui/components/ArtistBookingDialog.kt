package com.diego.tattooar.ui.components

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.diego.tattooar.data.model.Artist

@Composable
fun ArtistBookingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedArtist by remember { mutableStateOf<Artist?>(null) }
    var userEmail by remember { mutableStateOf("") }
    var appointmentNotes by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedArtist == null) "Book a Tattoo Artist" else "Request Consultation with ${selectedArtist?.name}",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            if (isSubmitted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Consultation Request Sent!",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The artist will review your tattoo preview and contact you via email at $userEmail within 24-48 hours.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (selectedArtist == null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(Artist.SAMPLE_ARTISTS) { artist ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArtist = artist }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.width(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = artist.rating.toString(),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                Text(
                                    text = artist.studioName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.width(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = artist.city,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artist.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            } else {
                Column {
                    Text(
                        text = "${selectedArtist?.studioName} • ${selectedArtist?.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Your Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = appointmentNotes,
                        onValueChange = { appointmentNotes = it },
                        label = { Text("Placement & Sizing Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            } else if (selectedArtist != null) {
                Button(
                    onClick = {
                        if (userEmail.isNotBlank()) {
                            isSubmitted = true
                        } else {
                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit Booking Request")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = {
                    if (selectedArtist != null) selectedArtist = null else onDismiss()
                }) {
                    Text(if (selectedArtist != null) "Back to Artists" else "Cancel")
                }
            }
        }
    )
}
