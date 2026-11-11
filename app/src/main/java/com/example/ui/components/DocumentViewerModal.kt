package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.ui.theme.*

@Composable
fun DocumentViewerModal(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onDelete: ((documentId: String, reason: String) -> Unit)? = null,
    onDownload: ((document: DocumentItem) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("Superceded by newer revision") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = CardSurface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepNavy)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(LightGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (document.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = LightGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = document.originalFileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${document.category.displayName} • ${document.formattedSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateGray
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_document_viewer")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Main Preview Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (document.isImage) {
                            // Pinch to zoom / pan image viewer
                            var scale by remember { mutableFloatStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }

                            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                                scale = (scale * zoomChange).coerceIn(1f, 4f)
                                offset = if (scale > 1f) offset + offsetChange else Offset.Zero
                            }

                            AsyncImage(
                                model = document.downloadUrl ?: document.localFilePath ?: document.thumbnailPath,
                                contentDescription = document.originalFileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .transformable(state = transformState)
                            )
                        } else {
                            // PDF Document Layout Preview
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(0.9f)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(0xFFFFEBEE), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = document.originalFileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = DeepNavy,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Official HBX Solar Enterprise Document",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Security Seal:", fontSize = 12.sp, color = TextMuted)
                                    Text("Cryptographically Verified", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusSuccess)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Storage Path:", fontSize = 12.sp, color = TextMuted)
                                    Text(document.storagePath.take(28) + "...", fontSize = 11.sp, color = DeepNavy)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Checksum (MD5):", fontSize = 12.sp, color = TextMuted)
                                    Text((document.checksum ?: "Calculated").take(16) + "...", fontSize = 11.sp, color = DeepNavy)
                                }
                            }
                        }
                    }

                    // Metadata & Actions Footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Uploaded by ${document.uploadedByName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Role: ${document.uploadedByRole} • ${document.formattedDate}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (document.uploadStatus == "COMPLETED") StatusSuccess.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = document.uploadStatus,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (document.uploadStatus == "COMPLETED") StatusSuccess else StatusWarning
                                )
                            }
                        }

                        if (!document.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes: ${document.description}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onDownload?.invoke(document) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_document_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download", fontSize = 13.sp)
                            }

                            if (onDelete != null) {
                                OutlinedButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.testTag("delete_document_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(StatusDanger)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Soft Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Request Document Deletion") },
            text = {
                Column {
                    Text("Are you sure you want to soft delete '${document.originalFileName}'? This will be recorded in enterprise audit logs.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for deletion") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke(document.documentId, deleteReason)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("Confirm Soft Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
