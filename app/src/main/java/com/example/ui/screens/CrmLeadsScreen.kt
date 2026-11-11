package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HBXRepository
import com.example.model.Lead
import com.example.model.LeadStage
import com.example.ui.theme.*

@Composable
fun CrmLeadsScreen(
    repository: HBXRepository,
    onNavigateToDossier: (String) -> Unit,
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leads by repository.leads.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LeadStage.ALL) }
    var selectedSort by remember { mutableStateOf("Next Follow-up (Earliest)") }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedLeadForNote by remember { mutableStateOf<Lead?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val filteredLeads = remember(leads, searchQuery, selectedFilter, selectedSort) {
        leads.filter { lead ->
            val matchesFilter = when (selectedFilter) {
                LeadStage.ALL -> true
                else -> lead.stage == selectedFilter
            }
            val matchesQuery = searchQuery.isBlank() ||
                    lead.name.contains(searchQuery, ignoreCase = true) ||
                    lead.phone.contains(searchQuery, ignoreCase = true) ||
                    lead.village.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }.let { list ->
            when (selectedSort) {
                "Lead Value (Highest)" -> list.sortedByDescending { it.valueEstimate }
                "Solar Capacity (kW)" -> list.sortedByDescending { it.capacity }
                else -> list
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MainBg)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // Live Operational Metric Strip
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryNavy,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = LightGold, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "TERRITORY PIPELINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "₹1.42 Cr (${leads.size} Active)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "WIN RATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "28.5%",
                                        fontWeight = FontWeight.Bold,
                                        color = LightGold,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { toastMessage = "Exporting ${leads.size} customer dossiers to Excel (.xlsx)" },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Search Box with Clear & Voice action
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
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customer name, mobile, village or ID...", fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("crm_search_input")
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(BorderSubtle))
                        IconButton(
                            onClick = { toastMessage = "Listening for Hindi / English query..." },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Filter Pills Carousel
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LeadStage.values()) { stage ->
                        val isSelected = stage == selectedFilter
                        val count = if (stage == LeadStage.ALL) leads.size else leads.count { it.stage == stage }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) DeepNavy else SurfaceWhite,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .clickable { selectedFilter = stage }
                                .testTag("filter_${stage.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stage.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else SurfaceContainerHigh)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Showing Count & Sort Dropdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SHOWING",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceContainerLow
                        ) {
                            Text(
                                text = "${filteredLeads.size} Leads",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceWhite,
                            shadowElevation = 1.dp,
                            modifier = Modifier.clickable { showSortMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = selectedSort,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                "Next Follow-up (Earliest)",
                                "Lead Value (Highest)",
                                "Solar Capacity (kW)",
                                "Recently Added"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 12.sp) },
                                    onClick = {
                                        selectedSort = option
                                        showSortMenu = false
                                        toastMessage = "Sorted by: $option"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Empty State
            if (filteredLeads.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ManageSearch, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Text("No Matching Solar Leads", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "No customer profile matches your current filter or query.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = LeadStage.ALL
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset All Filters")
                            }
                        }
                    }
                }
            }

            // Lead Cards Feed
            items(filteredLeads, key = { it.id }) { lead ->
                LeadCardItem(
                    lead = lead,
                    onCallClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone.replace(" ", "")}"))
                        context.startActivity(intent)
                    },
                    onWhatsAppClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${lead.phone.filter { it.isDigit() }}?text=Hello%20${lead.name},%20regarding%20your%20HBX%20Solar%20quote."))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            toastMessage = "Opening WhatsApp for ${lead.name}"
                        }
                    },
                    onNoteClick = { selectedLeadForNote = lead },
                    onDossierClick = { onNavigateToDossier(lead.id) }
                )
            }
        }

        // Floating Action Button: + New Customer Onboarding
        FloatingActionButton(
            onClick = onOpenOnboarding,
            containerColor = LightGold,
            contentColor = DeepNavy,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
                .testTag("open_onboarding_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DeepNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = LightGold, modifier = Modifier.size(16.dp))
                }
                Text("New Customer Onboarding", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Note Modal
        selectedLeadForNote?.let { lead ->
            com.example.ui.components.LeadInteractionNoteDialog(
                leadName = lead.name,
                onDismiss = { selectedLeadForNote = null },
                onSaveNote = { note ->
                    repository.addNoteToLead(lead.name, note)
                    toastMessage = "Interaction note saved for ${lead.name}"
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
}

@Composable
fun LeadCardItem(
    lead: Lead,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onNoteClick: () -> Unit,
    onDossierClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Name, Priority & Stage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val initials = lead.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = PrimaryNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = PlusJakartaSans
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = lead.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (lead.priority == com.example.model.LeadPriority.HIGH) {
                                Surface(shape = RoundedCornerShape(4.dp), color = StatusDanger.copy(alpha = 0.12f)) {
                                    Text("HIGH PRIORITY", color = StatusDanger, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            } else if (lead.priority == com.example.model.LeadPriority.COMMERCIAL) {
                                Surface(shape = RoundedCornerShape(4.dp), color = LightGold.copy(alpha = 0.35f)) {
                                    Text("C&I COMMERCIAL", color = PrimaryNavy, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Text(lead.phone, fontSize = 11.sp, color = TextSecondary, fontFamily = Inter)
                            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(BorderSubtle))
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Text(lead.village, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Stage Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(lead.stage.badgeBg)
                ) {
                    Text(
                        text = lead.stage.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(lead.stage.badgeColor),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Specs Bento Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MainBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceWhite,
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                            }
                        }
                        Column {
                            Text("CONFIG", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text(lead.capacity, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceWhite,
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                            }
                        }
                        Column {
                            Text("ASSIGNED TO", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text(lead.assignedTo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNavy, maxLines = 1)
                        }
                    }
                }
            }

            // Follow-up Alert Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusWarning.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(15.dp))
                        Text(
                            text = "Follow-up: ${lead.followUpTime}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Quote: ${lead.quoteRef}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // Action Buttons Row: Call, WhatsApp, Add Note, Dossier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp, color = DeepNavy, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onWhatsAppClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusSuccess.copy(alpha = 0.15f),
                        contentColor = StatusSuccess
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.2f).height(36.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceContainerLow,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onNoteClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EditNote, contentDescription = "Add Note", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = onDossierClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepNavy,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Dossier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
