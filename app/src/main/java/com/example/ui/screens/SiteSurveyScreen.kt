package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HBXRepository
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.model.RoofSubstrate
import com.example.ui.components.DocumentAttachModal
import com.example.ui.components.DocumentViewerModal
import com.example.ui.theme.*

@Composable
fun SiteSurveyScreen(
    repository: HBXRepository,
    onContinueToConfigurator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val substrate by repository.roofSubstrate.collectAsState()
    val unshadedArea by repository.unshadedAreaSqFt.collectAsState()
    val evidencePhotos by repository.evidencePhotos.collectAsState()
    val surveyDocs by repository.getDocumentsForParent("SITE_SURVEY", "HBX-SRV-2026-084").collectAsState(emptyList())

    var showRemeasureDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var selectedViewerDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var showAttachModal by remember { mutableStateOf(false) }
    var activeStepNumber by remember { mutableIntStateOf(1) }
    var activeStepTitle by remember { mutableStateOf("Site Photo") }
    var shadowNote by remember { mutableStateOf("Minimal shading from South-East overhead water tank after 4:15 PM.") }
    var conduitNote by remember { mutableStateOf("Ground floor utility room, 12m cable run from parapet south riser.") }

    val completedCount = evidencePhotos.count { it.isCompleted }
    val progressPct = (completedCount * 100) / evidencePhotos.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Sunlight Contrast Boost Ambient Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerHigh,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(12.dp), color = LightGold.copy(alpha = 0.5f)) {
                                    Text(
                                        text = "REF: HBX-SRV-2026-084",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavy,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                                        Text("Site 6.5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                                    }
                                }
                            }
                            Text(
                                text = "Amit Singh",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Text(
                                text = "Civil Lines, Azamgarh, UP",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("SANCTIONED", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("4 kW", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DeepNavy)
                            Text("Target: 3.3 kW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        }
                    }

                    // High Precision GPS Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceContainerLow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(15.dp))
                                }
                                Column {
                                    Text("26.0738° N, 83.1859° E", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepNavy, fontFamily = Inter)
                                    Text("Civil Lines sector • ±4m high precision", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                            Icon(Icons.Default.GpsFixed, contentDescription = "GPS Active", tint = StatusSuccess, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Offline Resilient Cloud Sync Strip
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ONLINE SYNC", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepNavy)
                                Text("• $completedCount of ${evidencePhotos.size} Uploaded", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("Auto-saving field drafts locally", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Circular Progress Ring Indicator
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progressPct / 100f },
                            color = StatusSuccess,
                            trackColor = SurfaceContainerHigh,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "$progressPct%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                    }
                }
            }
        }

        // Photo Checklist Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Site Evidence Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Text(
                        text = "Capture clear daylight frames for discom approval",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceContainerLow,
                    modifier = Modifier.clickable { toastMessage = "Camera Torch Mode: Auto" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(13.dp))
                        Text("Torch Auto", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    }
                }
            }
        }

        // 2-Column Responsive Matrix of Photos
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val chunked = evidencePhotos.chunked(2)
                chunked.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { photo ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (photo.requiresAction) LightGold.copy(alpha = 0.2f) else SurfaceWhite,
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (photo.isCompleted) {
                                            val existingDoc = surveyDocs.firstOrNull { it.documentType.contains(photo.title, ignoreCase = true) }
                                            if (existingDoc != null) {
                                                selectedViewerDoc = existingDoc
                                            } else {
                                                selectedViewerDoc = DocumentItem(
                                                    documentId = "DOC-SURVEY-${photo.step}",
                                                    businessId = "HBX-SRV-2026-084",
                                                    parentEntityType = "SITE_SURVEY",
                                                    parentEntityId = "HBX-SRV-2026-084",
                                                    category = DocumentCategory.SITE_PHOTO,
                                                    documentType = photo.title,
                                                    originalFileName = "${photo.title.replace(" ", "_")}.jpg",
                                                    storageFileName = "site_photo_${photo.step}.jpg",
                                                    storagePath = "site-surveys/HBX-SRV-2026-084/photos/${photo.title.replace(" ", "_")}.jpg",
                                                    localFilePath = null,
                                                    mimeType = "image/jpeg",
                                                    fileExtension = "jpg",
                                                    fileSizeBytes = 2400000L,
                                                    uploadedByUid = "SURVEY_BDE",
                                                    uploadedByName = "Rahul Kumar",
                                                    uploadedByRole = "BDE",
                                                    uploadedAt = System.currentTimeMillis() - 3600000L,
                                                    uploadStatus = "COMPLETED",
                                                    syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                                                    checksum = "c9a33e5512b988771199aaccbb223344",
                                                    thumbnailPath = null,
                                                    description = "Geotagged daylight frame for DISCOM UPPCL approval",
                                                    version = 1,
                                                    downloadUrl = null
                                                )
                                            }
                                        } else {
                                            activeStepNumber = photo.step
                                            activeStepTitle = photo.title
                                            showAttachModal = true
                                        }
                                    }
                                    .testTag("photo_card_${photo.step}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(
                                                if (photo.isCompleted) PrimaryNavy.copy(alpha = 0.85f)
                                                else if (photo.requiresAction) LightGold.copy(alpha = 0.4f)
                                                else SurfaceContainerLow
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (photo.isCompleted) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(28.dp))
                                                Text("Uploaded", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (photo.badgeTag != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = DeepNavy.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(6.dp)
                                                ) {
                                                    Text(photo.badgeTag, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        } else if (photo.requiresAction) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryNavy),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                Text("TAP TO SHOOT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepNavy)
                                            }
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                                Text("Pending", color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = photo.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = photo.statusText,
                                            fontSize = 11.sp,
                                            fontWeight = if (photo.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (photo.isCompleted) StatusSuccess else if (photo.requiresAction) StatusWarning else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Technical Param Matrix
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(4.dp, 16.dp).background(GoldAccent, RoundedCornerShape(2.dp)))
                            Text(
                                text = "Technical Param Matrix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = SurfaceContainerLow) {
                            Text("Field Verified", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Roof Type Multi-Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ROOFTOP SUBSTRATE TYPE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RoofSubstrate.values().forEach { sub ->
                                val isSelected = sub == substrate
                                val label = when (sub) {
                                    RoofSubstrate.RCC_FLAT -> "RCC Flat"
                                    RoofSubstrate.TIN_SHED -> "Tin Shed"
                                    RoofSubstrate.TILED_ROOF -> "Tiled Roof"
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) PrimaryNavy else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { repository.setRoofSubstrate(sub) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else DeepNavy,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Usable Area Metric Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("UNSHADED USABLE FOOTPRINT", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.SquareFoot, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "$unshadedArea sq.ft (Unshaded)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavy
                                    )
                                }
                                Button(
                                    onClick = { showRemeasureDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceWhite, contentColor = DeepNavy),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Re-measure", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Shading Field Note
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SHADOW & HORIZON ASSESSMENT", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.WbTwilight, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                                Text(
                                    text = shadowNote,
                                    fontSize = 12.sp,
                                    color = DeepNavy
                                )
                            }
                        }
                    }

                    // Inverter & Conduit Path Note
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("INVERTER PLACEMENT & CONDUIT PATH", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Cable, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(18.dp))
                                Text(
                                    text = conduitNote,
                                    fontSize = 12.sp,
                                    color = DeepNavy
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Mobile Action Deck
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onContinueToConfigurator,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("continue_to_electrical_review_btn")
                ) {
                    Text("Continue to Electrical Review", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                OutlinedButton(
                    onClick = { toastMessage = "Draft saved locally with offline sync tag" },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Local Draft (Offline Sync Ready)", color = DeepNavy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }

    // Re-measure Dialog
    if (showRemeasureDialog) {
        var tempArea by remember { mutableStateOf(unshadedArea.toString()) }
        AlertDialog(
            onDismissRequest = { showRemeasureDialog = false },
            title = { Text("Update Unshaded Roof Area") },
            text = {
                OutlinedTextField(
                    value = tempArea,
                    onValueChange = { tempArea = it },
                    label = { Text("Area (sq.ft)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    tempArea.toIntOrNull()?.let { repository.setUnshadedArea(it) }
                    showRemeasureDialog = false
                    toastMessage = "Roof area updated: $tempArea sq.ft"
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemeasureDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Document Viewer Modal
    selectedViewerDoc?.let { doc ->
        DocumentViewerModal(
            document = doc,
            onDismiss = { selectedViewerDoc = null },
            onDownload = {
                toastMessage = "Downloading ${doc.originalFileName}..."
            },
            onDelete = { docId, reason ->
                repository.softDeleteDocument(docId, reason)
                toastMessage = "Photo soft deleted: $reason"
            }
        )
    }

    // Document Attach Modal
    if (showAttachModal) {
        DocumentAttachModal(
            parentEntityType = "SITE_SURVEY",
            parentEntityId = "HBX-SRV-2026-084",
            defaultCategory = DocumentCategory.SITE_PHOTO,
            onDismiss = { showAttachModal = false },
            onAttach = { name, category, desc, bytes ->
                repository.attachDocumentFromBytes(
                    fileName = name,
                    category = category,
                    parentType = "SITE_SURVEY",
                    parentId = "HBX-SRV-2026-084",
                    description = if (desc.isBlank()) "Site Evidence: $activeStepTitle" else desc,
                    bytes = bytes
                )
                repository.capturePhoto(activeStepNumber)
                toastMessage = "Captured & Queued: $activeStepTitle"
            }
        )
    }

    // Toast
    if (toastMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DeepNavy,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 12.sp
                )
            }
        }
        LaunchedEffect(toastMessage) {
            kotlinx.coroutines.delay(2200)
            toastMessage = null
        }
    }
}
