package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BatchInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Batch Song Search",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter or paste song names separated by newlines or commas. Each track will be matched automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            "Blinding Lights - The Weeknd\nShape of You - Ed Sheeran\nAs It Was - Harry Styles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_input_text_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val pasteText = clipData.getItemAt(0).text?.toString() ?: ""
                                if (pasteText.isNotBlank()) {
                                    text = if (text.isBlank()) pasteText else "$text\n$pasteText"
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("paste_clipboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.width(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Clipboard", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("batch_submit_button")
            ) {
                Text("Search All")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("batch_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
