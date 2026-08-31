package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberSuccess
import com.example.ui.theme.CoralError
import com.example.ui.viewmodel.OtaUpdateStatus

@Composable
fun OtaUpdateDialog(
    status: OtaUpdateStatus,
    onDismiss: () -> Unit
) {
    if (status is OtaUpdateStatus.Idle) return

    AlertDialog(
        onDismissRequest = {
            if (status !is OtaUpdateStatus.Updating) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "yt-dlp Core Updater",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ota_update_dialog_content")
            ) {
                when (status) {
                    is OtaUpdateStatus.Updating -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is OtaUpdateStatus.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AmberSuccess,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is OtaUpdateStatus.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = CoralError,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = status.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralError
                            )
                        }
                    }
                    is OtaUpdateStatus.Idle -> Unit
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            if (status !is OtaUpdateStatus.Updating) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("ota_dismiss_button")
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(16.dp)
    )
}
