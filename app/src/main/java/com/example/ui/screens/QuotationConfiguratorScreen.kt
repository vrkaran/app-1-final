package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.ui.components.DocumentAttachModal
import com.example.ui.components.DocumentViewerModal
import com.example.ui.theme.*

@Composable
fun QuotationConfiguratorScreen(
    repository: HBXRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sanctionedLoad by remember { mutableStateOf(4.0) }
    var selectedCategory by remember { mutableStateOf("Domestic LM-1") }
    var selectedArchitecture by remember { mutableStateOf("On-Grid (PM Surya Ghar)") }
    var consumerId by remember { mutableStateOf("7219084421") }
    var isVerifyingId by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showBillViewer by remember { mutableStateOf(false) }
    var showAttachBillModal by remember { mutableStateOf(false) }

    // Dynamic Financial Calculations
    val recommendedKw = (sanctionedLoad * 0.825).coerceAtLeast(1.0)
    val grossCost = (recommendedKw * 60000).toInt()
    val subsidy = if (recommendedKw <= 2.0) 60000 else 78000
    val netPayable = grossCost - subsidy
    val emiMonthly = (netPayable * 0.018).toInt()
    val estUnits = (recommendedKw * 118).toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Back Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { onBack() }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                    Text("System Configurator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerHigh) {
                    Text("PM Surya Ghar Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        // Stepper Progress Tracker (Step 3 of 6)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Step 3 of 6", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 13.sp)
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(LightGold))
                            Text("Grid & Connection Details", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(
                            listOf(
                                "1. Personal" to true,
                                "2. Property" to true,
                                "3. Electricity" to false, // active
                                "4. Specs" to false,
                                "5. Consent" to false,
                                "6. Review" to false
                            )
                        ) { (step, isDone) ->
                            val isActive = step.startsWith("3.")
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isActive) PrimaryNavy else SurfaceContainerLow,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isDone) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(13.dp))
                                    } else if (isActive) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LightGold))
                                    }
                                    Text(
                                        text = step,
                                        fontSize = 11.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Customer Micro-Card
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AS", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 13.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Amit Singh", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Icon(Icons.Default.Verified, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(14.dp))
                            }
                            Text("+91 98391 44210 • Civil Lines, Azamgarh", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Button(
                        onClick = { toastMessage = "Editing applicant profile..." },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLow, contentColor = PrimaryNavy),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: Discom & Connection Matrix
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                            Text("Grid & Connection Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerLow) {
                            Text("PUVVNL Sync", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // DISCOM Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("POWER DISTRIBUTION UTILITY (DISCOM)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                                    Text("UPPCL (PUVVNL - Purvanchal)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                                }
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Consumer ID Input & Re-verify
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("10-DIGIT CONSUMER ACCOUNT ID", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusSuccess))
                                Text("Verified • PUVVNL Active", fontSize = 10.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(consumerId, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy, fontFamily = Inter)
                                Button(
                                    onClick = {
                                        isVerifyingId = true
                                        toastMessage = "Connecting with PUVVNL database gateway..."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceWhite, contentColor = PrimaryNavy),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-verify", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Tariff Category
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TARIFF / CONNECTION CATEGORY", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Domestic LM-1", "Commercial LM-2", "Agricultural LM-5").forEach { cat ->
                                val isSelected = cat == selectedCategory
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) PrimaryNavy else SurfaceContainerLow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Sanctioned Grid Load (+/- counter)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SANCTIONED GRID LOAD", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("PM Surya Ghar ceiling: 4.0 kW", fontSize = 10.sp, color = StatusInfo)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "%.1f kW".format(sanctionedLoad),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = PrimaryNavy
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { if (sanctionedLoad > 1.0) sanctionedLoad -= 0.5 },
                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceWhite)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { if (sanctionedLoad < 10.0) sanctionedLoad += 0.5 },
                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceWhite)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Document Capture: OCR Auto-Extraction Feedback
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
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                            Text("Electricity Bill Snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerLow) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                                Text("OCR Match 100%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBillViewer = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = LightGold, modifier = Modifier.size(24.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("UPPCL_Bill_Aug2024.pdf", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                                Text("Extracted: Tariffs, 4kW sanctioned load & bill cycle", fontSize = 11.sp, color = TextSecondary)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceWhite) {
                                Text("Verified", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Recommended Solar Solution & Subsidized Ledger
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SolarPower, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                            Text("Recommended Solar Solution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = LightGold.copy(alpha = 0.4f)) {
                            Text("Tier-1 PERC", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Architecture Selection (On-Grid vs Hybrid)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Option 1: On-Grid
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, LightGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArchitecture = "On-Grid (PM Surya Ghar)" }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("On-Grid (PM Surya Ghar Net-Metered)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                                        Text("Zero-export protection • Direct ₹78,000 DBT", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                Icon(Icons.Default.Verified, contentDescription = null, tint = LightGold, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Option 2: Hybrid
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArchitecture = "Hybrid Battery Backup" }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(18.dp).border(1.5.dp, BorderSubtle, CircleShape))
                                Column {
                                    Text("Hybrid (Solar + Lithium Battery Backup)", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextPrimary)
                                    Text("Guaranteed 24/7 power • Reduced subsidy tier", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    // Sized Capacity & Hardware
                    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("OPTIMAL SIZED CAPACITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("%.2f kWp".format(recommendedKw), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PrimaryNavy)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("EST. GENERATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                                        Text("$estUnits Units / mo", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 14.sp)
                                    }
                                }
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ViewModule, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                                    Text("6x 550W Tier-1 Mono PERC Bifacial + MicroTek Inverter", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PrimaryNavy)
                                }
                            }
                        }
                    }

                    // Subsidized Pricing Ledger
                    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SUBSIDIZED PRICING LEDGER", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Turnkey Project Cost", fontSize = 12.sp, color = TextSecondary)
                                Text("₹%,d".format(grossCost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, fontFamily = Inter)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Redeem, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                                    Text("Central DBT Subsidy (Direct to Bank)", fontSize = 12.sp, color = StatusSuccess, fontWeight = FontWeight.Medium)
                                }
                                Text("- ₹%,d".format(subsidy), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusSuccess, fontFamily = Inter)
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Net Customer Payable", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                                    Text("EMI starting at ₹%,d/mo (SBI Solar)".format(emiMonthly), fontSize = 10.sp, color = TextSecondary)
                                }
                                Text("₹%,d".format(netPayable), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryNavy, fontFamily = Inter)
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { toastMessage = "Quotation saved as draft (#HBX-8821)" },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Draft", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = PrimaryNavy)
                }

                Button(
                    onClick = {
                        toastMessage = "Generating PM Surya Ghar Application Package & Consent Form..."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(46.dp)
                        .testTag("docs_consent_btn")
                ) {
                    Text("Docs & Consent", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LightGold, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    // Bill Viewer Dialog
    if (showBillViewer) {
        val billDoc = DocumentItem(
            documentId = "DOC-BILL-UPPCL-01",
            businessId = "LEAD-001",
            parentEntityType = "LEAD",
            parentEntityId = "LEAD-001",
            category = DocumentCategory.ELECTRICITY_BILL,
            documentType = "Electricity Bill",
            originalFileName = "UPPCL_Bill_Aug2024.pdf",
            storageFileName = "uppcl_bill_aug2024.pdf",
            storagePath = "customers/LEAD-001/electricity-bills/UPPCL_Bill_Aug2024.pdf",
            localFilePath = null,
            mimeType = "application/pdf",
            fileExtension = "pdf",
            fileSizeBytes = 870400L,
            uploadedByUid = "FIELD_BDE",
            uploadedByName = "Rahul Kumar",
            uploadedByRole = "BDE",
            uploadedAt = System.currentTimeMillis() - 172800000L,
            uploadStatus = "COMPLETED",
            syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
            checksum = "d4f55a1234bcde567890f1234567890a",
            thumbnailPath = null,
            description = "Extracted: Tariffs Domestic LM-1, 4kW sanctioned load & bill cycle",
            version = 1,
            downloadUrl = null
        )
        DocumentViewerModal(
            document = billDoc,
            onDismiss = { showBillViewer = false },
            onDownload = {
                toastMessage = "Downloading ${billDoc.originalFileName}..."
            },
            onDelete = { docId, reason ->
                repository.softDeleteDocument(docId, reason)
                toastMessage = "Bill record soft deleted: $reason"
            }
        )
    }

    if (showAttachBillModal) {
        DocumentAttachModal(
            parentEntityType = "LEAD",
            parentEntityId = "LEAD-001",
            defaultCategory = DocumentCategory.ELECTRICITY_BILL,
            onDismiss = { showAttachBillModal = false },
            onAttach = { name, category, desc, bytes ->
                repository.attachDocumentFromBytes(
                    fileName = name,
                    category = category,
                    parentType = "LEAD",
                    parentId = "LEAD-001",
                    description = if (desc.isBlank()) "OCR Verified Bill for Consumer $consumerId" else desc,
                    bytes = bytes
                )
                toastMessage = "Attached bill for consumer #$consumerId!"
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
