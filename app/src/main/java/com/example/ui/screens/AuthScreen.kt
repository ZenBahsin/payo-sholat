package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.AuthOutcome
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    isCheckingSession: Boolean = false,
    isFirebaseReady: Boolean = true,
    onEnterLocalMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
    ) {
        // Organic curved Soft Sage arch header matching the reference screen
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(275.dp)
                .align(Alignment.TopCenter)
        ) {
            val width = size.width
            val height = size.height
            val archPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.72f)
                quadraticTo(
                    width * 0.5f,
                    height * 0.16f,
                    0f,
                    height * 0.72f
                )
                close()
            }
            drawPath(path = archPath, color = SoftSage)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // Brand Mark: Geometric Mosque Dome & Crescent in Deep Sage & Warm Sand Gold
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = DeepSage.copy(alpha = 0.16f),
                        spotColor = DeepSage.copy(alpha = 0.16f)
                    )
                    .clip(CircleShape)
                    .background(WarmCream)
                    .border(2.dp, SoftSage, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_brand_logo_sage_1795596472514),
                    contentDescription = "Logo Payo Shalat",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Payo Shalat",
                style = MaterialTheme.typography.displayMedium,
                color = DeepForestCharcoal,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Asisten Ibadah Harianmu",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedOliveGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Rounded Pure White Organic Minimalist Auth Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = DeepSage.copy(alpha = 0.10f),
                        spotColor = DeepSage.copy(alpha = 0.10f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, SageBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Assalamu'alaikum",
                        style = MaterialTheme.typography.headlineLarge,
                        color = DeepForestCharcoal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isFirebaseReady) {
                            "Masuk untuk menyimpan riwayat ibadahmu dengan aman di cloud"
                        } else {
                            "Mulai catat ibadah harianmu langsung di perangkat ini"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedOliveGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Cloud Sync Benefits Strip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftIvory)
                            .border(1.dp, SageBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SyncBenefitRow(
                            icon = Icons.Default.CheckCircle,
                            text = "Ceklis 5 waktu & streak shalat harian"
                        )
                        SyncBenefitRow(
                            icon = Icons.Default.Book,
                            text = "Terakhir baca Al-Qur'an & target khatam"
                        )
                        SyncBenefitRow(
                            icon = Icons.Default.AutoAwesome,
                            text = "Hitungan tasbih digital & jadwal kota favorit"
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    if (isFirebaseReady) {
                        // Primary Deep Sage CTA -> Interactive Google Sign-In via Jetpack Credential Manager
                        Button(
                            onClick = {
                                if (isSigningIn || isCheckingSession) return@Button
                                isSigningIn = true
                                errorMessage = null
                                scope.launch {
                                    when (val outcome = GoogleAuthHelper.signInWithGoogle(context)) {
                                        is AuthOutcome.Success -> {
                                            isSigningIn = false
                                        }
                                        AuthOutcome.Cancelled -> {
                                            isSigningIn = false
                                        }
                                        is AuthOutcome.Error -> {
                                            isSigningIn = false
                                            errorMessage = outcome.message
                                        }
                                    }
                                }
                            },
                            enabled = !isSigningIn && !isCheckingSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepSage,
                                contentColor = PureWhite,
                                disabledContainerColor = DeepSage.copy(alpha = 0.6f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isSigningIn || isCheckingSession) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isCheckingSession) "Memeriksa sesi..." else "Menghubungkan akun...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(PureWhite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = DeepSage
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Lanjutkan dengan Google",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 15.sp,
                                    color = PureWhite
                                )
                            }
                        }
                    } else if (onEnterLocalMode != null) {
                        Button(
                            onClick = onEnterLocalMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("enter_local_mode_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepSage,
                                contentColor = PureWhite
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Mulai Sekarang (Mode Lokal)",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 15.sp,
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = WarmSandGoldDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isFirebaseReady) {
                                "Tersinkronisasi otomatis antar perangkat"
                            } else {
                                "Riwayat ibadahmu tersimpan langsung di perangkat ini"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedOliveGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Dengan masuk, riwayat ibadahmu tersimpan aman secara pribadi di akunmu.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedOliveGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SyncBenefitRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SoftSageLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepSage,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepForestCharcoal,
            fontWeight = FontWeight.Medium
        )
    }
}
