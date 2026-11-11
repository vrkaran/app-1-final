package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.example.data.AuthRepository
import com.example.data.PhoneVerificationEvent
import com.example.model.UserRole
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(
    authRepository: AuthRepository,
    onDevBypassRole: (UserRole) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Phone Auth inputs
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var resendCooldown by remember { mutableIntStateOf(0) }

    // Resend countdown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo & Subtitle
            Image(
                painter = painterResource(id = R.drawable.ic_hbx_logo_white),
                contentDescription = "HBX Solar Logo",
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerHigh.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "ENTERPRISE SOLAR OPERATING SYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightGold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Authentication Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sign In to HBX Cloud",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    Text(
                        text = "Authorized access for Discom field staff, project engineers, and administrative managers.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    // Error Banner if any
                    errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusError.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                                Text(text = error, color = StatusError, fontSize = 12.sp)
                            }
                        }
                    }

                    // Info Banner if any
                    infoMessage?.let { info ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusSuccess.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                Text(text = info, color = StatusSuccess, fontSize = 12.sp)
                            }
                        }
                    }

                    // Primary Google Sign-In Button (Mandated by Firebase Skill)
                    Button(
                        onClick = {
                            isSigningIn = true
                            errorMessage = null
                            infoMessage = null
                            scope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val serverClientId = context.getString(R.string.default_web_client_id)
                                    val googleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleOption)
                                        .build()

                                    val response = credentialManager.getCredential(context = context, request = request)
                                    val cred = response.credential
                                    if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleTokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                                        val idToken = googleTokenCred.idToken
                                        val result = authRepository.signInWithGoogleCredential(idToken)
                                        if (result.isFailure) {
                                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed"
                                        }
                                    } else {
                                        errorMessage = "Unexpected credential format received"
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    Log.w(TAG, "Sign-in was cancelled by user.", e)
                                    errorMessage = "Sign-in cancelled. Please select an account."
                                } catch (e: Exception) {
                                    Log.e(TAG, "Sign-in error", e)
                                    errorMessage = e.localizedMessage ?: "Authentication failed"
                                } finally {
                                    isSigningIn = false
                                }
                            }
                        },
                        enabled = !isSigningIn && !isSendingOtp,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = LightGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Authenticating...")
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = LightGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceContainerLow)
                        Text("  ENTERPRISE OTP (+91)  ", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceContainerLow)
                    }

                    // Phone Number / Employee OTP Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            // Digits only, max 10
                            phoneNumber = input.filter { it.isDigit() }.take(10)
                        },
                        label = { Text("Mobile Phone (+91)") },
                        placeholder = { Text("9876543210") },
                        prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = PrimaryNavy) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryNavy) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = !isSigningIn && !isSendingOtp && verificationId == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input")
                    )

                    // Step 2: OTP Entry Field (Once SMS has been dispatched)
                    if (verificationId != null) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input ->
                                otpCode = input.filter { it.isDigit() }.take(6)
                            },
                            label = { Text("6-Digit SMS OTP Code") },
                            placeholder = { Text("123456") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input")
                        )

                        // Resend Countdown Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    verificationId = null
                                    otpCode = ""
                                    infoMessage = null
                                    errorMessage = null
                                }
                            ) {
                                Text("Change Number", fontSize = 12.sp, color = TextSecondary)
                            }

                            TextButton(
                                onClick = {
                                    if (resendCooldown == 0 && context is Activity) {
                                        errorMessage = null
                                        infoMessage = "Resending OTP code..."
                                        resendCooldown = 60
                                        authRepository.sendPhoneOtp(
                                            activity = context,
                                            normalizedPhoneNumber = "+91$phoneNumber",
                                            resendToken = resendToken
                                        ) { event ->
                                            when (event) {
                                                is PhoneVerificationEvent.CodeSent -> {
                                                    verificationId = event.verificationId
                                                    resendToken = event.token
                                                    infoMessage = "SMS code resent to +91 $phoneNumber"
                                                }
                                                is PhoneVerificationEvent.Failed -> {
                                                    errorMessage = event.error
                                                    infoMessage = null
                                                    resendCooldown = 0
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                },
                                enabled = resendCooldown == 0 && !isSendingOtp && !isSigningIn
                            ) {
                                Text(
                                    text = if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend OTP",
                                    fontSize = 12.sp,
                                    color = if (resendCooldown > 0) TextSecondary else PrimaryNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Action Button (Send OTP or Verify & Sign In)
                    Button(
                        onClick = {
                            errorMessage = null
                            infoMessage = null

                            if (verificationId == null) {
                                // Validate 10-digit Indian mobile number
                                if (phoneNumber.length != 10) {
                                    errorMessage = "Please enter a valid 10-digit Indian mobile number."
                                    return@Button
                                }

                                val activity = context as? Activity
                                if (activity == null) {
                                    errorMessage = "Internal error: Activity context not available for phone verification."
                                    return@Button
                                }

                                isSendingOtp = true
                                resendCooldown = 60
                                val normalizedNumber = "+91$phoneNumber"

                                authRepository.sendPhoneOtp(
                                    activity = activity,
                                    normalizedPhoneNumber = normalizedNumber,
                                    resendToken = resendToken
                                ) { event ->
                                    when (event) {
                                        is PhoneVerificationEvent.Sending -> {
                                            isSendingOtp = true
                                        }
                                        is PhoneVerificationEvent.CodeSent -> {
                                            isSendingOtp = false
                                            verificationId = event.verificationId
                                            resendToken = event.token
                                            infoMessage = "SMS code sent to +91 $phoneNumber"
                                        }
                                        is PhoneVerificationEvent.AutoVerified -> {
                                            isSendingOtp = false
                                            // Handle instant auto-retrieval
                                            scope.launch {
                                                isSigningIn = true
                                                val credSms = event.credential.smsCode ?: ""
                                                val verId = verificationId ?: ""
                                                if (credSms.isNotBlank() && verId.isNotBlank()) {
                                                    authRepository.verifyOtpAndSignIn(verId, credSms)
                                                }
                                                isSigningIn = false
                                            }
                                        }
                                        is PhoneVerificationEvent.Failed -> {
                                            isSendingOtp = false
                                            errorMessage = event.error
                                            resendCooldown = 0
                                        }
                                        is PhoneVerificationEvent.Idle -> {
                                            isSendingOtp = false
                                        }
                                    }
                                }
                            } else {
                                // Step 2: Verify 6-digit OTP
                                if (otpCode.length != 6) {
                                    errorMessage = "Please enter the complete 6-digit OTP code."
                                    return@Button
                                }

                                isSigningIn = true
                                scope.launch {
                                    val currentVerId = verificationId ?: return@launch
                                    val result = authRepository.verifyOtpAndSignIn(currentVerId, otpCode)
                                    isSigningIn = false
                                    if (result.isFailure) {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Invalid or expired OTP code."
                                    }
                                }
                            }
                        },
                        enabled = !isSigningIn && !isSendingOtp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("request_or_verify_otp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (verificationId == null) DeepNavy else StatusSuccess),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSendingOtp || isSigningIn) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSendingOtp) "Requesting SMS..." else "Verifying Code...")
                        } else {
                            Icon(
                                imageVector = if (verificationId == null) Icons.Default.Sms else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (verificationId == null) "Request OTP Code" else "Verify & Access HBX Cloud",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Debug Developer Bypass (Visible in DEBUG builds for previewing all 10 roles without consuming SMS quotas)
                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("DEV PREVIEW (DEBUG ONLY)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                    Text("10 Roles", fontSize = 9.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { onDevBypassRole(UserRole.SUPER_ADMIN) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        Text("Admin", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { onDevBypassRole(UserRole.BDE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        Text("BDE", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { onDevBypassRole(UserRole.CUSTOMER) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        Text("Customer", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Purvanchal Vidyut Vitaran Nigam Ltd (PUVVNL) • PM Surya Ghar Muft Bijli Yojana",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
