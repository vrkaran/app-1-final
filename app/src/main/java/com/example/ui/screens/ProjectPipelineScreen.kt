package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HBXRepository
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.model.StageStatus
import com.example.ui.components.DeliveryScannerDialog
import com.example.ui.components.DocumentAttachModal
import com.example.ui.components.DocumentViewerModal
import com.example.ui.theme.*

@Composable
fun ProjectPipelineScreen(
    repository: HBXRepository,
    onNavigateToSurvey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bomItems by repository.bomItems.collectAsState()
    val pipelineStages by repository.pipelineStages.collectAsState()
    val projectDocs by repository.getDocumentsForParent("PROJECT", "HBX-PRJ-2026-0042").collectAsState(emptyList())

    var showScannerDialog by remember { mutableStateOf(false) }
    var selectedViewerDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var showAttachDocModal by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Project Context & Selector Header
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerHigh) {
                                Text(
                                    text = "HBX-PRJ-2026-0042",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontFamily = Inter
                                )
                            }
                            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerLow) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusSuccess))
                                    Text("Active Site", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable { toastMessage = "Switching project context..." }
                        ) {
                            Text("Switch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Amit Singh 3.3 kW Rooftop Solar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Text("Azamgarh, UP", fontSize = 11.sp, color = TextSecondary)
                            Text("•", color = BorderSubtle)
                            Icon(Icons.Default.Handyman, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Text("Purvanchal Solar Works", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                    }

                    // Quick Meta Metrics Bento
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(4.dp)) {
                            Text("CAPACITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("3.30 kW", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f).padding(4.dp)) {
                            Text("GOVT. SUBSIDY", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("₹78,000", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f).padding(4.dp)) {
                            Text("STAGE MET", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("18 / 23", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // 23-Stage Pipeline Stepper Card
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
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Installation Pipeline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                Surface(shape = RoundedCornerShape(10.dp), color = LightGold.copy(alpha = 0.35f)) {
                                    Text("Stage 18 of 23", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("District Execution Workflow", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerLow) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusSuccess))
                                Text("ON SCHEDULE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                        }
                    }

                    // Progress Meter Bar (78.2%)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { 0.782f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryNavy,
                            trackColor = SurfaceContainerHigh
                        )
                    }

                    // Timeline Nodes (Stages 14 - 19)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        pipelineStages.forEach { stage ->
                            val isCompleted = stage.status == StageStatus.COMPLETED
                            val isActive = stage.status == StageStatus.ACTIVE

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isActive) Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceContainerHigh.copy(alpha = 0.5f))
                                            .padding(8.dp)
                                        else Modifier
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) StatusSuccess
                                            else if (isActive) PrimaryNavy
                                            else SurfaceContainerHigh
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else if (isActive) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(LightGold))
                                    } else {
                                        Text("${stage.stageNumber}", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stage.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isActive) PrimaryNavy else TextPrimary
                                        )
                                        Text(
                                            text = stage.dateOrSubtext,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCompleted && stage.dateOrSubtext.contains("₹")) StatusSuccess else TextSecondary
                                        )
                                    }
                                    Text(
                                        text = stage.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isActive) TextPrimary else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bill of Materials (BOM) Manifest Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Bill of Materials (BOM)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text("Manifest #MN-9921", fontSize = 11.sp, color = TextSecondary, fontFamily = Inter)
                    }

                    bomItems.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                item.title.contains("PERC") -> Icons.Default.SolarPower
                                                item.title.contains("Inverter") -> Icons.Default.ElectricalServices
                                                else -> Icons.Default.GridView
                                            },
                                            contentDescription = null,
                                            tint = PrimaryNavy,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text(item.specs, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.isVerified) StatusSuccess.copy(alpha = 0.15f) else SurfaceWhite
                                ) {
                                    Text(
                                        text = if (item.isVerified) "Verified" else item.quantity,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isVerified) StatusSuccess else PrimaryNavy,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontFamily = Inter
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Field Quality Handover
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Field Quality Handover",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text("2 Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusWarning)
                    }

                    // Challan PDF Item
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Dispatch Challan #CH-8821", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Generated by Lucknow Hub • 1.4 MB", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Button(
                                onClick = {
                                    val challanDoc = projectDocs.firstOrNull { it.documentId == "DOC-CHALLAN-8821" }
                                    if (challanDoc != null) {
                                        selectedViewerDoc = challanDoc
                                    } else {
                                        selectedViewerDoc = DocumentItem(
                                            documentId = "DOC-CHALLAN-8821",
                                            businessId = "HBX-PRJ-2026-0042",
                                            parentEntityType = "PROJECT",
                                            parentEntityId = "HBX-PRJ-2026-0042",
                                            category = DocumentCategory.QUOTATION_PDF,
                                            documentType = "Dispatch Challan",
                                            originalFileName = "Dispatch_Challan_CH-8821.pdf",
                                            storageFileName = "challan_CH-8821.pdf",
                                            storagePath = "projects/HBX-PRJ-2026-0042/installation-evidence/Dispatch_Challan_CH-8821.pdf",
                                            localFilePath = null,
                                            mimeType = "application/pdf",
                                            fileExtension = "pdf",
                                            fileSizeBytes = 1468006,
                                            uploadedByUid = "LOGISTICS_HUB",
                                            uploadedByName = "Lucknow Logistics Hub",
                                            uploadedByRole = "OPERATIONS_MANAGER",
                                            uploadedAt = System.currentTimeMillis() - 86400000L,
                                            uploadStatus = "COMPLETED",
                                            syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                                            checksum = "a8f92cd310f847b2c91823901bceaa42",
                                            thumbnailPath = null,
                                            description = "Official dispatch manifest for UP-Logistics vehicle UP-53-AT-9012",
                                            version = 1,
                                            downloadUrl = null
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceWhite, contentColor = PrimaryNavy),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Delivery OTP Handover
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerHigh.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Delivery OTP Handover", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Required from Amit Singh", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Button(
                                onClick = { showOtpDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LightGold, contentColor = DeepNavy),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Assigned Field Crew
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ASSIGNED FIELD CREW", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(PrimaryNavy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("SM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Sandeep Maurya", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                        Text("Lead Technician", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }

                            Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(GoldAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("AG", color = DeepNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Alok Gupta", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                        Text("Quality Inspector", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showScannerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("record_delivery_btn")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record Material Delivery & Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876500001"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call Sandeep (Tech)", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { toastMessage = "Connecting with Azamgarh District Support Hub..." },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("District Support", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Barcode Scanner Dialog
    if (showScannerDialog) {
        DeliveryScannerDialog(
            projectCode = "Amit Singh (HBX-PRJ-2026-0042)",
            onDismiss = { showScannerDialog = false },
            onScannedSuccess = {
                repository.verifyBomItem("BOM-1")
                repository.verifyBomItem("BOM-2")
                showScannerDialog = false
                toastMessage = "Scanner validated serial barcodes! Stage 18 delivery recorded."
            }
        )
    }

    // Document Viewer Modal for Challan / Evidence
    selectedViewerDoc?.let { doc ->
        DocumentViewerModal(
            document = doc,
            onDismiss = { selectedViewerDoc = null },
            onDownload = {
                toastMessage = "Downloading ${doc.originalFileName} to device..."
            },
            onDelete = { docId, reason ->
                repository.softDeleteDocument(docId, reason)
                toastMessage = "Document soft deleted with audit reason: $reason"
            }
        )
    }

    if (showAttachDocModal) {
        DocumentAttachModal(
            parentEntityType = "PROJECT",
            parentEntityId = "HBX-PRJ-2026-0042",
            defaultCategory = DocumentCategory.INSTALLATION_EVIDENCE,
            onDismiss = { showAttachDocModal = false },
            onAttach = { name, category, desc, bytes ->
                repository.attachDocumentFromBytes(
                    fileName = name,
                    category = category,
                    parentType = "PROJECT",
                    parentId = "HBX-PRJ-2026-0042",
                    description = desc,
                    bytes = bytes
                )
                toastMessage = "Queued document upload: $name"
            }
        )
    }

    // OTP Dialog
    if (showOtpDialog) {
        var enteredOtp by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = { Text("Customer OTP Verification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ask Amit Singh for the 6-digit handover OTP sent by UP-DISCOM / HBX.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { enteredOtp = it },
                        placeholder = { Text("Enter 6-digit OTP") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showOtpDialog = false
                    toastMessage = "OTP Handshake verified successfully! Handover authenticated."
                }) {
                    Text("Verify OTP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancel")
                }
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
