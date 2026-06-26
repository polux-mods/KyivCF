package ua.vitaliyshkarupa.kyivclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val KyivBlue = Color(0xFF008CFF)
private val CardDark = Color(0xFF172840)
private val WhitePanel = Color(0xFFF7F7F7)
private val TextDark = Color(0xFF343942)
private val TextMuted = Color(0xFF737985)
private val Green = Color(0xFF50CC66)
private val Red = Color(0xFFF43F43)
private val WarningYellow = Color(0xFFFFC247)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.rgb(140, 141, 145)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false

        setContent {
            MaterialTheme {
                KyivTransportCloneApp()
            }
        }
    }
}

@Composable
private fun KyivTransportCloneApp(vm: KyivEventsViewModel = viewModel()) {
    val events by vm.events.collectAsState()
    val loading by vm.isLoading.collectAsState()
    var showPayment by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KyivBlue)
    ) {
        MainDashboard(
            events = events,
            loading = loading,
            onPaymentClick = { showPayment = true },
            onRefreshAlerts = { vm.refreshNow() }
        )

        AnimatedVisibility(
            visible = showPayment,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            PaymentOverlay(onClose = { showPayment = false })
        }
    }
}

@Composable
private fun MainDashboard(
    events: List<CityEvent>,
    loading: Boolean,
    onPaymentClick: () -> Unit,
    onRefreshAlerts: () -> Unit
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val panelHeight = maxHeight - 455.dp
        val safePanelHeight = if (panelHeight < 360.dp) maxHeight * 0.56f else panelHeight

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusTop)
        ) {
            TopChipsRow()
            Spacer(Modifier.height(31.dp))
            HeaderBlock()
        }

        ServiceCarousel(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = statusTop + 405.dp),
            onPaymentClick = onPaymentClick
        )

        NotificationsPanel(
            events = events,
            loading = loading,
            onRefreshAlerts = onRefreshAlerts,
            bottomPadding = navBottom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(safePanelHeight)
        )
    }
}

@Composable
private fun TopChipsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChipCloud()
        Spacer(Modifier.width(12.dp))
        Text("27°", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(18.dp))
        AqiCircle()
        Spacer(Modifier.width(10.dp))
        Text("27", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(17.dp))
        BoltCircle()
        Spacer(Modifier.weight(1f))
        SearchPill()
    }
}

@Composable
private fun ChipCloud() {
    Box(
        modifier = Modifier
            .size(55.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center
    ) {
        CloudIcon(color = Color.White, modifier = Modifier.size(33.dp))
    }
}

@Composable
private fun AqiCircle() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.17f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AQI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(26.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Green)
            )
        }
    }
}

@Composable
private fun BoltCircle() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.17f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(43.dp)
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Text("⚡", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SearchPill() {
    Row(
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchIcon(color = Color.White, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(8.dp))
        Text("Пошук", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeaderBlock() {
    Column(modifier = Modifier.padding(horizontal = 40.dp)) {
        Text(
            text = "Пʼятниця, 26 червня",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "У Києві хмарно",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.4).sp,
                modifier = Modifier.weight(1f)
            )
            Avatar(modifier = Modifier.offset(y = (-3).dp))
        }
    }
}

@Composable
private fun Avatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFE6A93C), Color(0xFF5A3A18), Color(0xFF151515)),
                    center = Offset(50f, 42f),
                    radius = 95f
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("К", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ServiceCarousel(modifier: Modifier = Modifier, onPaymentClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ServiceCard(
            title = "Міські\nсервіси",
            dark = true,
            iconKind = ServiceIconKind.City,
            enabled = false
        )
        ServiceCard(
            title = "Придбати\nQR-квиток",
            iconKind = ServiceIconKind.Qr,
            enabled = false
        )
        ServiceCard(
            title = "Оплата\nпроїзду",
            iconKind = ServiceIconKind.Wallet,
            enabled = true,
            onClick = onPaymentClick
        )
        ServiceCard(
            title = "Рух\nтранспорту",
            iconKind = ServiceIconKind.Transport,
            enabled = false
        )
    }
}

private enum class ServiceIconKind { City, Qr, Wallet, Transport }

@Composable
private fun ServiceCard(
    title: String,
    iconKind: ServiceIconKind,
    dark: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(14.dp)
    val background = if (dark) CardDark else WhitePanel
    val textColor = if (dark) Color.White else Color(0xFF3D3E43)
    val iconColor = if (dark) Color.White else Color(0xFF3D3E43)
    val base = Modifier
        .width(181.dp)
        .height(187.dp)
        .clip(shape)
        .background(background)
    val clickableModifier = if (enabled) base.clickable(onClick = onClick) else base

    Box(modifier = clickableModifier) {
        Box(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            when (iconKind) {
                ServiceIconKind.City -> CityShieldIcon(iconColor, Modifier.size(31.dp))
                ServiceIconKind.Qr -> QrIcon(iconColor, Modifier.size(31.dp))
                ServiceIconKind.Wallet -> WalletIcon(iconColor, Modifier.size(32.dp))
                ServiceIconKind.Transport -> TransportLinesIcon(iconColor, Modifier.size(34.dp))
            }
        }
        Text(
            text = title,
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 23.dp, end = 10.dp)
        )
    }
}

@Composable
private fun NotificationsPanel(
    events: List<CityEvent>,
    loading: Boolean,
    onRefreshAlerts: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WhitePanel,
        shape = RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 29.dp, top = 43.dp, end = 28.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Сьогодні", color = TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (loading) "Оновлюю…" else "Оновити",
                    color = Color.Transparent,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onRefreshAlerts() }
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 18.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    NotificationRow(event)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(event: CityEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 27.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventCircle(event.type)
        Spacer(Modifier.width(23.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.statusDot != null) {
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .clip(CircleShape)
                            .background(event.statusDot)
                            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = buildString {
                        if (event.time.isNotBlank()) append(event.time).append(" ")
                        append(event.title)
                    },
                    color = TextDark,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = event.description,
                color = TextMuted,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("›", color = TextMuted, fontSize = 52.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun EventCircle(type: EventType) {
    val color = when (type) {
        EventType.Info -> Color(0xFFDFE9F8)
        EventType.Alert -> Red
        EventType.AllClear -> Green
        EventType.Warning -> WarningYellow
    }
    Box(
        modifier = Modifier
            .size(67.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            EventType.Info -> Text("🚇", fontSize = 30.sp)
            EventType.Alert -> {
                Canvas(Modifier.size(38.dp)) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, Color.White)
                }
                Text("!", color = Red, fontSize = 25.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(y = 5.dp))
            }
            EventType.AllClear -> Text("✓", color = Color.White, fontSize = 39.sp, fontWeight = FontWeight.Black)
            EventType.Warning -> Text("!", color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PaymentOverlay(onClose: () -> Unit) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KyivBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusTop)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Оплата проїзду",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(42.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = WhitePanel,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 30.dp, bottom = navBottom + 24.dp)
                ) {
                    Text(
                        "Доступні дії",
                        color = TextMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    PaymentAction("Придбати QR-квиток", "Квиток для разової поїздки", "⌗")
                    PaymentAction("Оплатити банківською картою", "Муляж екрана оплати без списання коштів", "▱")
                    PaymentAction("Транспортна карта", "Поповнення та перегляд балансу", "◫")
                    Spacer(Modifier.weight(1f))
                    Text(
                        "У цьому макеті працює тільки перехід у розділ оплати. Інші плитки зверху залишені як муляж, як ти просив.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentAction(title: String, subtitle: String, mark: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(KyivBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(mark, color = KyivBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = TextMuted, fontSize = 15.sp, lineHeight = 20.sp)
        }
        Text("›", color = TextMuted, fontSize = 42.sp, fontWeight = FontWeight.Light)
    }
    Spacer(Modifier.height(12.dp))
}

// ---------- UI icons drawn locally, без залежностей на Material Icons ----------

@Composable
private fun CloudIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 4.2f, cap = StrokeCap.Round)
        drawCircle(color, radius = size.minDimension * 0.20f, center = Offset(size.width * 0.36f, size.height * 0.55f), style = stroke)
        drawCircle(color, radius = size.minDimension * 0.26f, center = Offset(size.width * 0.52f, size.height * 0.44f), style = stroke)
        drawCircle(color, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.70f, size.height * 0.58f), style = stroke)
        drawLine(color, Offset(size.width * 0.28f, size.height * 0.73f), Offset(size.width * 0.75f, size.height * 0.73f), strokeWidth = 4.2f, cap = StrokeCap.Round)
    }
}

@Composable
private fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(color, radius = size.minDimension * 0.31f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
        drawLine(color, Offset(size.width * 0.64f, size.height * 0.64f), Offset(size.width * 0.88f, size.height * 0.88f), strokeWidth = 4.5f, cap = StrokeCap.Round)
    }
}

@Composable
private fun CityShieldIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val shield = Path().apply {
            moveTo(size.width * 0.08f, 0f)
            lineTo(size.width * 0.92f, 0f)
            lineTo(size.width * 0.92f, size.height * 0.72f)
            lineTo(size.width * 0.50f, size.height)
            lineTo(size.width * 0.08f, size.height * 0.72f)
            close()
        }
        drawPath(shield, color)
        drawRect(if (color == Color.White) CardDark else WhitePanel, topLeft = Offset(size.width * 0.29f, size.height * 0.24f), size = Size(size.width * 0.42f, size.height * 0.16f))
        drawRect(if (color == Color.White) CardDark else WhitePanel, topLeft = Offset(size.width * 0.29f, size.height * 0.48f), size = Size(size.width * 0.42f, size.height * 0.16f))
    }
}

@Composable
private fun QrIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 4.6f
        val gap = s * 0.62f
        val stroke = Stroke(width = 3.5f)
        fun box(x: Float, y: Float) {
            drawRoundRect(color, topLeft = Offset(x, y), size = Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = stroke)
        }
        box(0f, 0f); box(s + gap, 0f); box(0f, s + gap); box(s + gap, s + gap)
        drawCircle(color, radius = 3.8f, center = Offset(size.width * 0.82f, size.height * 0.82f))
    }
}

@Composable
private fun WalletIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)
        drawRoundRect(color, topLeft = Offset(size.width * 0.10f, size.height * 0.18f), size = Size(size.width * 0.72f, size.height * 0.62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = stroke)
        drawRoundRect(color, topLeft = Offset(size.width * 0.53f, size.height * 0.34f), size = Size(size.width * 0.36f, size.height * 0.30f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = stroke)
        drawCircle(color, radius = 2.7f, center = Offset(size.width * 0.71f, size.height * 0.49f))
    }
}

@Composable
private fun TransportLinesIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 4.4f, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.18f, size.height * 0.30f), Offset(size.width * 0.72f, size.height * 0.30f), strokeWidth = 4.4f, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.28f, size.height * 0.55f), Offset(size.width * 0.82f, size.height * 0.55f), strokeWidth = 4.4f, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.18f, size.height * 0.80f), Offset(size.width * 0.72f, size.height * 0.80f), strokeWidth = 4.4f, cap = StrokeCap.Round)
        drawCircle(color, radius = 3.2f, center = Offset(size.width * 0.90f, size.height * 0.18f), style = stroke)
        drawCircle(color, radius = 3.2f, center = Offset(size.width * 0.10f, size.height * 0.55f), style = stroke)
        drawCircle(color, radius = 3.2f, center = Offset(size.width * 0.86f, size.height * 0.80f), style = stroke)
    }
}

// ---------- Дані подій і реальна тривога ----------

enum class EventType { Info, Alert, AllClear, Warning }

data class CityEvent(
    val id: String,
    val type: EventType,
    val time: String,
    val title: String,
    val description: String,
    val statusDot: Color? = null
)

data class KyivAlertState(
    val active: Boolean,
    val changedAt: String? = null,
    val cachedAt: String? = null,
    val source: String? = null
)

class KyivEventsViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<CityEvent>>(baseEvents())
    val events: StateFlow<List<CityEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastActiveState: Boolean? = null

    init {
        viewModelScope.launch {
            while (true) {
                refreshAirAlert(addTechnicalWarning = true)
                delay(60_000)
            }
        }
    }

    fun refreshNow() {
        viewModelScope.launch { refreshAirAlert(addTechnicalWarning = true) }
    }

    private suspend fun refreshAirAlert(addTechnicalWarning: Boolean) {
        _isLoading.value = true
        val result = UbillingAerialAlertsClient.fetchKyivAirAlert()
        _isLoading.value = false

        result.onSuccess { state ->
            val old = lastActiveState
            lastActiveState = state.active

            val currentAirEvent = if (state.active) {
                alertStartedEvent(state)
            } else {
                allClearEvent(state, initial = old == null)
            }

            _events.value = listOf(currentAirEvent) + _events.value.filterNot {
                it.id.startsWith("air_") || it.id == "api_warning"
            }
        }.onFailure { error ->
            if (addTechnicalWarning && _events.value.none { it.id == "api_warning" }) {
                _events.value = listOf(
                    CityEvent(
                        id = "api_warning",
                        type = EventType.Warning,
                        time = currentHm(),
                        title = "Не вдалося отримати тривоги",
                        description = error.message ?: "Перевір інтернет-з’єднання або доступність ubilling.net.ua/aerialalerts/.",
                        statusDot = WarningYellow
                    )
                ) + _events.value
            }
        }
    }

    private fun alertStartedEvent(state: KyivAlertState): CityEvent {
        val time = formatUbillingTime(state.changedAt) ?: currentHm()
        val duration = durationFromUbillingDate(state.changedAt)
        return CityEvent(
            id = "air_current_alert",
            type = EventType.Alert,
            time = time,
            title = "повітряна тривога!",
            description = if (duration != null) {
                "Київ. Небезпека триває $duration. Дані оновлено ${state.cachedAt ?: "щойно"}."
            } else {
                "Київ. Прямуйте до укриття та чекайте на відбій."
            },
            statusDot = Red
        )
    }

    private fun allClearEvent(state: KyivAlertState, initial: Boolean): CityEvent {
        val time = formatUbillingTime(state.changedAt) ?: currentHm()
        return CityEvent(
            id = "air_current_all_clear",
            type = EventType.AllClear,
            time = time,
            title = "відбій тривоги",
            description = if (initial) {
                "У Києві немає активної повітряної тривоги. Дані оновлено ${state.cachedAt ?: "щойно"}."
            } else {
                "Повітряну тривогу в Києві скасовано. Дані оновлено ${state.cachedAt ?: "щойно"}."
            },
            statusDot = Color(0xFF70AE44)
        )
    }

    private companion object {
        fun baseEvents(): List<CityEvent> = listOf(
            CityEvent(
                id = "student_pass",
                type = EventType.Info,
                time = "",
                title = "Платний проїзд для учнів",
                description = "З 1 липня до 31 серпня за учнівськими діє знижка 75% на проїзні."
            )
        )
    }
}

object UbillingAerialAlertsClient {
    private const val ENDPOINT = "https://ubilling.net.ua/aerialalerts/"
    private const val KYIV_CITY_KEY = "м. Київ"

    suspend fun fetchKyivAirAlert(): Result<KyivAlertState> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "KyivTransportClone/1.0")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            connection.disconnect()

            if (code !in 200..299) error("ubilling.net.ua/aerialalerts повернув HTTP $code")

            val root = JSONObject(body)
            val states = root.optJSONObject("states") ?: error("У відповіді API немає блока states")
            val kyiv = states.optJSONObject(KYIV_CITY_KEY) ?: error("У відповіді API немає стану для м. Київ")

            KyivAlertState(
                active = kyiv.optBoolean("alertnow", false),
                changedAt = kyiv.optString("changed").takeIf { it.isNotBlank() },
                cachedAt = root.optString("cachedat").takeIf { it.isNotBlank() },
                source = root.optString("source").takeIf { it.isNotBlank() }
            )
        }
    }
}

private val ubillingDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale("uk", "UA"))
private val kyivZone: ZoneId = ZoneId.of("Europe/Kyiv")

private fun currentHm(): String = DateTimeFormatter.ofPattern("HH:mm")
    .format(Instant.now().atZone(ZoneId.systemDefault()))

private fun parseUbillingDate(value: String?) = try {
    if (value.isNullOrBlank()) null
    else LocalDateTime.parse(value, ubillingDateFormatter).atZone(kyivZone)
} catch (_: Throwable) {
    null
}

private fun formatUbillingTime(value: String?): String? = parseUbillingDate(value)?.let {
    DateTimeFormatter.ofPattern("HH:mm").format(it.withZoneSameInstant(ZoneId.systemDefault()))
}

private fun durationFromUbillingDate(value: String?): String? {
    val start = parseUbillingDate(value)?.toInstant() ?: return null
    return try {
        val minutes = Duration.between(start, Instant.now()).toMinutes().coerceAtLeast(0)
        val hours = minutes / 60
        val rest = minutes % 60
        when {
            hours <= 0 -> "$rest хвилин"
            rest <= 0 -> "$hours ${hourWord(hours)}"
            else -> "$hours ${hourWord(hours)} $rest хвилин"
        }
    } catch (_: Throwable) {
        null
    }
}

private fun hourWord(hours: Long): String {
    val h = hours % 100
    val last = hours % 10
    return when {
        h in 11..14 -> "годин"
        last == 1L -> "годину"
        last in 2..4 -> "години"
        else -> "годин"
    }
}

