package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import com.example.model.LeadPriority
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun RoleSwitchDialog(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Switch User Role",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                        Text(
                            text = if (com.example.BuildConfig.DEBUG) "RBAC Matrix (10 Roles • Dev Override)" else "RBAC Security Policy (10 Roles)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (com.example.BuildConfig.DEBUG) GoldAccent else TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                if (!com.example.BuildConfig.DEBUG) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusError.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Production Security: Role elevation is strictly enforced via Firebase Auth Custom Claims. Self-assignment is prohibited.",
                            fontSize = 11.sp,
                            color = StatusError,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(UserRole.values()) { role ->
                        val isSelected = role == currentRole
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SurfaceContainerHigh else SurfaceContainerLow,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryNavy) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRoleSelected(role)
                                    onDismiss()
                                }
                                .testTag("role_option_${role.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryNavy else Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = role.displayName.take(2).uppercase(),
                                            color = if (isSelected) LightGold else PrimaryNavy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = role.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = role.badge,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewCustomerOnboardingDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, village: String, capacity: String, priority: LeadPriority) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("3 kW Rooftop On-Grid") }
    var priority by remember { mutableStateOf(LeadPriority.HIGH) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LightGold.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryNavy)
                        }
                        Column {
                            Text(
                                text = "Register Solar Prospect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            Text(
                                text = "Direct UP-NEDA & Discom sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Ramesh Chandra Yadav") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboard_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number *") },
                    placeholder = { Text("+91 98390 XXXXX") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboard_phone_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village / Locality *") },
                    placeholder = { Text("e.g. Bilariyaganj, Azamgarh") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboard_village_input"),
                    singleLine = true
                )

                Text(
                    text = "ESTIMATED SOLAR CAPACITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val caps = listOf("2 kW", "3 kW", "5 kW", "10 kW+")
                    caps.forEach { capOption ->
                        val isSelected = capacity.startsWith(capOption)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PrimaryNavy else SurfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { capacity = "$capOption Rooftop System" }
                        ) {
                            Text(
                                text = capOption,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onSubmit(name, phone, village.ifBlank { "Azamgarh" }, capacity, priority)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightGold,
                            contentColor = DeepNavy
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("submit_onboard_button")
                    ) {
                        Text("Create Prospect Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LeadInteractionNoteDialog(
    leadName: String,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, tint = PrimaryNavy)
                        Text(
                            text = "Lead Interaction Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Lead: $leadName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryNavy,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Remarks / Field Findings") },
                    placeholder = { Text("Enter notes on shadow inspection, discom sanction, subsidy docs, or loan terms...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("lead_note_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                onSaveNote(noteText)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("Save Activity Note")
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryScannerDialog(
    projectCode: String,
    onDismiss: () -> Unit,
    onScannedSuccess: () -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var scannedItem by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Record Material Delivery & Scan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                        Text(
                            text = projectCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Camera Viewfinder Simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepNavy),
                    contentAlignment = Alignment.Center
                ) {
                    // Reticle overlay
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .border(2.dp, LightGold, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(color = LightGold, strokeWidth = 3.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = LightGold.copy(alpha = 0.8f),
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    // Scanned overlay text
                    Text(
                        text = if (scannedItem != null) "Scanned: $scannedItem" else "Align Serial Barcode / QR Code",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "Scan serial numbers for 6x 550W Bifacial Modules & PureWave-3K Inverter to trigger recipient OTP handshake.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = {
                        isScanning = true
                        scannedItem = "HBX-PV-550W-BATCH-9921-A"
                        onScannedSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("trigger_barcode_scan_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture Barcode & Validate")
                }
            }
        }
    }
}

@Composable
fun NotificationDrawerDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryNavy)
                        Text(
                            text = "Executive Action Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Alert 1 (Critical)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusDanger.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Material Dispatch Lag", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 12.sp)
                            Text("Inverter consignment #MN-9921 stranded on Varanasi bypass. 14 installations impacted.", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                // Alert 2 (Warning)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusWarning.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                        Column {
                            Text("DISCOM Inspection Delay", fontWeight = FontWeight.Bold, color = StatusWarning, fontSize = 12.sp)
                            Text("PUVVNL net-meter inspector unassigned for 4 commercial sites in Sagri block.", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                // Alert 3 (Finance)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusInfo.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(20.dp))
                        Column {
                            Text("PM Surya Ghar DBT Release", fontWeight = FontWeight.Bold, color = StatusInfo, fontSize = 12.sp)
                            Text("Batch #881 (₹18.4 Lakhs, 24 consumers) cleared by portal; awaiting authorized signature.", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Acknowledge Alerts")
                }
            }
        }
    }
}
