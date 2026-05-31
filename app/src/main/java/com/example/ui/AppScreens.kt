package com.example.ui

import android.widget.Space
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PixelPurchase
import com.example.data.UserAccount
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple screen state enumerator
enum class AppScreen {
    Canvas,
    PixelArt,
    Leaderboard,
    StatsAndImpact,
    UserHistory
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppScreensContainer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val inAppNotification by viewModel.inAppNotification.collectAsState()
    
    // Default current tab inside the main app
    var currentScreen by remember { mutableStateOf(AppScreen.Canvas) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundHex)
    ) {
        if (currentUser == null) {
            AuthScreen(viewModel = viewModel)
        } else {
            // Main Responsive Scaffold containing Canvas, Leaderboard, Stats, or History
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth >= 720.dp
                
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        if (!isTablet) {
                            BottomNavigationBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { currentScreen = it }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Left-side Navigation Rail on Tablets
                        if (isTablet) {
                            NavigationRailSection(
                                currentScreen = currentScreen,
                                onScreenSelected = { currentScreen = it },
                                currentUser = currentUser!!,
                                onLogout = { viewModel.logout() }
                            )
                        }
                        
                        // Responsive Main Content Viewport
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(16.dp)
                        ) {
                            ResponsiveLayoutContent(
                                currentScreen = currentScreen,
                                isTablet = isTablet,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        
        // Dynamic Pop-up Banner Notification overlay (Milestone alerts, success logs)
        AnimatedVisibility(
            visible = inAppNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .zIndex(10f)
        ) {
            inAppNotification?.let { alertText ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceHex,
                        contentColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonGreenHex),
                    modifier = Modifier
                        .fillMaxWidth(if (LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600) 0.6f else 0.95f)
                        .testTag("in_app_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Milestone Flash Alert",
                            tint = NeonGreenHex,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = alertText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.dismissInAppNotification() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Interactive Click-to-Buy Pixel confirmation dialog with Paint Color selection!
        if (viewModel.showBuyConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showBuyConfirmationDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = NeonGreenHex,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Secure & Paint Pixel(s)?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Are you sure do you want to buy this pixel/s?\n\n" +
                                    "📍 Selected coordinates:\nOrigin: X = ${viewModel.selectedX}, Y = ${viewModel.selectedY}\n" +
                                    "📐 Dimensions: ${viewModel.selectedWidth} × ${viewModel.selectedHeight} pixels\n" +
                                    "✨ Quantity: ${viewModel.selectedWidth * viewModel.selectedHeight} pixel(s)\n" +
                                    "💵 Amount: $${String.format("%.2f", (viewModel.selectedWidth * viewModel.selectedHeight).toDouble())} USD",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        Text(
                            text = "COLOUR THEM WITH THE COLOUR YOU WANT:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyanHex
                            )
                        )

                        // Visual row of clickable palette circles
                        val palette = listOf(
                            "#D0BCFF" to "Lavender",
                            "#EFB8C8" to "Soft Rose",
                            "#CCC2DC" to "Sage Grey",
                            "#FF453A" to "Bright Red",
                            "#FF9F0A" to "Neon Orange",
                            "#FFD60A" to "Gold Yellow",
                            "#30D158" to "Lime Green",
                            "#64D2FF" to "Ocean Blue",
                            "#007AFF" to "Classic Blue",
                            "#BF5AF2" to "Vivid Purple"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palette.forEach { (hex, name) ->
                                val isSelected = viewModel.selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(android.graphics.Color.parseColor(hex)), shape = RoundedCornerShape(percent = 50))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(percent = 50)
                                        )
                                        .clickable { viewModel.selectedColorHex = hex }
                                        .testTag("color_item_$hex")
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(android.graphics.Color.parseColor(viewModel.selectedColorHex)), shape = RoundedCornerShape(percent = 50))
                            )
                            Text(
                                text = "Selected Color: ${palette.find { it.first == viewModel.selectedColorHex }?.second ?: "Custom"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White
                            )
                        }

                        // Message input in dialog
                        OutlinedTextField(
                            value = viewModel.donationMessage,
                            onValueChange = { viewModel.donationMessage = it },
                            placeholder = { Text("Write check memo/text message...", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = transparentTextFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.showBuyConfirmationDialog = false
                            viewModel.buySelectedPixels { }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreenHex,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Yes, Secure & paint!",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.showBuyConfirmationDialog = false }
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray
                        )
                    }
                },
                containerColor = DarkSurfaceHex,
                tonalElevation = 6.dp
            )
        }
    }
}

// ----------------- Authentication Screen Component -----------------
@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    val authError by viewModel.authError.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundHex)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background brush accents
        Box(
            modifier = Modifier
                .size(400.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyanHex.copy(alpha = 0.12f), Color.Transparent),
                            center = center,
                            radius = size.width / 2
                        )
                    )
                }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHex),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 480.dp)
                .testTag("auth_card")
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Futuristic header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .drawBehind {
                            drawRect(
                                color = NeonGreenHex,
                                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                size = Size(8.dp.toPx(), 8.dp.toPx())
                            )
                            drawRect(
                                color = NeonPinkHex,
                                topLeft = Offset(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                                size = Size(8.dp.toPx(), 8.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Key Lock Icon",
                        tint = NeonGreenHex,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isRegisterMode) "SECURE REGISTRATION" else "SECURE DONOR PORTAL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = if (isRegisterMode) "Enroll to access ledger & record pixel orders" else "Acknowledge session context to view logs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Error Banner
                authError?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF33101E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = err,
                            color = NeonPinkHex,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Inputs
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                        maxLines = 1,
                        singleLine = true,
                        colors = transparentTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("register_name_input")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearAuthError() },
                    label = { Text("E-mail Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    maxLines = 1,
                    singleLine = true,
                    colors = transparentTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearAuthError() },
                    label = { Text("Secure Password") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    maxLines = 1,
                    singleLine = true,
                    colors = transparentTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("password_input")
                )

                // Submit Button
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            viewModel.register(email, password, displayName)
                        } else {
                            viewModel.login(email, password)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreenHex,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_button")
                ) {
                    Text(
                        text = if (isRegisterMode) "[ REGISTER ENTRANT ]" else "[ AUTHENTICATE CHECKOUT ]",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Flow Link Text
                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        viewModel.clearAuthError()
                    },
                    modifier = Modifier.testTag("toggle_auth_mode")
                ) {
                    Text(
                        text = if (isRegisterMode) "Already have registered session? Authenticate credentials"
                               else "Establish new secure entrance keys (Register)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyanHex
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ----------------- Responsive Content Manager -----------------
@Composable
fun ResponsiveLayoutContent(
    currentScreen: AppScreen,
    isTablet: Boolean,
    viewModel: MainViewModel
) {
    if (isTablet && currentScreen == AppScreen.Canvas) {
        // Multi-pane Wide Layout specifically for active Canvas and Purchase flow!
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                CanvasCanvasView(viewModel = viewModel, isTablet = true)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(DarkSurfaceHex, shape = RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                BuyPixelsPanel(viewModel = viewModel)
            }
        }
    } else {
        // Standard Screen Selection Navigation Flow
        when (currentScreen) {
            AppScreen.Canvas -> {
                // In Mobile, Canvas requires an explicit modal or scrolled stack to view purchases 
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CanvasCanvasView(viewModel = viewModel, isTablet = false)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceHex, shape = RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        BuyPixelsPanel(viewModel = viewModel)
                    }
                }
            }
            AppScreen.PixelArt -> {
                PixelArtScreen(viewModel = viewModel)
            }
            AppScreen.Leaderboard -> {
                LeaderboardScreen(viewModel = viewModel, isTablet = isTablet)
            }
            AppScreen.StatsAndImpact -> {
                StatsAndImpactScreen(viewModel = viewModel, isTablet = isTablet)
            }
            AppScreen.UserHistory -> {
                UserHistoryScreen(viewModel = viewModel)
            }
        }
    }
}

// ----------------- Canvas View Visualizer -----------------
@Composable
fun CanvasCanvasView(viewModel: MainViewModel, isTablet: Boolean) {
    val purchases by viewModel.allPurchases.collectAsState()
    val totalSold by viewModel.totalPixelsSold.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHex),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header stats segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "10 MILLION PIXELS BOARD",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Real-time coordinate ownership map (4000 × 2500)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Secured: $totalSold px",
                        color = NeonGreenHex,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful custom interactive drawn map canvas representing the grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF020306))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pixel_grid_canvas")
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val canvasW = size.width.toFloat()
                                val canvasH = size.height.toFloat()
                                if (canvasW > 0 && canvasH > 0) {
                                    viewModel.selectedX = ((offset.x / canvasW) * 4000f).toInt().coerceIn(0, 3999)
                                    viewModel.selectedY = ((offset.y / canvasH) * 2500f).toInt().coerceIn(0, 2499)
                                    viewModel.showBuyConfirmationDialog = true
                                }
                            }
                        }
                ) {
                    val canvasW = size.width
                    val canvasH = size.height
                    
                    // Coordinates projection scaling. Map 4000x2500 grid to actual physically drawn canvas pixels
                    val scaleX = canvasW / 4000f
                    val scaleY = canvasH / 2500f

                    // Draw subtle coordinate guidelines (Asymmetric grids as mandated)
                    val strokeEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                    
                    // Drawing crosshairs grid
                    for (i in 1..3) {
                        val posX = (canvasW / 4) * i
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(posX, 0f),
                            end = Offset(posX, canvasH),
                            strokeWidth = 1f,
                            pathEffect = strokeEffect
                        )
                        val posY = (canvasH / 4) * i
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, posY),
                            end = Offset(canvasW, posY),
                            strokeWidth = 1f,
                            pathEffect = strokeEffect
                        )
                    }

                    // Render registered community purchases to glowing rectangles!
                    purchases.forEach { purchase ->
                        // Dynamically resolve custom chosen color hex selected by user during purchase
                        val glowingColor = try {
                            Color(android.graphics.Color.parseColor(purchase.colorHex)).copy(alpha = 0.65f)
                        } catch (e: Exception) {
                            NeonGreenHex.copy(alpha = 0.5f)
                        }

                        // Project database coordinates to canvas scale
                        val pX = purchase.startX * scaleX
                        val pY = purchase.startY * scaleY
                        val pW = (purchase.width * scaleX).coerceAtLeast(6f)
                        val pH = (purchase.height * scaleY).coerceAtLeast(6f)

                        drawRect(
                            color = glowingColor,
                            topLeft = Offset(pX, pY),
                            size = Size(pW, pH)
                        )
                        // Thin glowing outline
                        drawRect(
                            color = glowingColor.copy(alpha = 0.8f),
                            topLeft = Offset(pX, pY),
                            size = Size(pW, pH),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Render dynamic target selection boundary box in bold neon highlighter
                    val targetX = viewModel.selectedX * scaleX
                    val targetY = viewModel.selectedY * scaleY
                    val targetW = (viewModel.selectedWidth * scaleX).coerceAtLeast(8f)
                    val targetH = (viewModel.selectedHeight * scaleY).coerceAtLeast(8f)

                    // Draw glowing highlight overlay
                    drawRect(
                        color = Color(0xFFFF007F).copy(alpha = 0.15f),
                        topLeft = Offset(targetX, targetY),
                        size = Size(targetW, targetH)
                    )
                    drawRect(
                        color = Color(0xFFFF007F),
                        topLeft = Offset(targetX, targetY),
                        size = Size(targetW, targetH),
                        style = Stroke(width = 2f)
                    )
                    
                    // Draw tiny pointer dot for targeting
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(targetX + targetW/2, targetY + targetH/2)
                    )
                }

                // HUD overlay in canvas corner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Target Area Selection",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    )
                    Text(
                        text = "X:${viewModel.selectedX} Y:${viewModel.selectedY} | Dim:${viewModel.selectedWidth}x${viewModel.selectedHeight}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selection Controls (Responsive sliders)
            Text(
                text = "MANUALLY STREAM COORDINATES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // X Slider
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Origin X", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.White))
                        Text("${viewModel.selectedX}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonCyanHex, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = viewModel.selectedX.toFloat(),
                        onValueChange = { viewModel.selectedX = it.toInt() },
                        valueRange = 0f..4000f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyanHex,
                            activeTrackColor = NeonCyanHex.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.testTag("slider_x")
                    )
                }

                // Y Slider
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Origin Y", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.White))
                        Text("${viewModel.selectedY}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonPinkHex, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = viewModel.selectedY.toFloat(),
                        onValueChange = { viewModel.selectedY = it.toInt() },
                        valueRange = 0f..2500f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPinkHex,
                            activeTrackColor = NeonPinkHex.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.testTag("slider_y")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Width select
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Width (px)", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.White))
                        Text("${viewModel.selectedWidth}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonGreenHex, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = viewModel.selectedWidth.toFloat(),
                        onValueChange = { viewModel.selectedWidth = it.toInt() },
                        valueRange = 1f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreenHex,
                            activeTrackColor = NeonGreenHex.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.testTag("slider_width")
                    )
                }

                // Height select
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Height (px)", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.White))
                        Text("${viewModel.selectedHeight}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonGreenHex, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = viewModel.selectedHeight.toFloat(),
                        onValueChange = { viewModel.selectedHeight = it.toInt() },
                        valueRange = 1f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreenHex,
                            activeTrackColor = NeonGreenHex.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.testTag("slider_height")
                    )
                }
            }
        }
    }
}

// ----------------- Buy Pixels Screen Panel (Form + Checkout) -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyPixelsPanel(viewModel: MainViewModel) {
    val pixelCount = viewModel.selectedWidth * viewModel.selectedHeight
    val totalCost = pixelCount * 1.0 // $1 per pixel
    
    var showCheckoutDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "SECURE PIXELS TRANSACTION",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyanHex,
                letterSpacing = 1.5.sp
            )
        )
        
        Spacer(modifier = Modifier.height(14.dp))

        // Transaction Overview receipt card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reserve Quantity", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray))
                    Text("$pixelCount Pixels", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold))
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.04f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cost Rate", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray))
                    Text("$1.00 USD / pixel", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White))
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.04f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Withdrawal Account", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray))
                    Text(viewModel.checkoutBankName, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold))
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.04f))
                
                // Paypal destination requirement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recipient PayPal", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OfflineShare, contentDescription = null, tint = NeonGreenHex, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("waterempire0@gmail.com", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = NeonGreenHex, fontWeight = FontWeight.Bold))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.04f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Transaction Value", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold))
                    Text("$${String.format("%,.2f", totalCost)}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = NeonGreenHex, fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SELECT PIXEL PAINT COLOUR",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Visual Row of selectable palette circles in sliders panel
        val sliderPalette = listOf(
            "#D0BCFF" to "Lavender",
            "#EFB8C8" to "Soft Rose",
            "#CCC2DC" to "Sage Grey",
            "#FF453A" to "Red",
            "#FF9F0A" to "Orange",
            "#FFD60A" to "Gold",
            "#30D158" to "Lime",
            "#64D2FF" to "Ocean",
            "#007AFF" to "Classic",
            "#BF5AF2" to "Vivid"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sliderPalette.forEach { (hex, name) ->
                val isSelected = viewModel.selectedColorHex == hex
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), shape = RoundedCornerShape(percent = 50))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .clickable { viewModel.selectedColorHex = hex }
                        .testTag("color_picker_slider_$hex")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message input
        Text(
            text = "PUBLIC LEDGER MESSAGE (Required)",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = viewModel.donationMessage,
            onValueChange = { viewModel.donationMessage = it },
            placeholder = { Text("Write something (e.g., 'This is my first goal!')", color = Color.DarkGray, fontSize = 13.sp) },
            singleLine = false,
            maxLines = 2,
            colors = transparentTextFieldColors(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("donation_message_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Checkout routing details toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCheckoutDetails = !showCheckoutDetails }
                .padding(vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (showCheckoutDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = NeonCyanHex,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Show billing & bank-routing checkout details",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyanHex
                )
            )
        }

        AnimatedVisibility(visible = showCheckoutDetails) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.checkoutBankName,
                    onValueChange = { viewModel.checkoutBankName = it },
                    label = { Text("Bank Organization") },
                    colors = transparentTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.checkoutAccountNumber,
                        onValueChange = { viewModel.checkoutAccountNumber = it },
                        label = { Text("Account/Card No") },
                        colors = transparentTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.checkoutRoutingCode,
                        onValueChange = { viewModel.checkoutRoutingCode = it },
                        label = { Text("Routing Code") },
                        colors = transparentTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large CTA Transaction checkout button
        Button(
            onClick = {
                viewModel.buySelectedPixels {
                    // Success callback
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreenHex,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("buy_pixels_button")
        ) {
            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "[ CONFIRM WITHDRAWAL OF $${String.format("%.2f", totalCost)} ]",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "⚠️ Real withdrawal: Coordinates are locked inside local secured SQlite instance on execution. Safe sandbox demonstration environment.",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color.LightGray.copy(alpha = 0.5f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ----------------- Leaderboard Screen Component -----------------
@Composable
fun LeaderboardScreen(viewModel: MainViewModel, isTablet: Boolean) {
    val purchases by viewModel.allPurchases.collectAsState()
    
    // Group purchases by users to compute total dollar leaders
    val aggregatedLeaderboard = remember(purchases) {
        purchases.groupBy { it.userEmail }
            .map { (email, userPurchasesList) ->
                val userName = userPurchasesList.firstOrNull()?.userName ?: "Anonymous"
                val totalSpent = userPurchasesList.sumOf { it.amountUsd }
                val totalPixels = userPurchasesList.sumOf { it.pixelCount }
                val lastMsg = userPurchasesList.maxByOrNull { it.timestamp }?.message ?: ""
                val lastDate = userPurchasesList.maxByOrNull { it.timestamp }?.timestamp ?: 0L
                
                Triple(userName, totalSpent, lastMsg)
            }
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700), // Pure Gold
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "GLOBAL FOUNDER LEADERBOARD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Top digital canvas investors sorted by pixels secured",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (aggregatedLeaderboard.isEmpty()) {
            EmptyStateView(
                title = "No leaderboard entries",
                description = "Secure your first pixel chunk to top the leader list!"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("leaderboard_list")
            ) {
                items(aggregatedLeaderboard.size) { index ->
                    val entry = aggregatedLeaderboard[index]
                    val donorName = entry.first
                    val spent = entry.second
                    val message = entry.third
                    
                    // Style first three spots uniquely
                    val badgeColor = when (index) {
                        0 -> Color(0xFFFFD700) // Gold
                        1 -> Color(0xFFC0C0C0) // Silver
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> Color.White.copy(alpha = 0.2f)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHex),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (index == 0) NeonGreenHex.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.04f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, badgeColor), shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (index < 3) badgeColor else Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))

                            // Name + Message
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = donorName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                if (message.isNotBlank()) {
                                    Text(
                                        text = "\"$message\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.LightGray.copy(alpha = 0.7f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Amount Paid
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format("%,.2f", spent)}",
                                    color = NeonGreenHex,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${spent.toInt()} px",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- Stats & Impact Highlights Screen -----------------
@Composable
fun StatsAndImpactScreen(viewModel: MainViewModel, isTablet: Boolean) {
    val totalMoney by viewModel.totalMoneyRaised.collectAsState()
    val totalSold by viewModel.totalPixelsSold.collectAsState()
    val mySpend by viewModel.userSpendUsd.collectAsState()

    val currentProgressPercent = (totalSold.toFloat() / 10_000_000f) * 100f

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("stats_scene")
    ) {
        // Broad Overview Row
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = NeonCyanHex,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "TOTAL VALUE SECURED",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Milestones and direct support statistics",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    )
                }
            }
        }

        // Stats Display Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHex),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "GLOBAL COLLATERAL POOL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Total Raised
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$${String.format("%,.2f", totalMoney)}", // Starting at exactly $0, increasing as people donate
                                color = NeonGreenHex,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            )
                            Text(
                                text = "Total Raised (USD)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            )
                        }

                        // My Spends Highlighted separately as requested 
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$${String.format("%,.2f", mySpend)}",
                                color = NeonCyanHex,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            )
                            Text(
                                text = "Your Secure Spent",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress boundary Meter towards 10M mark
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Canvas Allocation Reach",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White)
                        )
                        Text(
                            text = "${String.format("%.5f", currentProgressPercent)}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = NeonGreenHex, fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = (totalSold / 10_000_000f),
                        color = NeonGreenHex,
                        trackColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Section header for milestones progress
        item {
            Text(
                text = "COMMUNITY PROGRESS MILESTONES & IMPACTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // List milestones dynamically
        items(viewModel.milestones) { milestone ->
            val isUnlocked = totalMoney >= milestone.targetAmount
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) Color(0xFF0C1215) else DarkSurfaceHex
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isUnlocked) NeonCyanHex.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.03f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isUnlocked) NeonCyanHex.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.LockClock,
                            contentDescription = null,
                            tint = if (isUnlocked) NeonCyanHex else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = milestone.label,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = milestone.description,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "$${String.format("%,.0f", milestone.targetAmount)}",
                        color = if (isUnlocked) NeonGreenHex else Color.LightGray.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// ----------------- User Secure Transaction History -----------------
@Composable
fun UserHistoryScreen(viewModel: MainViewModel) {
    val purchases by viewModel.userPurchases.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = NeonPinkHex,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "YOUR SECURE BILLING RECEPTACLE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Acknowledge receipts, bank origins and PayPal endpoints",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (purchases.isEmpty()) {
            EmptyStateView(
                title = "No personal transactions found",
                description = "Authenticated purchases get recorded permanently inside the encrypted SQLite standard flow model."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("user_history_list")
            ) {
                items(purchases) { purchase ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHex),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Row 1: Coordinates and Cost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Block (${purchase.startX}, ${purchase.startY}) @ ${purchase.width}x${purchase.height}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "$${String.format("%,.2f", purchase.amountUsd)}",
                                    color = NeonGreenHex,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Public message box content
                            Text(
                                text = "Public Box: \"${purchase.message}\"",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.04f))

                            // Administrative Transaction receipt strings (Bank origin and Paypal receipt)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Origin",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
                                    )
                                    Text(
                                        text = purchase.paymentBankSnippet,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.LightGray, fontSize = 11.sp)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "PayPal Receipt ID",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
                                    )
                                    Text(
                                        text = purchase.paymentPaypalReceipt,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = NeonCyanHex, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Share Achievement / Post Social Action Button
                            OutlinedButton(
                                onClick = { viewModel.sharePurchaseAchievement(context, purchase) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = NeonCyanHex
                                ),
                                border = BorderStroke(1.dp, NeonCyanHex.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "POST ACHIEVEMENT ONLINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- Helper UI: Empty State View -----------------
@Composable
fun EmptyStateView(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ----------------- Bottom Navigation Bar (Mobile) -----------------
@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurfaceHex,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.Canvas,
            onClick = { onScreenSelected(AppScreen.Canvas) },
            icon = { Icon(Icons.Default.GridView, contentDescription = "Canvas") },
            label = { Text("Grid Board", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
            colors = customNavigationItemColors()
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.PixelArt,
            onClick = { onScreenSelected(AppScreen.PixelArt) },
            icon = { Icon(Icons.Default.Palette, contentDescription = "Pixel Art") },
            label = { Text("Pixel Art", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
            colors = customNavigationItemColors()
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.Leaderboard,
            onClick = { onScreenSelected(AppScreen.Leaderboard) },
            icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard") },
            label = { Text("Founders", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
            colors = customNavigationItemColors()
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.StatsAndImpact,
            onClick = { onScreenSelected(AppScreen.StatsAndImpact) },
            icon = { Icon(Icons.Default.MilitaryTech, contentDescription = "Stats") },
            label = { Text("Impact", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
            colors = customNavigationItemColors()
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.UserHistory,
            onClick = { onScreenSelected(AppScreen.UserHistory) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Receipts") },
            label = { Text("Receipts", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
            colors = customNavigationItemColors()
        )
    }
}

// ----------------- Left Navigation Rail (Tablet) -----------------
@Composable
fun NavigationRailSection(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    currentUser: UserAccount,
    onLogout: () -> Unit
) {
    NavigationRail(
        containerColor = DarkSurfaceHex,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonGreenHex.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, NeonGreenHex), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser.displayName.firstOrNull()?.toString()?.uppercase() ?: "U",
                        color = NeonGreenHex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = currentUser.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(top = 8.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NavigationRailItem(
                    selected = currentScreen == AppScreen.Canvas,
                    onClick = { onScreenSelected(AppScreen.Canvas) },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Canvas") },
                    label = { Text("Grid Board", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    colors = customNavigationRailItemColors()
                )
                NavigationRailItem(
                    selected = currentScreen == AppScreen.PixelArt,
                    onClick = { onScreenSelected(AppScreen.PixelArt) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Pixel Art") },
                    label = { Text("Pixel Art", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    colors = customNavigationRailItemColors()
                )
                NavigationRailItem(
                    selected = currentScreen == AppScreen.Leaderboard,
                    onClick = { onScreenSelected(AppScreen.Leaderboard) },
                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard") },
                    label = { Text("Founders", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    colors = customNavigationRailItemColors()
                )
                NavigationRailItem(
                    selected = currentScreen == AppScreen.StatsAndImpact,
                    onClick = { onScreenSelected(AppScreen.StatsAndImpact) },
                    icon = { Icon(Icons.Default.MilitaryTech, contentDescription = "Stats") },
                    label = { Text("Impact", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    colors = customNavigationRailItemColors()
                )
                NavigationRailItem(
                    selected = currentScreen == AppScreen.UserHistory,
                    onClick = { onScreenSelected(AppScreen.UserHistory) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Receipts") },
                    label = { Text("Receipts", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    colors = customNavigationRailItemColors()
                )
            }

            // Power off lock session
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Lock Session",
                    tint = NeonPinkHex
                )
            }
        }
    }
}

// ----------------- Style Customizers -----------------
@Composable
fun transparentTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyanHex,
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    focusedLabelColor = NeonCyanHex,
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
)

@Composable
fun customNavigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.Black,
    selectedTextColor = NeonCyanHex,
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray,
    indicatorColor = NeonCyanHex
)

@Composable
fun customNavigationRailItemColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = Color.Black,
    selectedTextColor = NeonCyanHex,
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray,
    indicatorColor = NeonCyanHex
)

// ----------------- Full Immersive Interactive Pixel Art Screen -----------------
@Composable
fun PixelArtScreen(viewModel: MainViewModel) {
    val purchases by viewModel.allPurchases.collectAsState()
    
    // Pinch-to-Zoom & Drag-to-Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Dynamic size registration elements
    var canvasWidth by remember { mutableStateOf(1f) }
    var canvasHeight by remember { mutableStateOf(1f) }
    
    // Statically selected item inside viewport
    var highlightedPurchase by remember { mutableStateOf<PixelPurchase?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pixel_art_screen_container")
    ) {
        // Immersive high-contrast header section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = NeonCyanHex,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PIXEL ART WORKSPACE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Double finger to Zoom, drag to Pan. Tap space to inspect or paint",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                )
            }
            
            // Floating reset coordinates option
            if (scale != 1f || offset != Offset.Zero) {
                OutlinedButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        highlightedPurchase = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreenHex),
                    border = BorderStroke(1.dp, NeonGreenHex.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterCenterFocus,
                        contentDescription = "Recenter",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Parent Box with Tap Gestures detector mapping un-transformed coordinates
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF020305))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp))
                .pointerInput(purchases) {
                    detectTapGestures { tapOffset ->
                        if (canvasWidth > 1f && canvasHeight > 1f) {
                            val viewCenterX = canvasWidth / 2f
                            val viewCenterY = canvasHeight / 2f
                            
                            // Solve transformation matrix equation backwards
                            val xInCanvas = viewCenterX + (tapOffset.x - offset.x - viewCenterX) / scale
                            val yInCanvas = viewCenterY + (tapOffset.y - offset.y - viewCenterY) / scale
                            
                            // Map coordinates to real logical 4000x2500 canvas standard grid
                            val gridX = ((xInCanvas / canvasWidth) * 4000f).toInt().coerceIn(0, 3999)
                            val gridY = ((yInCanvas / canvasHeight) * 2500f).toInt().coerceIn(0, 2499)
                            
                            // Search matching secured box
                            val match = purchases.firstOrNull { p ->
                                gridX >= p.startX && gridX < p.startX + p.width &&
                                gridY >= p.startY && gridY < p.startY + p.height
                            }
                            
                            if (match != null) {
                                highlightedPurchase = match
                            } else {
                                highlightedPurchase = null
                                // Set coordinates and prompt confirming purchase
                                viewModel.selectedX = gridX
                                viewModel.selectedY = gridY
                                viewModel.showBuyConfirmationDialog = true
                            }
                        }
                    }
                }
        ) {
            // Interactive scaling Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        canvasWidth = it.width.toFloat().coerceAtLeast(1f)
                        canvasHeight = it.height.toFloat().coerceAtLeast(1f)
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        transformOrigin = TransformOrigin.Center
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 35f)
                            offset += pan
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height
                
                val scaleX = canvasW / 4000f
                val scaleY = canvasH / 2500f
                
                // Draw coordinate guides dashes lines
                val strokeEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f), 0f)
                for (i in 1..4) {
                    val posX = (canvasW / 5) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(posX, 0f),
                        end = Offset(posX, canvasH),
                        strokeWidth = 1f,
                        pathEffect = strokeEffect
                    )
                    val posY = (canvasH / 5) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(0f, posY),
                        end = Offset(canvasW, posY),
                        strokeWidth = 1f,
                        pathEffect = strokeEffect
                    )
                }

                // Render registered pixel art blocks
                purchases.forEach { purchase ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(purchase.colorHex)).copy(alpha = 0.75f)
                    } catch (e: Exception) {
                        NeonGreenHex.copy(alpha = 0.60f)
                    }
                    
                    val pX = purchase.startX * scaleX
                    val pY = purchase.startY * scaleY
                    val pW = (purchase.width * scaleX).coerceAtLeast(4f)
                    val pH = (purchase.height * scaleY).coerceAtLeast(4f)
                    
                    // Draw fill color rectangle
                    drawRect(
                        color = color,
                        topLeft = Offset(pX, pY),
                        size = Size(pW, pH)
                    )
                    // Draw outer border outline
                    drawRect(
                        color = color.copy(alpha = 0.95f),
                        topLeft = Offset(pX, pY),
                        size = Size(pW, pH),
                        style = Stroke(width = 0.8f)
                    )
                }

                // Draw glowing crosshair/marker around active touch highlighted rectangle
                highlightedPurchase?.let { p ->
                    val pX = p.startX * scaleX
                    val pY = p.startY * scaleY
                    val pW = (p.width * scaleX).coerceAtLeast(8f)
                    val pH = (p.height * scaleY).coerceAtLeast(8f)
                    
                    drawRect(
                        color = Color(0xFFFF007F).copy(alpha = 0.20f),
                        topLeft = Offset(pX, pY),
                        size = Size(pW, pH)
                    )
                    
                    drawRect(
                        color = Color(0xFFFF007F),
                        topLeft = Offset(pX, pY),
                        size = Size(pW, pH),
                        style = Stroke(width = 2.5f)
                    )
                    
                    // Draw glowing intersection circle helper
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(pX + pW / 2f, pY + pH / 2f)
                    )
                }
            }

            // HUD Viewport card details overlay
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "VIEWPORT HUD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyanHex,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scale: ${String.format("%.1f", scale)}x",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "Offset: (${offset.x.toInt()}, ${offset.y.toInt()})",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                }
            }

            // Floating purchase info Card overlay at the bottom
            androidx.compose.animation.AnimatedVisibility(
                visible = highlightedPurchase != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                highlightedPurchase?.let { p ->
                    val customTone = try {
                        Color(android.graphics.Color.parseColor(p.colorHex))
                    } catch (e: Exception) {
                        NeonGreenHex
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHex.copy(alpha = 0.96f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, customTone),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                            .size(12.dp)
                                            .background(customTone, shape = RoundedCornerShape(percent = 50))
                                    )
                                    Text(
                                        text = p.userName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    )
                                }
                                
                                IconButton(
                                    onClick = { highlightedPurchase = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "📍 Coord: (${p.startX}, ${p.startY}) | Size: ${p.width}x${p.height} (${p.pixelCount} px)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.LightGray
                                )
                            )
                            
                            if (p.message.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${p.message}\"",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

