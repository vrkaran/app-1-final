package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.data.HBXRepository
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.model.PaymentRecord
import com.example.ui.components.DocumentAttachModal
import com.example.ui.components.DocumentViewerModal
import com.example.ui.theme.*

@Composable
fun FinancePaymentsScreen(
    repository: HBXRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val payments by repository.payments.collectAsState()
    var showNewPaymentDialog by remember { mutableStateOf(false) }
    var selectedViewerDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var showAttachDocModal by remember { mutableStateOf(false) }
    var attachForReceiptId by remember { mutableStateOf("RCP-2026-8801") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Top Back Header
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
                    Text("Finance & Manual Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerHigh) {
                    Text("Direct Treasury", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        // Summary Header Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrimaryNavy,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("COLLECTIONS & RECONCILIATION", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), letterSpacing = 0.5.sp)
                    val totalCollected = payments.sumOf { it.amount }
                    Text("₹%,.0f".format(totalCollected), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3 Payment Methods: Cash, UPI, Bank NEFT", color = LightGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("${payments.size} Verified Receipts", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            }
        }

        // Action Trigger Button
        item {
            Button(
                onClick = { showNewPaymentDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = LightGold, contentColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("record_manual_payment_btn")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Record Manual Customer Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Payments History List
        item {
            Text("RECEIPT & PAYMENT AUDIT TRAIL", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
        }

        items(payments, key = { it.receiptId }) { payment ->
            Surface(
                shape = RoundedCornerShape(12.dp),
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
                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceContainerLow) {
                                Text(payment.receiptId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = Inter)
                            }
                            Text("•", color = BorderSubtle)
                            Text(payment.method, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = StatusSuccess.copy(alpha = 0.15f)) {
                            Text(payment.status, color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(payment.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Consumer ID: ${payment.consumerId}", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("₹%,.0f".format(payment.amount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepNavy, fontFamily = Inter)
                    }

                    Surface(shape = RoundedCornerShape(6.dp), color = SurfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ref: ${payment.referenceNo}", fontSize = 10.sp, color = TextSecondary)
                            Text("Verified by: ${payment.verifiedBy}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = PrimaryNavy)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedViewerDoc = DocumentItem(
                                    documentId = "DOC-PAYMENT-${payment.receiptId}",
                                    businessId = payment.receiptId,
                                    parentEntityType = "PAYMENT",
                                    parentEntityId = payment.receiptId,
                                    category = DocumentCategory.PAYMENT_PROOF,
                                    documentType = "Payment Proof",
                                    originalFileName = "Proof_${payment.receiptId}.jpg",
                                    storageFileName = "proof_${payment.receiptId}.jpg",
                                    storagePath = "payments/${payment.receiptId}/proof/Proof_${payment.receiptId}.jpg",
                                    localFilePath = null,
                                    mimeType = "image/jpeg",
                                    fileExtension = "jpg",
                                    fileSizeBytes = 460800L,
                                    uploadedByUid = "FIELD_BDE",
                                    uploadedByName = "Rahul Kumar",
                                    uploadedByRole = "BDE",
                                    uploadedAt = System.currentTimeMillis() - 7200000L,
                                    uploadStatus = "COMPLETED",
                                    syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                                    checksum = "f1c88a44556677889900aabbccddeeff",
                                    thumbnailPath = null,
                                    description = "Payment ref: ${payment.referenceNo} for ${payment.customerName}",
                                    version = 1,
                                    downloadUrl = null
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Proof", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                attachForReceiptId = payment.receiptId
                                showAttachDocModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attach Proof", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // New Payment Record Dialog
    if (showNewPaymentDialog) {
        var custName by remember { mutableStateOf("Amit Singh") }
        var consId by remember { mutableStateOf("7219084421") }
        var amountText by remember { mutableStateOf("25000") }
        var selectedMethod by remember { mutableStateOf("UPI") }
        var refNumber by remember { mutableStateOf("UPI/TXN/88192019") }

        AlertDialog(
            onDismissRequest = { showNewPaymentDialog = false },
            title = { Text("Record Customer Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Customer Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = consId,
                        onValueChange = { consId = it },
                        label = { Text("10-Digit Consumer ID") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        singleLine = true
                    )
                    Text("Payment Method", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Cash", "UPI", "Bank Transfer", "Cheque").forEach { m ->
                            val isSel = m == selectedMethod
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSel) PrimaryNavy else SurfaceContainerLow,
                                modifier = Modifier.weight(1f).clickable { selectedMethod = m }
                            ) {
                                Text(
                                    text = m,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = refNumber,
                        onValueChange = { refNumber = it },
                        label = { Text("UTR / Cheque / Cash Receipt No") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 10000.0
                    repository.addPayment(custName, consId, amt, selectedMethod, refNumber)
                    showNewPaymentDialog = false
                    toastMessage = "Receipt generated for ₹%,.0f!".format(amt)
                }) {
                    Text("Issue Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPaymentDialog = false }) {
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
                toastMessage = "Payment proof soft deleted: $reason"
            }
        )
    }

    // Document Attach Modal
    if (showAttachDocModal) {
        DocumentAttachModal(
            parentEntityType = "PAYMENT",
            parentEntityId = attachForReceiptId,
            defaultCategory = DocumentCategory.PAYMENT_PROOF,
            onDismiss = { showAttachDocModal = false },
            onAttach = { name, category, desc, bytes ->
                repository.attachDocumentFromBytes(
                    fileName = name,
                    category = category,
                    parentType = "PAYMENT",
                    parentId = attachForReceiptId,
                    description = if (desc.isBlank()) "Payment evidence for receipt $attachForReceiptId" else desc,
                    bytes = bytes
                )
                toastMessage = "Attached proof for $attachForReceiptId!"
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
