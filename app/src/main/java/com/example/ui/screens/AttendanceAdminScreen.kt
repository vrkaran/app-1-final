package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HBXRepository
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.model.UserRole
import com.example.ui.components.DocumentAttachModal
import com.example.ui.components.DocumentViewerModal
import com.example.ui.components.RoleSwitchDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AttendanceAdminScreen(
    repository: HBXRepository,
    onNavigateToPayments: () -> Unit,
    onNavigateToAi: () -> Unit,
    onOpenRoleDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isClockedIn by repository.isClockedIn.collectAsState()
    val isOnBreak by repository.isOnBreak.collectAsState()
    val currentRole by repository.currentRole.collectAsState()
    val allDocs by repository.allDocuments.collectAsState(emptyList())

    var secondsElapsed by remember { mutableStateOf(1280) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showBreakDialog by remember { mutableStateOf(false) }
    var showVisitDialog by remember { mutableStateOf(false) }

    var docSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<DocumentCategory?>(null) }
    var selectedViewerDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var showAttachDocModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    val hours = secondsElapsed / 3600
    val minutes = (secondsElapsed % 3600) / 60
    val seconds = secondsElapsed % 60
    val timerString = "${hours}h ${minutes}m ${seconds}s active"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Real-Time Telemetry & Shift Header Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeepNavy,
                shadowElevation = 4.dp,
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LightGold))
                                Text("REAL-TIME TELEMETRY", color = LightGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("SYNC: 100%", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = Inter)
                    }

                    Text(
                        text = "08:47:16 AM",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LightGold, modifier = Modifier.size(14.dp))
                            Text("Thursday, 12 Nov 2026", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.15f)) {
                            Text("Shift In-Progress", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = LightGold, modifier = Modifier.size(15.dp))
                                Text("General Field Shift", color = Color.White, fontSize = 11.sp)
                            }
                            Text("08:30 AM – 05:30 PM", color = LightGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Inter)
                        }
                    }
                }
            }
        }

        // Live Geofence Status Banner
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusSuccess))
                            Text("Inside Configured Geofence", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                    }

                    Text("HBX Azamgarh District Operational Center, Civil Lines", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("SATELLITE PRECISION", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text("±4.2 metres", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Text("High Precision Lock", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("CENTROID PROXIMITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text("12m from hub center", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Text("Permitted radius: 150m", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Interactive Mini Geo-Map Card (Perimeter Canvas)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                            Text("Azamgarh Hub Perimeter Telemetry", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                        }
                        Text("15s ago", fontSize = 10.sp, color = TextSecondary)
                    }

                    // Vector Canvas Simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(DeepNavy)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2

                            // Draw radar grid rings
                            drawCircle(color = Color(0xFF1B2B4B), radius = 60.dp.toPx(), center = Offset(centerX, centerY))
                            drawCircle(color = Color(0xFF22355A), radius = 35.dp.toPx(), center = Offset(centerX, centerY))

                            // Draw Geofence Perimeter 150m Circle
                            drawCircle(
                                color = StatusSuccess.copy(alpha = 0.25f),
                                radius = 70.dp.toPx(),
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = StatusSuccess,
                                radius = 70.dp.toPx(),
                                center = Offset(centerX, centerY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )

                            // Hub Centroid
                            drawCircle(color = LightGold, radius = 5.dp.toPx(), center = Offset(centerX, centerY))

                            // User Location (12m offset)
                            val userX = centerX + 18.dp.toPx()
                            val userY = centerY - 14.dp.toPx()
                            drawCircle(color = StatusInfo.copy(alpha = 0.4f), radius = 12.dp.toPx(), center = Offset(userX, userY))
                            drawCircle(color = StatusInfo, radius = 5.dp.toPx(), center = Offset(userX, userY))
                        }

                        // HUD Coordinate Tag
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DeepNavy.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                                Text("26.0734° N, 83.1859° E", color = Color.White, fontSize = 10.sp, fontFamily = Inter)
                            }
                        }

                        // Recenter Button
                        IconButton(
                            onClick = { toastMessage = "Centroid aligned: Lat 26.0734° N, Lon 83.1859° E" },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceWhite)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Recenter", tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Azamgarh Civil Lines Hub Polygon", fontSize = 11.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                            Text("Biometric + Cryptographic Tag", fontSize = 10.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Primary Shift Action Matrix
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clocked-in Status Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isClockedIn) StatusSuccess else StatusDanger))
                            Text(
                                text = if (isClockedIn) "Clocked In & Verified" else "Shift Offline",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.12f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Timelapse, contentDescription = null, tint = LightGold, modifier = Modifier.size(13.dp))
                                Text(timerString, color = LightGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Inter)
                            }
                        }
                    }
                }

                // Dual Actions: Field Break / Register Visit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            repository.toggleBreak()
                            toastMessage = if (!isOnBreak) "Field break initiated (60m quota)" else "Resumed operational shift"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceWhite, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Coffee, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isOnBreak) "Resume Shift" else "Start Field Break", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showVisitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceWhite, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register Visit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Clock Out Action
                OutlinedButton(
                    onClick = {
                        repository.toggleClockIn()
                        toastMessage = if (isClockedIn) "Shift egress logged." else "Shift started."
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isClockedIn) "Clock Out Shift" else "Clock In Shift", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Quick Module Hub Shortcuts (Payments & AI)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("EXTENDED BUSINESS MODULES", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToPayments() }
                                .testTag("open_finance_module_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Finance Ledger", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepNavy)
                                    Text("Cash / UPI / Cheque", fontSize = 9.sp, color = TextSecondary)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToAi() }
                                .testTag("open_ai_assistant_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("AI Solar Advisor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepNavy)
                                    Text("Gemini Sizing & DBT", fontSize = 9.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    // Role Switcher Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryNavy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRoleDialog() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = LightGold, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Current Role: ${currentRole.displayName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text(currentRole.badge, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                            Text("Switch (10 Roles)", color = LightGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Enterprise Document Management Console
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
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Enterprise Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                                Surface(shape = RoundedCornerShape(8.dp), color = StatusSuccess.copy(alpha = 0.15f)) {
                                    Text("Cloud Storage", color = StatusSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("${allDocs.size} files in local & cloud sandbox", fontSize = 11.sp, color = TextSecondary)
                        }

                        Button(
                            onClick = { showAttachDocModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Search text field
                    OutlinedTextField(
                        value = docSearchQuery,
                        onValueChange = { docSearchQuery = it },
                        placeholder = { Text("Search by file name or ID...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Category Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCategoryFilter == null) LightGold.copy(alpha = 0.25f) else SurfaceContainerLow,
                            modifier = Modifier.clickable { selectedCategoryFilter = null }
                        ) {
                            Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCategoryFilter == DocumentCategory.SITE_PHOTO) LightGold.copy(alpha = 0.25f) else SurfaceContainerLow,
                            modifier = Modifier.clickable { selectedCategoryFilter = DocumentCategory.SITE_PHOTO }
                        ) {
                            Text("Site Photos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCategoryFilter == DocumentCategory.QUOTATION_PDF) LightGold.copy(alpha = 0.25f) else SurfaceContainerLow,
                            modifier = Modifier.clickable { selectedCategoryFilter = DocumentCategory.QUOTATION_PDF }
                        ) {
                            Text("PDFs & Quotes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCategoryFilter == DocumentCategory.PAYMENT_PROOF) LightGold.copy(alpha = 0.25f) else SurfaceContainerLow,
                            modifier = Modifier.clickable { selectedCategoryFilter = DocumentCategory.PAYMENT_PROOF }
                        ) {
                            Text("Payments", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }

                    // Filtered Documents List
                    val filteredDocs = allDocs.filter { doc ->
                        val matchesQuery = docSearchQuery.isBlank() || doc.originalFileName.contains(docSearchQuery, ignoreCase = true) || doc.businessId.contains(docSearchQuery, ignoreCase = true)
                        val matchesCat = selectedCategoryFilter == null || doc.category == selectedCategoryFilter
                        matchesQuery && matchesCat
                    }

                    if (filteredDocs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No documents matching filter criteria", fontSize = 12.sp, color = TextSecondary)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredDocs.take(6).forEach { doc ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceContainerLow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedViewerDoc = doc }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (doc.isPdf) StatusDanger.copy(alpha = 0.15f) else StatusInfo.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (doc.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = if (doc.isPdf) StatusDanger else StatusInfo,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(doc.originalFileName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${doc.category.displayName} • ${doc.formattedSize}", fontSize = 10.sp, color = TextSecondary)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = StatusSuccess.copy(alpha = 0.15f)
                                        ) {
                                            Text("Verified", color = StatusSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Register Visit Dialog
    if (showVisitDialog) {
        var clientName by remember { mutableStateOf("") }
        var purpose by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showVisitDialog = false },
            title = { Text("Register Field Client Visit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Geotagged with current coordinates (26.0734° N, 83.1859° E)", fontSize = 11.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name / Org") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Visit Purpose (Survey, Quote, Inspection)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showVisitDialog = false
                    toastMessage = "Field visit logged for $clientName"
                }) {
                    Text("Register Visit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVisitDialog = false }) {
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
                toastMessage = "Document soft deleted: $reason"
            }
        )
    }

    // Document Attach Modal
    if (showAttachDocModal) {
        DocumentAttachModal(
            parentEntityType = "COMPANY",
            parentEntityId = "HBX-CORP-01",
            defaultCategory = DocumentCategory.OTHER,
            onDismiss = { showAttachDocModal = false },
            onAttach = { name, category, desc, bytes ->
                repository.attachDocumentFromBytes(
                    fileName = name,
                    category = category,
                    parentType = "COMPANY",
                    parentId = "HBX-CORP-01",
                    description = desc,
                    bytes = bytes
                )
                toastMessage = "Queued document upload: $name"
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
