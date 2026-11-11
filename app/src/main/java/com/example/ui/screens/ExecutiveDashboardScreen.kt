package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HBXRepository
import com.example.ui.theme.*

@Composable
fun ExecutiveDashboardScreen(
    repository: HBXRepository,
    onBackToFieldView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val approvalQueue by repository.approvalQueue.collectAsState()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Return to Field Operations Bar
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { onBackToFieldView() }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                        Text("Return to Field Operations", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 12.sp)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = SurfaceContainerLow) {
                        Text("Super Admin Mode", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // Section 1: Executive Command Header Bar
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceContainerHigh) {
                                Text("Tier-1 Command Grid", color = DeepNavy, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text("•", color = BorderSubtle)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusSuccess))
                                Text("DISCOM Gateway: 99.98% Synced", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Executive Command Center",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Text(
                        text = "Operational cycle: Q3 FY2026-27 • Telemetry synchronized across 6 district sub-hubs (Azamgarh, Mau, Jaunpur, Ballia, Ghazipur, Deoria)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { toastMessage = "Generating full executive dossier (.pdf / .xlsx)..." },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = LightGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Territory Dossier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Executive 8-KPI Master Metric Matrix
        item {
            Text("TERRITORY TELEMETRY MATRIX", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Metric 1: Workforce
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite, modifier = Modifier.weight(1f), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("FIELD WORKFORCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("142", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DeepNavy)
                                Text("+12 MTD", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("98.2% shift attendance today", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    // Metric 2: Applications
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite, modifier = Modifier.weight(1f), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PENDING APPLICATIONS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("28", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DeepNavy)
                                Text("Action Req", color = StatusWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("8 BDA • 14 Installers • 6 QA", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Metric 3: Active Customers
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite, modifier = Modifier.weight(1f), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ACTIVE CUSTOMERS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("1,842", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DeepNavy)
                                Text("+18%", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Residential & Agri Cold Hubs", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    // Metric 4: Subsidy Disbursed
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite, modifier = Modifier.weight(1f), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PM SURYA GHAR DBT", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("₹1.14 Cr", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DeepNavy)
                            Text("₹18.4L in DBT clearing (Batch 14)", fontSize = 10.sp, color = StatusInfo)
                        }
                    }
                }
            }
        }

        // Section 3: 23-Stage Pipeline Velocity & Distribution
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
                            Text("23-Stage Pipeline Velocity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                            Text("124 Live Projects across 5 synchronized phases", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow) {
                            Text("124 Live", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    // Multi-Phase Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(modifier = Modifier.weight(42f).fillMaxHeight().background(Color(0xFF94A3B8)))
                        Box(modifier = Modifier.weight(31f).fillMaxHeight().background(StatusInfo))
                        Box(modifier = Modifier.weight(24f).fillMaxHeight().background(GoldAccent))
                        Box(modifier = Modifier.weight(16f).fillMaxHeight().background(PrimaryNavy))
                        Box(modifier = Modifier.weight(11f).fillMaxHeight().background(StatusSuccess))
                    }

                    // 5 Phase Cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "Ph I" to "42",
                            "Ph II" to "31",
                            "Ph III" to "24",
                            "Ph IV" to "16",
                            "Ph V" to "11"
                        ).forEach { (ph, count) ->
                            Surface(shape = RoundedCornerShape(6.dp), color = SurfaceContainerLow, modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(ph, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Text(count, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                                }
                            }
                        }
                    }

                    Text("Purvanchal average installation turnaround stands at 13.4 days from structural sanction.", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Section 4: District Operational Benchmarks Table
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
                    Text("District Operational Benchmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                    Text("Aggregated monthly throughput against MNRE regional quotas", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    val benchmarks = listOf(
                        Triple("Azamgarh (HQ)", "94.2%", "214.5 kW"),
                        Triple("Mau Sub-Hub", "88.6%", "96.0 kW"),
                        Triple("Jaunpur Sector", "78.1%", "64.2 kW"),
                        Triple("Ballia Extension", "65.0%", "37.3 kW")
                    )

                    benchmarks.forEach { (name, target, cap) ->
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Target: $target", fontSize = 11.sp, color = TextSecondary)
                                    Text(cap, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepNavy)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: High-Priority Executive Approvals Queue
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(LightGold))
                            Text("Executive Approvals Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerLow) {
                            Text("${approvalQueue.count { it.status == "Pending" }} Pending", color = StatusWarning, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    approvalQueue.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.requestId, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryNavy)
                                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceWhite) {
                                        Text(item.domain, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Text(item.applicant, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(item.applicantSub, fontSize = 10.sp, color = TextSecondary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.valuation, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepNavy)
                                    if (item.status == "Approved") {
                                        Surface(shape = RoundedCornerShape(6.dp), color = StatusSuccess.copy(alpha = 0.15f)) {
                                            Text("Approved", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                repository.approveRequest(item.requestId)
                                                toastMessage = "Approved ${item.requestId}: ${item.applicant}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp).testTag("approve_btn_${item.requestId}")
                                        ) {
                                            Text("Direct Authorize", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
