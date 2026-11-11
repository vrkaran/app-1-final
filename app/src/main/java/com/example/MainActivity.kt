package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.AuthRepository
import com.example.data.FirebaseAuthRepository
import com.example.data.HBXRepository
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.HBXSolarTheme
import com.example.ui.theme.LightGold
import com.example.ui.theme.MainBg
import com.example.util.configureAppCheck
import kotlinx.coroutines.launch

enum class AppSubScreen {
    NONE,
    CONFIGURATOR,
    EXECUTIVE_VIEW,
    PAYMENTS_VIEW,
    AI_ASSISTANT_VIEW
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check before setContent, ViewModels, or any Firebase operation
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            HBXSolarTheme {
                val context = LocalContext.current
                val authRepository: AuthRepository = remember { FirebaseAuthRepository(context) }
                HBXAppRoot(authRepository = authRepository)
            }
        }
    }
}

@Composable
fun HBXAppRoot(authRepository: AuthRepository) {
    val context = LocalContext.current
    val repository = remember(context) { HBXRepository(context) }

    val currentUser by authRepository.currentUser.collectAsState()
    val userProfile by authRepository.userProfile.collectAsState()
    val isAuthLoading by authRepository.isAuthLoading.collectAsState()

    var devBypassRole by remember { mutableStateOf<UserRole?>(null) }

    // Derive effective role from authenticated Firestore profile or dev override
    val effectiveRole = remember(userProfile, devBypassRole) {
        devBypassRole ?: userProfile?.toUserRole() ?: UserRole.BDE
    }

    LaunchedEffect(effectiveRole) {
        repository.switchRole(effectiveRole)
    }

    var currentTab by remember { mutableStateOf(HBXTab.HOME) }
    var currentSubScreen by remember { mutableStateOf(AppSubScreen.NONE) }

    var showRoleDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Authentication Gate: if not authenticated and no dev bypass, render SignInScreen
    val isAuthenticated = currentUser != null || devBypassRole != null

    if (isAuthLoading && currentUser != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LightGold, strokeWidth = 3.dp)
        }
        return
    }

    if (!isAuthenticated) {
        SignInScreen(
            authRepository = authRepository,
            onDevBypassRole = { role -> devBypassRole = role }
        )
        return
    }

    // Handle Hardware Back Button
    BackHandler(enabled = currentSubScreen != AppSubScreen.NONE || currentTab != HBXTab.HOME) {
        if (currentSubScreen != AppSubScreen.NONE) {
            currentSubScreen = AppSubScreen.NONE
        } else if (currentTab != HBXTab.HOME) {
            currentTab = HBXTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentSubScreen == AppSubScreen.NONE) {
                HBXHeader(
                    currentRole = effectiveRole,
                    onRoleClick = { showRoleDialog = true },
                    onNotificationClick = { showNotificationDialog = true }
                )
            }
        },
        bottomBar = {
            if (currentSubScreen == AppSubScreen.NONE) {
                HBXBottomNav(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                        currentSubScreen = AppSubScreen.NONE
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MainBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(
                targetState = currentSubScreen to currentTab,
                label = "screen_crossfade"
            ) { (subScreen, tab) ->
                when (subScreen) {
                    AppSubScreen.CONFIGURATOR -> {
                        QuotationConfiguratorScreen(
                            repository = repository,
                            onBack = { currentSubScreen = AppSubScreen.NONE }
                        )
                    }
                    AppSubScreen.EXECUTIVE_VIEW -> {
                        ExecutiveDashboardScreen(
                            repository = repository,
                            onBackToFieldView = { currentSubScreen = AppSubScreen.NONE }
                        )
                    }
                    AppSubScreen.PAYMENTS_VIEW -> {
                        FinancePaymentsScreen(
                            repository = repository,
                            onBack = { currentSubScreen = AppSubScreen.NONE }
                        )
                    }
                    AppSubScreen.AI_ASSISTANT_VIEW -> {
                        AiAssistantScreen(
                            repository = repository,
                            onBack = { currentSubScreen = AppSubScreen.NONE }
                        )
                    }
                    AppSubScreen.NONE -> {
                        when (tab) {
                            HBXTab.HOME -> {
                                BdeDashboardScreen(
                                    repository = repository,
                                    onNavigateToCrm = { currentTab = HBXTab.CUSTOMERS },
                                    onNavigateToSurvey = { currentTab = HBXTab.TASKS },
                                    onNavigateToProject = { currentTab = HBXTab.PROJECTS },
                                    onOpenOnboarding = { showOnboardingDialog = true },
                                    onOpenExecutiveView = { currentSubScreen = AppSubScreen.EXECUTIVE_VIEW }
                                )
                            }
                            HBXTab.CUSTOMERS -> {
                                CrmLeadsScreen(
                                    repository = repository,
                                    onNavigateToDossier = { _ ->
                                        currentSubScreen = AppSubScreen.CONFIGURATOR
                                    },
                                    onOpenOnboarding = { showOnboardingDialog = true }
                                )
                            }
                            HBXTab.TASKS -> {
                                SiteSurveyScreen(
                                    repository = repository,
                                    onContinueToConfigurator = {
                                        currentSubScreen = AppSubScreen.CONFIGURATOR
                                    }
                                )
                            }
                            HBXTab.PROJECTS -> {
                                ProjectPipelineScreen(
                                    repository = repository,
                                    onNavigateToSurvey = { currentTab = HBXTab.TASKS }
                                )
                            }
                            HBXTab.PROFILE -> {
                                AttendanceAdminScreen(
                                    repository = repository,
                                    onNavigateToPayments = { currentSubScreen = AppSubScreen.PAYMENTS_VIEW },
                                    onNavigateToAi = { currentSubScreen = AppSubScreen.AI_ASSISTANT_VIEW },
                                    onOpenRoleDialog = { showRoleDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Role Switch Dialog
    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = effectiveRole,
            onRoleSelected = { newRole ->
                devBypassRole = newRole
                repository.switchRole(newRole)
                scope.launch {
                    snackbarHostState.showSnackbar("Switched active view to: ${newRole.displayName} (${newRole.badge})")
                }
            },
            onDismiss = { showRoleDialog = false }
        )
    }

    // New Customer Onboarding Dialog
    if (showOnboardingDialog) {
        NewCustomerOnboardingDialog(
            onDismiss = { showOnboardingDialog = false },
            onSubmit = { name, phone, village, capacity, priority ->
                repository.addLead(name, phone, village, capacity, priority)
                scope.launch {
                    snackbarHostState.showSnackbar("Prospect $name registered & synchronized with UP-NEDA!")
                }
            }
        )
    }

    // Notifications Drawer
    if (showNotificationDialog) {
        NotificationDrawerDialog(
            onDismiss = { showNotificationDialog = false }
        )
    }
}
