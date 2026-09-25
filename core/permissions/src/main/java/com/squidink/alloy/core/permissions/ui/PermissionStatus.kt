package com.squidink.alloy.core.permissions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.R
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionState
import com.squidink.alloy.core.permissions.PermissionUiState

/**
 * Visual indicator showing permission status.
 * Can be clicked to re-request permission or open settings.
 */
@Composable
fun PermissionStatusIndicator(
    permissionState: PermissionUiState,
    onReRequestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionInfo = when (permissionState.state) {
        is PermissionState.Granted -> {
            Triple(
                Icons.Filled.CheckCircle,
                stringResource(R.string.permission_status_granted),
                MaterialTheme.colorScheme.primary
            )
        }
        is PermissionState.Denied -> {
            Triple(
                Icons.Outlined.Warning,
                stringResource(R.string.permission_status_denied),
                MaterialTheme.colorScheme.error
            )
        }
        is PermissionState.PermanentlyDenied -> {
            Triple(
                Icons.Outlined.Lock,
                stringResource(R.string.permission_status_permanently_denied),
                MaterialTheme.colorScheme.error
            )
        }
        is PermissionState.Pending -> {
            Triple(
                Icons.Outlined.HourglassEmpty,
                stringResource(R.string.permission_status_pending),
                MaterialTheme.colorScheme.secondary
            )
        }
    }
    
    val (icon, text, color) = permissionInfo
    val description = when (permissionState.state) {
        is PermissionState.Granted -> stringResource(R.string.permission_status_granted_description, permissionState.permission.title)
        is PermissionState.Denied -> stringResource(R.string.permission_status_denied_description, permissionState.permission.title)
        is PermissionState.PermanentlyDenied -> stringResource(R.string.permission_status_permanently_denied_description, permissionState.permission.title)
        is PermissionState.Pending -> stringResource(R.string.permission_status_pending_description, permissionState.permission.title)
    }

    Row(
        modifier = modifier
            .clickable(onClick = onReRequestClick)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

/**
 * Compact permission status badge.
 */
@Composable
fun PermissionStatusBadge(
    permissionState: PermissionUiState,
    modifier: Modifier = Modifier
) {
    val permissionInfo = when (permissionState.state) {
        is PermissionState.Granted -> Pair(
            Icons.Filled.CheckCircle,
            stringResource(R.string.permission_status_granted_description, permissionState.permission.title)
        )
        is PermissionState.Denied -> Pair(
            Icons.Outlined.Warning,
            stringResource(R.string.permission_status_denied_description, permissionState.permission.title)
        )
        is PermissionState.PermanentlyDenied -> Pair(
            Icons.Outlined.Lock,
            stringResource(R.string.permission_status_permanently_denied_description, permissionState.permission.title)
        )
        is PermissionState.Pending -> Pair(
            Icons.Outlined.HourglassEmpty,
            stringResource(R.string.permission_status_pending_description, permissionState.permission.title)
        )
    }

    val (icon, description) = permissionInfo
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = when (permissionState.state) {
            is PermissionState.Granted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        },
        modifier = modifier.semantics { contentDescription = description }
    )
}
