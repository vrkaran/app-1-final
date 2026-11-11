package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.example.model.DocumentCategory
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentAttachModal(
    parentEntityType: String,
    parentEntityId: String,
    defaultCategory: DocumentCategory = DocumentCategory.OTHER,
    onDismiss: () -> Unit,
    onAttach: (fileName: String, category: DocumentCategory, description: String, simulatedBytes: ByteArray) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(defaultCategory) }
    var fileName by remember {
        mutableStateOf(
            when (defaultCategory) {
                DocumentCategory.KYC -> "kyc_aadhaar_card.pdf"
                DocumentCategory.ELECTRICITY_BILL -> "uppcl_bill_latest.pdf"
                DocumentCategory.SITE_PHOTO -> "site_evidence_azimuth.jpg"
                DocumentCategory.PAYMENT_PROOF -> "upi_token_receipt.jpg"
                DocumentCategory.QUOTATION_PDF -> "hbx_proposal_3kwp.pdf"
                else -> "document_attachment.pdf"
            }
        )
    }
    var description by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0.45f) }

    val categories = listOf(
        DocumentCategory.SITE_PHOTO,
        DocumentCategory.ELECTRICITY_BILL,
        DocumentCategory.KYC,
        DocumentCategory.PAYMENT_PROOF,
        DocumentCategory.AGREEMENT,
        DocumentCategory.QUOTATION_PDF,
        DocumentCategory.INSPECTION_REPORT,
        DocumentCategory.OTHER
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardSurface,
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
                    Text(
                        text = "Attach Business Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) LightGold.copy(alpha = 0.2f) else SoftBg,
                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(LightGold)
                            ) else null,
                            modifier = Modifier.clickable {
                                selectedCategory = category
                                if (fileName.contains("_")) {
                                    val ext = fileName.substringAfterLast(".", "pdf")
                                    fileName = "${category.name.lowercase()}_file.$ext"
                                }
                            }
                        ) {
                            Text(
                                text = category.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DeepNavy else TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name & Extension") },
                    leadingIcon = {
                        Icon(
                            if (fileName.endsWith(".pdf", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = DeepNavy
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("document_filename_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Field Notes / Description (Optional)") },
                    placeholder = { Text("e.g. Verified by BDE with customer Aadhaar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("document_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Storage routing info banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = LightGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Target: $parentEntityType #$parentEntityId",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                            Text(
                                text = "Auto-saving to Room SQLite • Auto-syncs to Cloud Storage",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        // Generate a valid minimal PDF or image sample byte array
                        val sampleBytes = if (fileName.endsWith(".pdf", ignoreCase = true)) {
                            "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000101 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF".toByteArray()
                        } else {
                            // Minimal JPEG 1x1 header
                            byteArrayOf(
                                0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
                                0x00.toByte(), 0x10.toByte(), 0x4A.toByte(), 0x46.toByte(),
                                0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
                                0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x48.toByte(),
                                0x00.toByte(), 0x48.toByte(), 0x00.toByte(), 0x00.toByte(),
                                0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x43.toByte(),
                                0x00.toByte(), 0x08.toByte(), 0x06.toByte(), 0x06.toByte(),
                                0xFF.toByte(), 0xD9.toByte()
                            )
                        }

                        onAttach(fileName, selectedCategory, description, sampleBytes)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_attach_document_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Securely Attach & Queue Upload", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
