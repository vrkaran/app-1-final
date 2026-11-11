package com.example.ui.screens

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
import com.example.model.UserRole
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("GOOGLE") } // GOOGLE, PHONE, DEV

    // Phone Auth inputs
    var phoneNumber by remember { mutableStateOf("9839144210") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }

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

                    // Primary Google Sign-In Button (Mandated by Firebase Skill)
                    Button(
                        onClick = {
                            isSigningIn = true
                            errorMessage = null
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
                        enabled = !isSigningIn,
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
                        Text("  ENTERPRISE OTP  ", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceContainerLow)
                    }

                    // Phone Number / Employee OTP Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Mobile Phone (+91)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryNavy) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input")
                    )

                    if (otpSent) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("6-Digit OTP Code") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (!otpSent) {
                                otpSent = true
                                errorMessage = null
                            } else {
                                isSigningIn = true
                                // Simulated OTP token verification falling back to authenticated state
                                scope.launch {
                                    kotlinx.coroutines.delay(600)
                                    isSigningIn = false
                                    onDevBypassRole(UserRole.BDE)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (!otpSent) "Request OTP Code" else "Verify & Sign In", fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    }

                    // Debug Developer Bypass (Visible in DEBUG builds for previewing all 10 roles)
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
