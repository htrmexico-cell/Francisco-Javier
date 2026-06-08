package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.data.NewsItem
import com.example.data.NotificationHelper
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground
                ) { innerPadding ->
                    MainSportsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainSportsScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedArticle by viewModel.selectedArticle.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Notification permission and preference state
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "¡Permiso de notificaciones concedido!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notificaciones desactivadas. Actívalas en los ajustes del sistema.", Toast.LENGTH_LONG).show()
        }
    }

    var showNotificationSettings by remember { mutableStateOf(false) }
    var breakingEnabled by remember { mutableStateOf(NotificationHelper.isBreakingEnabled(context)) }
    var summaryEnabled by remember { mutableStateOf(NotificationHelper.isSummaryEnabled(context)) }
    
    // Bottom Tab navigation state (Inicio, Tablas, Archivo)
    var activeTab by remember { mutableStateOf("inicio") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. SPORTS BRAND HEADER
            SportsHeader(
                isRefreshing = isRefreshing,
                onRefreshClick = {
                    viewModel.refreshNews()
                    Toast.makeText(context, "Buscando noticias del momento...", Toast.LENGTH_SHORT).show()
                },
                onSettingsClick = {
                    showNotificationSettings = true
                }
            )

            // 2. BREAKING NEWS TICKER
            val tickers = when (val state = uiState) {
                is NewsUiState.Success -> state.data.tickerHeadlines
                else -> listOf(
                    "🔴 ÚLTIMA HORA: Buscando últimas noticias deportivas...",
                    "⚽ EN VIVO: Mercado de fichajes Liga MX al instante...",
                    "🦅 TRICOLOR: Concentración total en el campamento nacional..."
                )
            }
            BreakingTicker(tickerItems = tickers)

            // Main Contents Area dependent on activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    "inicio" -> {
                        when (val state = uiState) {
                            is NewsUiState.Loading -> {
                                SkeletonFeed()
                            }
                            is NewsUiState.Success -> {
                                val filteredArticles = if (selectedCategory == "Todos") {
                                    state.data.articles
                                } else {
                                    state.data.articles.filter { it.category.equals(selectedCategory, ignoreCase = true) }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // IA Summary section
                                    item {
                                        AISummaryCard(summary = state.data.summaryOfTheDay)
                                    }

                                    // Category selector row
                                    item {
                                        CategorySelector(
                                            selectedCategory = selectedCategory,
                                            onCategorySelected = { viewModel.selectCategory(it) }
                                        )
                                    }

                                    if (filteredArticles.isEmpty()) {
                                        item {
                                            EmptyStateView(category = selectedCategory)
                                        }
                                    } else {
                                        items(filteredArticles, key = { it.id }) { article ->
                                            NewsArticleCard(
                                                article = article,
                                                onClick = { viewModel.selectArticle(article) }
                                            )
                                        }
                                    }
                                }
                            }
                            is NewsUiState.Error -> {
                                ErrorStateView(
                                    message = state.message,
                                    onRetryClick = { viewModel.loadNews() }
                                )
                            }
                        }
                    }
                    "tablas" -> {
                        LigaMXStandingsTable()
                    }
                    "archivo" -> {
                        ArchiveScreen(
                            uiState = uiState,
                            onArticleClick = { viewModel.selectArticle(it) }
                        )
                    }
                }
            }

            // Elegant Dark Navigation Bar (h-20 bg-[#0A0A0A] border-t border-white/10 flex items-center justify-around px-6 pb-2)
            BottomNavBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }

        // 3. SLIDING ARTICLE DETAIL MODAL / OVERLAY
        AnimatedVisibility(
            visible = selectedArticle != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedArticle?.let { article ->
                ArticleDetailScreen(
                    article = article,
                    onClose = { viewModel.selectArticle(null) }
                )
            }
        }

        if (showNotificationSettings) {
            NotificationSettingsDialog(
                breakingEnabled = breakingEnabled,
                onBreakingToggle = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationHelper.setBreakingEnabled(context, isChecked)
                        breakingEnabled = isChecked
                    }
                },
                summaryEnabled = summaryEnabled,
                onSummaryToggle = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationHelper.setSummaryEnabled(context, isChecked)
                        summaryEnabled = isChecked
                    }
                },
                hasPermission = hasNotificationPermission,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onSimulateBreaking = {
                    NotificationHelper.showBreakingNewsNotification(
                        context,
                        "🚨 ÚLTIMA HORA | Fichaje Caliente",
                        "¡Orbelín Pineda abrió pláticas para regresar al Rebaño Sagrado para el Apertura 2026!"
                    )
                },
                onSimulateSummary = {
                    NotificationHelper.showDailySummaryNotification(
                        context,
                        "📅 Resumen Diario GOL MX",
                        "Cruz Azul inicia entrenamientos al mando de Anselmi exigiendo refuerzos urgentes, Orbelín cerca de Chivas, y Mbappé es presentado por el Madrid luciendo el dorsal 9."
                    )
                },
                onDismiss = {
                    showNotificationSettings = false
                }
            )
        }
    }
}

// ---------------- UI COMPONENTS ----------------

@Composable
fun SportsHeader(
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Rotation animation for the refresh button
    val transition = rememberInfiniteTransition(label = "refresh_rotate")
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_rotation"
    )

    Surface(
        color = Color(0xFF050505), // bg-[#050505]
        border = BorderStroke(1.dp, Color(0x0DFFFFFF)), // border-b border-white/5 (using light white border)
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "CENTRAL DEPORTIVA",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen, // text-[#00a651]
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)) {
                            append("GOL")
                        }
                        withStyle(style = SpanStyle(color = PrimaryGreen, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)) {
                            append(" MX")
                        }
                    },
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Beautiful interactive update button, 40dp (w-10 h-10) with solid #1A1A1A background
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .testTag("refresh_button")
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)) // bg-[#1A1A1A]
                        .border(BorderStroke(1.dp, Color(0x1BFFFFFF)), CircleShape) // border-white/10
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar Noticias",
                        tint = PrimaryGreen,
                        modifier = if (isRefreshing) {
                            Modifier.rotate(rotationAngle)
                        } else {
                            Modifier
                        }
                    )
                }

                // Beautiful interactive notifications settings button, 40dp with solid #1A1A1A background
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .testTag("notification_settings_button")
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .border(BorderStroke(1.dp, Color(0x1BFFFFFF)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Configurar Notificaciones",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Divider line separator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreakingTicker(
    tickerItems: List<String>
) {
    val singleLineTicker = tickerItems.joinToString("   •   ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentRed)
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EN VIVO",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = AccentRed,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = singleLineTicker,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
        }
    }
}

@Composable
fun AISummaryCard(
    summary: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), // bg-[#121212]
        shape = RoundedCornerShape(16.dp), // rounded-2xl
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Makes the left bar align perfectly with the height
        ) {
            // Elegant green 4dp left border (border-l-4 border-[#00a651])
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(PrimaryGreen)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // AI indicator circle
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦", // Elegant Sparkle/AI icon
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESUMEN IA DEL DÍA",
                            fontFamily = SportsCondensedFamily,
                            fontWeight = FontWeight.Black,
                            color = PrimaryGreen, // text-[#00a651]
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    // Simple "VIVO" or "IA" badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryGreen.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BETA IA",
                            fontFamily = SportsCondensedFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = PrimaryGreen,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF5F5F5).copy(alpha = 0.8f), // text-white/80
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Todos", "Liga MX", "Selección Nacional", "Internacional")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val label = if (category == "Todos") "TODO" else if (category == "Selección Nacional") "SELECCIÓN" else category.uppercase()
            
            Box(
                modifier = Modifier
                    .testTag("category_chip_$category")
                    .clip(CircleShape) // rounded-full
                    .background(if (isSelected) PrimaryGreen else Color(0xFF1A1A1A)) // bg-[#00a651] vs bg-[#1A1A1A]
                    .clickable { onCategorySelected(category) }
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryGreen else Color.White.copy(alpha = 0.05f) // border-white/5
                        ), CircleShape
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black, // font-black
                    fontSize = 12.sp, // text-xs
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f), // text-white/60
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun NewsArticleCard(
    article: NewsItem,
    onClick: () -> Unit
) {
    val categoryBorderColor = when (article.category) {
        "Liga MX" -> ColorLigaMX
        "Selección Nacional" -> ColorSeleccion
        "Internacional" -> ColorInternacional
        else -> PrimaryGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), // bg-[#1A1A1A]
        shape = RoundedCornerShape(16.dp), // rounded-2xl
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), // border-white/5
        modifier = Modifier
            .testTag("news_item_card_${article.id}")
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // align left block and right details perfectly
        ) {
            // Elegant Left side thumbnail gradient block with vector sport graphics (w-24 bg-[#333] gradient)
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                categoryBorderColor.copy(alpha = 0.25f),
                                Color(0xFF050505)
                            )
                        )
                    )
                    .drawBehind {
                        // Minimal category indicator line on the left edge inside the thumbnail
                        drawRect(
                            color = categoryBorderColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(4.dp.toPx(), size.height)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                SportsGraphic(
                    type = article.graphicType,
                    color = categoryBorderColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Right side story details with compact space alignments
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = article.category.uppercase(),
                        fontFamily = SportsCondensedFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp, // text-[10px]
                        color = categoryBorderColor,
                        letterSpacing = 0.5.sp
                    )

                    if (article.isBreaking) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AccentRed)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "URGENTE",
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = article.title,
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, // text-sm
                    lineHeight = 18.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Cleaner 1-line summary that maintains descriptive depth without clutter
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Metadata: Correspondent • timeAgo
                Text(
                    text = "${article.source.uppercase()} • ${article.timeAgo}",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp, // text-[10px]
                    color = Color.White.copy(alpha = 0.4f), // text-white/40
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SportsGraphic(
    type: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val px = size.width
        val py = size.height

        when (type) {
            "ball" -> {
                val strokeW = px * 0.04f
                // Soccer ball outline
                drawCircle(color = color, radius = px / 2f, style = Stroke(width = strokeW))
                // Stylized internal lines
                drawLine(color = color, start = Offset(px * 0.15f, py * 0.5f), end = Offset(px * 0.85f, py * 0.5f), strokeWidth = strokeW * 0.75f)
                drawLine(color = color, start = Offset(px * 0.5f, py * 0.15f), end = Offset(px * 0.5f, py * 0.85f), strokeWidth = strokeW * 0.75f)
                
                // Draw a small central pentagon outline
                val path = Path().apply {
                    moveTo(px * 0.5f, py * 0.38f)
                    lineTo(px * 0.62f, py * 0.46f)
                    lineTo(px * 0.58f, py * 0.60f)
                    lineTo(px * 0.42f, py * 0.60f)
                    lineTo(px * 0.38f, py * 0.46f)
                    close()
                }
                drawPath(path, color = color, style = Stroke(width = strokeW * 0.75f))
            }
            "stadium" -> {
                val strokeW = px * 0.04f
                // Pitch fields lines
                drawRoundRect(
                    color = color,
                    size = size,
                    cornerRadius = CornerRadius(px * 0.08f, px * 0.08f),
                    style = Stroke(width = strokeW)
                )
                // Halfway line
                drawLine(color = color, start = Offset(0f, py / 2f), end = Offset(px, py / 2f), strokeWidth = strokeW)
                // Central circle
                drawCircle(color = color, center = Offset(px / 2f, py / 2f), radius = px / 5f, style = Stroke(width = strokeW * 0.75f))
                // Penalty area top
                drawRect(
                    color = color,
                    topLeft = Offset(px * 0.15f, 0f),
                    size = Size(px * 0.7f, py * 0.18f),
                    style = Stroke(width = strokeW * 0.75f)
                )
                // Penalty area bottom
                drawRect(
                    color = color,
                    topLeft = Offset(px * 0.15f, py * 0.82f),
                    size = Size(px * 0.7f, py * 0.18f),
                    style = Stroke(width = strokeW * 0.75f)
                )
            }
            "trophy" -> {
                val strokeW = px * 0.04f
                // Victory cup vector path
                val path = Path().apply {
                    // Central cup bowl
                    moveTo(px * 0.25f, py * 0.22f)
                    lineTo(px * 0.75f, py * 0.22f)
                    cubicTo(px * 0.75f, py * 0.55f, px * 0.62f, py * 0.65f, px * 0.55f, py * 0.65f)
                    lineTo(px * 0.55f, py * 0.80f)
                    lineTo(px * 0.70f, py * 0.80f)
                    lineTo(px * 0.70f, py * 0.90f)
                    lineTo(px * 0.30f, py * 0.90f)
                    lineTo(px * 0.30f, py * 0.80f)
                    lineTo(px * 0.45f, py * 0.80f)
                    lineTo(px * 0.45f, py * 0.65f)
                    cubicTo(px * 0.38f, py * 0.65f, px * 0.25f, py * 0.55f, px * 0.25f, py * 0.22f)
                }
                drawPath(path, color = color, style = Stroke(width = strokeW))

                // Cup handles
                drawArc(
                    color = color,
                    startAngle = 120f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(px * 0.13f, py * 0.26f),
                    size = Size(px * 0.18f, py * 0.26f),
                    style = Stroke(width = strokeW * 0.75f)
                )
                drawArc(
                    color = color,
                    startAngle = 300f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(px * 0.69f, py * 0.26f),
                    size = Size(px * 0.18f, py * 0.26f),
                    style = Stroke(width = strokeW * 0.75f)
                )
            }
            "jersey" -> {
                val strokeW = px * 0.04f
                // Athlete jersey path
                val path = Path().apply {
                    moveTo(px * 0.35f, py * 0.15f)
                    lineTo(px * 0.40f, py * 0.20f) // collar crease
                    lineTo(px * 0.60f, py * 0.20f)
                    lineTo(px * 0.65f, py * 0.15f)
                    lineTo(px * 0.85f, py * 0.28f)
                    lineTo(px * 0.75f, py * 0.42f)
                    lineTo(px * 0.68f, py * 0.38f)
                    lineTo(px * 0.68f, py * 0.88f)
                    lineTo(px * 0.32f, py * 0.88f)
                    lineTo(px * 0.32f, py * 0.38f)
                    lineTo(px * 0.25f, py * 0.42f)
                    lineTo(px * 0.15f, py * 0.28f)
                    close()
                }
                drawPath(path, color = color, style = Stroke(width = strokeW))
                
                // Stripes
                drawLine(color = color, start = Offset(px * 0.42f, py * 0.32f), end = Offset(px * 0.42f, py * 0.85f), strokeWidth = strokeW * 0.75f)
                drawLine(color = color, start = Offset(px * 0.50f, py * 0.32f), end = Offset(px * 0.50f, py * 0.85f), strokeWidth = strokeW * 0.75f)
                drawLine(color = color, start = Offset(px * 0.58f, py * 0.32f), end = Offset(px * 0.58f, py * 0.85f), strokeWidth = strokeW * 0.75f)
            }
            else -> { // "training" or general tactics layout
                val strokeW = px * 0.04f
                // Focus points with arrows
                drawCircle(color = color, center = Offset(px * 0.25f, py * 0.75f), radius = px * 0.1f, style = Stroke(width = strokeW * 0.75f))
                drawCircle(color = color, center = Offset(px * 0.70f, py * 0.30f), radius = px * 0.1f)
                
                val arrowPath = Path().apply {
                    moveTo(px * 0.25f, py * 0.75f)
                    quadraticTo(px * 0.35f, py * 0.35f, px * 0.68f, py * 0.31f)
                    // arrow head
                    moveTo(px * 0.58f, py * 0.22f)
                    lineTo(px * 0.71f, py * 0.30f)
                    lineTo(px * 0.60f, py * 0.42f)
                }
                drawPath(arrowPath, color = color, style = Stroke(width = strokeW * 0.75f))
            }
        }
    }
}

@Composable
fun SkeletonFeed() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_angle"
    )

    // Sleek metallic shimmer brush
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF13151A),
            Color(0xFF262A35),
            Color(0xFF13151A)
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )

        // Categories Row Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 85.dp, height = 36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
            }
        }

        // News Items Skeletons (Three beautiful placeholder cards)
        repeat(3) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x3BFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(width = 75.dp, height = 18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 50.dp, height = 12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleDetailScreen(
    article: NewsItem,
    onClose: () -> Unit
) {
    val categoryAccentColor = when (article.category) {
        "Liga MX" -> ColorLigaMX
        "Selección Nacional" -> ColorSeleccion
        "Internacional" -> ColorInternacional
        else -> PrimaryGreen
    }

    val context = LocalContext.current

    Surface(
        color = DarkBackground,
        modifier = Modifier
            .testTag("article_detail_modal")
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Elegant Stadium Header Cover Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                categoryAccentColor.copy(alpha = 0.35f),
                                DarkBackground
                            )
                        )
                    )
                    .padding(horizontal = 16.dp)
            ) {
                // Header interactive controls
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back badge button
                    Text(
                        text = "‹ VOLVER",
                        fontFamily = SportsCondensedFamily,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .testTag("detail_close_button")
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3E000000))
                            .clickable(onClick = onClose)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Compartiendo: ${article.title}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x3E000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir Notica",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Decorative sports item center visual graphic
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomEnd)
                        .offset(y = 20.dp)
                        .alpha(0.08f)
                ) {
                    SportsGraphic(
                        type = article.graphicType,
                        color = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Small live tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(categoryAccentColor.copy(alpha = 0.2f))
                        .border(BorderStroke(1.dp, categoryAccentColor.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = article.category.uppercase(),
                        fontFamily = SportsCondensedFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = categoryAccentColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STORY DETAILED WRITTEN TEXT PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Large sport Title
                Text(
                    text = article.title,
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Sports News Agency Signature bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .drawBehind {
                            // Bottom separator line
                            drawLine(
                                color = LightWhiteBorder,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(categoryAccentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = article.source.take(1).uppercase(),
                            fontFamily = SportsCondensedFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = categoryAccentColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column {
                        Text(
                            text = article.source,
                            fontFamily = SportsCondensedFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = article.timeAgo,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High Quality Drop Cap Paragraph Section
                val articleParagraphs = article.body.split("\n\n")
                if (articleParagraphs.isNotEmpty()) {
                    val firstParagraph = articleParagraphs.first()
                    val otherParagraphs = articleParagraphs.drop(1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Styled drop cap box (First letter in an outstanding visual accent container!)
                        val dropCapLetter = firstParagraph.take(1)
                        val dropCapText = firstParagraph.drop(1)

                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp, top = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(categoryAccentColor.copy(alpha = 0.15f))
                                .border(BorderStroke(2.dp, categoryAccentColor), RoundedCornerShape(4.dp))
                                .size(width = 46.dp, height = 46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dropCapLetter,
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = categoryAccentColor,
                                textAlign = Alignment.Center.run { TextAlign.Center }
                            )
                        }

                        Text(
                            text = dropCapText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = Color(0xFFECEFF4)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remaining paragraphs
                    otherParagraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = Color(0xFFECEFF4),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else {
                    Text(
                        text = article.body,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        color = Color(0xFFECEFF4)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action closing card at bottom
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = categoryAccentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        text = "ENTENDIDO",
                        fontFamily = SportsCondensedFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(category: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚽",
            fontSize = 44.sp,
            modifier = Modifier.alpha(0.6f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Sin noticias en la categoría $category",
            fontFamily = SportsCondensedFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        Text(
            text = "Prueba actualizando para buscar más notas.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ocurrió un contratiempo",
            fontFamily = SportsCondensedFamily,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(
                text = "REINTENTAR",
                fontFamily = SportsCondensedFamily,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun BottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = Color(0xFF0A0A0A), // h-20 bg-[#0A0A0A]
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), // border-t border-white/10
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(68.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 1: Inicio
            val isInicio = activeTab == "inicio"
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("inicio") }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isInicio) PrimaryGreen else Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Inicio",
                    tint = if (isInicio) PrimaryGreen else Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(22.dp)
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = "INICIO",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = if (isInicio) PrimaryGreen else Color.White.copy(alpha = 0.40f),
                    letterSpacing = 0.5.sp
                )
            }

            // Tab 2: Tablas
            val isTablas = activeTab == "tablas"
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("tablas") }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isTablas) PrimaryGreen else Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Tablas",
                    tint = if (isTablas) PrimaryGreen else Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(22.dp)
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = "TABLAS",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = if (isTablas) PrimaryGreen else Color.White.copy(alpha = 0.40f),
                    letterSpacing = 0.5.sp
                )
            }

            // Tab 3: Archivo
            val isArchivo = activeTab == "archivo"
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("archivo") }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isArchivo) PrimaryGreen else Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Archivo",
                    tint = if (isArchivo) PrimaryGreen else Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(22.dp)
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = "ARCHIVO",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = if (isArchivo) PrimaryGreen else Color.White.copy(alpha = 0.40f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun LigaMXStandingsTable() {
    val teamsByZone = listOf(
        StandingsRow("Cruz Azul", 17, 13, 3, 1, 39, 12, 42, ColorSeleccion), // Green dot: Liguilla Directa
        StandingsRow("Toluca", 17, 10, 5, 2, 38, 16, 35, ColorSeleccion),
        StandingsRow("Tigres UANL", 17, 10, 4, 3, 25, 15, 34, ColorSeleccion),
        StandingsRow("Pumas UNAM", 17, 9, 4, 4, 21, 13, 31, ColorSeleccion),
        StandingsRow("Monterrey", 17, 9, 4, 4, 26, 19, 31, ColorSeleccion),
        StandingsRow("Club América", 17, 8, 3, 6, 27, 21, 27, ColorSeleccion),
        StandingsRow("Atlético de San Luis", 17, 8, 3, 6, 26, 19, 27, ColorLigaMX), // Yellow dot: PlayIn
        StandingsRow("Tijuana", 17, 7, 5, 5, 22, 24, 26, ColorLigaMX),
        StandingsRow("Chivas Guadalajara", 17, 7, 4, 6, 24, 15, 25, ColorLigaMX),
        StandingsRow("Atlas", 17, 5, 7, 5, 17, 23, 22, ColorLigaMX),
        StandingsRow("Pachuca", 17, 4, 4, 9, 14, 25, 16, Color.Gray), // Gray dot: No clasificado
        StandingsRow("Necaxa", 17, 3, 6, 8, 13, 22, 15, Color.Gray)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "TABLA GENERAL LIGA MX",
            fontFamily = SportsCondensedFamily,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Clasificación general del torneo actual. Los primeros 6 avanzan directo a Liguilla. Posiciones 7 a 10 juegan eliminatoria Play-In.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "POS", fontFamily = SportsCondensedFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryGreen, modifier = Modifier.width(32.dp))
                Text(text = "CLUB", fontFamily = SportsCondensedFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                Text(text = "JJ", fontFamily = SportsCondensedFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                Text(text = "DG", fontFamily = SportsCondensedFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                Text(text = "PTS", fontFamily = SportsCondensedFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryGreen, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Table List
        teamsByZone.forEachIndexed { index, team ->
            val posNum = index + 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index % 2 == 0) Color(0xFF161616) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.width(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(team.zoneColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$posNum",
                        fontFamily = SportsCondensedFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = team.name,
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${team.jj}",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )

                val diffText = if (team.dg > 0) "+${team.dg}" else "${team.dg}"
                Text(
                    text = diffText,
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (team.dg > 0) ColorLigaMX else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )

                Text(
                    text = "${team.pts}",
                    fontFamily = SportsCondensedFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = PrimaryGreen,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ArchiveScreen(
    uiState: NewsUiState,
    onArticleClick: (NewsItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ARCHIVO HISTÓRICO",
            fontFamily = SportsCondensedFamily,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Ediciones o resúmenes anteriores generados automáticamente por Inteligencia Artificial.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (uiState) {
            is NewsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            }
            is NewsUiState.Success -> {
                val archivedItems = uiState.data.articles.filter { !it.isBreaking }.mapIndexed { index, newsItem ->
                    newsItem.copy(
                        title = "[HISTÓRICO] " + newsItem.title,
                        timeAgo = "Hace ${index + 2} días"
                    )
                }

                if (archivedItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No hay notas archivadas por ahora.", color = Color.White)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(archivedItems) { article ->
                            NewsArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) }
                            )
                        }
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No se pudieron recuperar las notas del archivo.", color = Color.White)
                }
            }
        }
    }
}

data class StandingsRow(
    val name: String,
    val jj: Int,
    val jg: Int,
    val je: Int,
    val jp: Int,
    val gf: Int,
    val gc: Int,
    val pts: Int,
    val zoneColor: Color
) {
    val dg = gf - gc
}

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsDialog(
    breakingEnabled: Boolean,
    onBreakingToggle: (Boolean) -> Unit,
    summaryEnabled: Boolean,
    onSummaryToggle: (Boolean) -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onSimulateBreaking: () -> Unit,
    onSimulateSummary: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("notification_settings_dialog"),
        content = {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF161616),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "NOTIFICACIONES",
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Configura tus alertas en tiempo real para estar siempre enterado de lo que pasa en la Liga MX y el fútbol mundial.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Row 1: Breaking News Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NOTICIAS DE ÚLTIMA HORA",
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Fichajes bomba, lesiones urgentes y noticias relevantes al instante.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = breakingEnabled,
                            onCheckedChange = onBreakingToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF2E2E2E)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                    // Row 2: Daily Summary Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RESUMEN DIARIO POR IA",
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Un boletín inteligente con lo más destacado de cada jornada deportiva.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = summaryEnabled,
                            onCheckedChange = onSummaryToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF2E2E2E)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                    // Permission Warning (Android 13+ support)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentRed.copy(alpha = 0.1f))
                                .border(BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⚠️ Permisos inactivos. El sistema operativo requiere autorización para mostrar alertas en tu teléfono.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentRed,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Activar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Test push simulations
                    Text(
                        text = "PRUEBAS DE NOTIFICACIÓN",
                        fontFamily = SportsCondensedFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = PrimaryGreen,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSimulateBreaking,
                            enabled = breakingEnabled,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF252525),
                                disabledContainerColor = Color(0xFF1D1D1D)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (breakingEnabled) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        ) {
                            Text(
                                text = "Simular Alerta",
                                color = if (breakingEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                                fontSize = 12.sp,
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onSimulateSummary,
                            enabled = summaryEnabled,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF252525),
                                disabledContainerColor = Color(0xFF1D1D1D)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (summaryEnabled) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        ) {
                            Text(
                                text = "Simular Resumen",
                                color = if (summaryEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                                fontSize = 12.sp,
                                fontFamily = SportsCondensedFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Guardar y Cerrar
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "GUARDAR Y CERRAR",
                            fontFamily = SportsCondensedFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    )
}
