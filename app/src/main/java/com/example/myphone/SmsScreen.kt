package com.example.myphone

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- STARTUP DESIGN TOKENS (Netflix Style) ---
val NetflixRed = Color(0xFFDB0000)
val DarkBackground = Color(0xFF000000)
val CardBackground = Color(0xFF1A1A1A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val AccentDarkRed = Color(0xFF831010)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(viewModel: SmsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("BizCredit intelligence", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground,
                        titleContentColor = TextPrimary
                    )
                )
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = DarkBackground,
                    contentColor = NetflixRed,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = NetflixRed
                        )
                    }
                ) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        text = { Text("Overview", color = if (currentTab == 0) NetflixRed else TextSecondary) }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        text = { Text("Analytics", color = if (currentTab == 1) NetflixRed else TextSecondary) }
                    )
                    Tab(
                        selected = currentTab == 2,
                        onClick = { viewModel.setTab(2) },
                        text = { Text("Loans", color = if (currentTab == 2) NetflixRed else TextSecondary) }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.syncBusinessData() },
                icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary) },
                text = { Text("Sync Data", color = TextPrimary) },
                containerColor = NetflixRed,
                contentColor = TextPrimary
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    LoadingOverlay()
                }
                is UiState.Success -> {
                    Crossfade(targetState = currentTab, label = "tabFade") { tab ->
                        when (tab) {
                            0 -> OverviewTab(state.profile, state.history, viewModel.getCurrentTimestamp())
                            1 -> AnalyticsTab(state.history, viewModel)
                            2 -> LoansTab(state.profile.eligibleLoans)
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorOverlay(state.message) { viewModel.syncBusinessData() }
                }
                is UiState.Idle -> {
                    EmptyStateOverlay { viewModel.syncBusinessData() }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
fun OverviewTab(profile: CreditProfileResponse, history: List<HistoryItem>, status: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CreditScoreCard(profile) }
        item { BusinessMetricsGrid(profile.features) }
        item { EvaluationInsightsList(profile.insights) }
        item { 
            Text(
                status,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun AnalyticsTab(history: List<HistoryItem>, viewModel: SmsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AnalyticsTitle("Cash flow distribution")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                val (income, expense) = viewModel.getIncomeExpenseData(history)
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IncomeExpensePieChart(income, expense)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        LegendItem("Inflow", NetflixRed)
                        LegendItem("Outflow", Color(0xFF444444))
                    }
                }
            }
        }

        item {
            AnalyticsTitle("Business activity Trends")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                val activityData = viewModel.getActivityData(history)
                Column(modifier = Modifier.padding(24.dp)) {
                    ActivityBarChart(activityData)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Transaction density monitoring", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        item { AnalyticsTitle("Recent Transacting Partners") }
        items(history.take(5)) { item ->
            TransactionRow(item)
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun LoansTab(loans: List<LoanProduct>?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Loan marketplace",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text("Personalized credit products for you", color = TextSecondary, fontSize = 14.sp)
        }

        if (loans.isNullOrEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = AccentDarkRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Offers available", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            "Improve your score to unlock higher credit limits and micro-loan opportunities.",
                            textAlign = TextAlign.Center,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(loans) { loan ->
                LoanCard(loan)
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

// --- COMPONENTS ---

@Composable
fun CreditScoreCard(profile: CreditProfileResponse) {
    val riskColor = when (profile.risk) {
        "LOW" -> Color(0xFF4CAF50)
        "MEDIUM" -> Color(0xFFFFC107)
        else -> NetflixRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp, 
                ambientColor = NetflixRed, 
                spotColor = NetflixRed, 
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Health Score", color = TextSecondary, fontSize = 14.sp)
            
            // SCORE WITH GLOW
            Box(contentAlignment = Alignment.Center) {
                // Subtle Red Glow
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NetflixRed.copy(0.2f), Color.Transparent),
                            center = center,
                            radius = size.width / 1f
                        )
                    )
                }
                Text(
                    "${profile.score}", 
                    fontSize = 64.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = NetflixRed
                )
            }
            
            Surface(
                color = riskColor.copy(0.1f), 
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "${profile.risk} Risk Profile",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(0.05f))
            Spacer(modifier = Modifier.height(24.dp))

            val eligibilityText = if (profile.score >= 650) "✅ High Loan Eligibility" else "⚠️ Build history to qualify"
            Text(eligibilityText, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(profile.summary, textAlign = TextAlign.Center, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun BusinessMetricsGrid(features: BusinessFeatures?) {
    val feat = features ?: BusinessFeatures()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox("Cash Inflow", "₹${feat.totalCredit}", NetflixRed, Modifier.weight(1f))
            MetricBox("Outflow", "₹${feat.totalDebit}", TextSecondary, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox("Net Flow", "₹${feat.netBalance}", TextPrimary, Modifier.weight(1f))
            MetricBox("Activity", "${feat.transactionCount}", TextPrimary, Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, accentColor: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
    }
}

@Composable
fun LoanCard(loan: LoanProduct) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(loan.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("By ${loan.provider}", color = TextSecondary, fontSize = 12.sp)
                }
                Surface(color = NetflixRed.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(loan.amount, Modifier.padding(6.dp), color = NetflixRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { 
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loan.link))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Safe fallback
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- VISUALIZATIONS ---

@Composable
fun IncomeExpensePieChart(income: Float, expense: Float) {
    val total = income + expense
    if (total == 0f) return
    
    val incomeSweep = (income / total) * 360f
    
    Canvas(modifier = Modifier.size(160.dp)) {
        drawArc(
            color = NetflixRed,
            startAngle = -90f,
            sweepAngle = incomeSweep,
            useCenter = false,
            style = Stroke(width = 35f)
        )
        drawArc(
            color = Color(0xFF333333),
            startAngle = -90f + incomeSweep,
            sweepAngle = 360f - incomeSweep,
            useCenter = false,
            style = Stroke(width = 35f)
        )
    }
}

@Composable
fun ActivityBarChart(data: List<Pair<String, Float>>) {
    if (data.isEmpty()) return
    val maxVal = data.maxByOrNull { it.second }?.second ?: 10f

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(80.dp * (value / maxVal).coerceIn(0.1f, 1f))
                        .background(NetflixRed, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Text(label, fontSize = 7.sp, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// --- HELPERS ---

@Composable
fun LoadingOverlay() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = NetflixRed)
        Spacer(Modifier.height(16.dp))
        Text("Running intelligence...", color = TextSecondary)
    }
}

@Composable
fun AnalyticsTitle(title: String) {
    Text(
        title.uppercase(), 
        fontWeight = FontWeight.ExtraBold, 
        color = TextPrimary, 
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, Modifier.padding(start = 8.dp), fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun TransactionRow(item: HistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.merchant, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text(item.date, fontSize = 11.sp, color = TextSecondary)
            }
            Text(
                if (item.type == "credit") "+₹${item.amount}" else "-₹${item.amount}",
                color = if (item.type == "credit") Color(0xFF4CAF50) else NetflixRed,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun EvaluationInsightsList(insights: CreditInsights?) {
    val ins = insights ?: CreditInsights()
    Column {
        Text("Insights", fontWeight = FontWeight.Bold, color = TextPrimary)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BulletPoint("Stability", ins.incomeStrength)
                BulletPoint("Spending", ins.spendingBehavior)
                BulletPoint("Activity", ins.activityLevel)
            }
        }
    }
}

@Composable
fun BulletPoint(label: String, text: String) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.size(4.dp).background(NetflixRed).offset(y = 8.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
            Text(text, fontSize = 13.sp, color = TextPrimary)
        }
    }
}

@Composable
fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = NetflixRed)
        Text(message, textAlign = TextAlign.Center, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onRetry, Modifier.padding(top = 24.dp), colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)) { 
            Text("Retry Analysis") 
        }
    }
}

@Composable
fun EmptyStateOverlay(onSync: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No Business activity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            "Push your transaction data to our engine to generate credits score and unlock micro-loan offers.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onSync, Modifier.padding(top = 32.dp), colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)) {
            Text("Start Analysis", fontWeight = FontWeight.Bold)
        }
    }
}