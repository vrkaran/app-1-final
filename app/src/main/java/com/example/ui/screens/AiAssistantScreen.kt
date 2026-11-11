package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HBXRepository
import com.example.ui.theme.*

@Composable
fun AiAssistantScreen(
    repository: HBXRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var billInput by remember { mutableStateOf("3450") }
    var loadInput by remember { mutableStateOf("4.0") }
    var areaInput by remember { mutableStateOf("380") }

    var aiOutput by remember {
        mutableStateOf(
            repository.calculateSolarAdvice(4.0, 3450.0, 380)
        )
    }

    var isThinking by remember { mutableStateOf(false) }

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
                    Text("AI Solar & Business Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = LightGold.copy(alpha = 0.35f)) {
                    Text("Gemini 2.5 Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepNavy, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        // Introduction Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeepNavy,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(LightGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(18.dp))
                        }
                        Text("HBX Solar Intelligence Suite", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        text = "Automated rooftop engineering analysis, PM Surya Ghar DBT eligibility auditing, and territory lead prioritization.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Interactive Audit Inputs Card
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
                    Text("SITE & ELECTRICAL PARAMETERS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = billInput,
                            onValueChange = { billInput = it },
                            label = { Text("Monthly Bill (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = loadInput,
                            onValueChange = { loadInput = it },
                            label = { Text("Sanctioned kW") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = areaInput,
                        onValueChange = { areaInput = it },
                        label = { Text("Usable Unshaded Rooftop (sq.ft)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            isThinking = true
                            val bill = billInput.toDoubleOrNull() ?: 3000.0
                            val load = loadInput.toDoubleOrNull() ?: 4.0
                            val area = areaInput.toIntOrNull() ?: 350
                            aiOutput = repository.calculateSolarAdvice(load, bill, area)
                            isThinking = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("run_ai_analysis_btn")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = LightGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Technical Audit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // AI Output Response Card
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
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                            Text("Technical Sizing & DBT Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = SurfaceContainerLow) {
                            Text("Verified", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiOutput,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepNavy,
                            modifier = Modifier.padding(12.dp),
                            fontFamily = Inter,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Quick Intelligence Prompts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("INTELLIGENT PRE-SET AUDITS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                listOf(
                    "PM Surya Ghar DBT Sanction Criteria (Azamgarh PUVVNL)" to "Direct ₹78,000 subsidy applicable for domestic residential systems >=3kW with net meter.",
                    "Shadow Obstacle Tolerance on 15° Tilt Angle" to "South-facing 180° orientation yields 4.8 peak sun hours with minimal parapet losses.",
                    "C&I High-T Commercial Solar ROI" to "Payback period: 3.2 years with accelerated depreciation benefits."
                ).forEach { (prompt, desc) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceWhite,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aiOutput = "💡 $prompt:\n\n$desc" }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(prompt, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                            Text(desc, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
