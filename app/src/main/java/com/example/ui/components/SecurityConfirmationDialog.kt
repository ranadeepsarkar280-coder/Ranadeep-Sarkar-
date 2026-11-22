package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.assistant.PendingSecurityConfirmation
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoidBlack

@Composable
fun SecurityConfirmationDialog(
    confirmation: PendingSecurityConfirmation,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDeny,
        containerColor = HudSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = "Security gate",
                tint = StatusWarning,
            )
        },
        title = {
            Text(
                text = "Boss, নিরাপত্তা অনুমোদন প্রয়োজন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MetallicSilver,
            )
        },
        text = {
            Column {
                Text(
                    text = "কাজ: ${confirmation.actionType.labelBn}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusWarning,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = confirmation.promptBn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetallicSilver,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusWarning,
                    contentColor = VoidBlack,
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("security_approve_button"),
            ) {
                Text("অনুমতি দিন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDeny,
                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.7f)),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("security_deny_button"),
            ) {
                Text("বাতিল করুন", color = StatusDanger)
            }
        },
    )
}
