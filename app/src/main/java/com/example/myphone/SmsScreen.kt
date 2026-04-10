package com.example.myphone

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- DESIGN SYSTEM ---
val NetflixRed = Color(0xFFDB0000)
val DarkBackground = Color(0xFF000000)
val CardBackground = Color(0xFF1A1A1A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val GrowthGreen = Color(0xFF4CAF50)

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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = DarkBackground,
                    contentColor = NetflixRed,
                    divider = {},
                    indicator = { tabPositions ->
                        SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[currentTab]), color = NetflixRed)
                    }
                ) {
                    Tab(selected = currentTab == 0, onClick = { viewModel.setTab(0) }, text = { Text("Overview", color = if (currentTab == 0) NetflixRed else TextSecondary) })
                    Tab(selected = currentTab == 1, onClick = { viewModel.setTab(1) }, text = { Text("Analytics", color = if (currentTab == 1) NetflixRed else TextSecondary) })
                    Tab(selected = currentTab == 2, onClick = { viewModel.setTab(2) }, text = { Text("Loans", color = if (currentTab == 2) NetflixRed else TextSecondary) })
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.syncBusinessData() },
                icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary) },
                text = { Text("Sync Analytics", color = TextPrimary) },
                containerColor = NetflixRed
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(DarkBackground)) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingOverlay()
                is UiState.Success -> {
                    Crossfade(targetState = currentTab, label = "tab") { tab ->
                        when (tab) {
                            0 -> OverviewTab(state.profile, state.history, viewModel.getCurrentTimestamp())
                            1 -> AnalyticsTab(state.history, viewModel)
                            2 -> LoansTab(state.profile.eligibleLoans)
                        }
                    }
                }
                is UiState.Error -> ErrorOverlay(state.message) { viewModel.syncBusinessData() }
                is UiState.Idle -> EmptyStateOverlay { viewModel.syncBusinessData() }
            }
        }
    }
}

// --- TABS ---

@Composable
fun OverviewTab(profile: CreditProfileResponse, history: List<HistoryItem>, lastUpdate: String) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { CreditScoreCard(profile) }
        item { ScoreBreakdownSection(profile.breakdown) }
        item { BusinessMetricsGrid(profile.features) }
        item { EvaluationInsightsList(profile.insights) }
        item { Text(lastUpdate, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TextSecondary, fontSize = 11.sp) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun AnalyticsTab(history: List<HistoryItem>, viewModel: SmsViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            AnalyticsTitle("Cash Flow Profile")
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                val (income, expense) = viewModel.getIncomeExpenseData(history)
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IncomeExpensePieChart(income, expense)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        LegendItem("Inflow", NetflixRed)
                        LegendItem("Outflow", Color(0xFF333333))
                    }
                }
            }
        }
        item { AnalyticsTitle("Historical transacting partners") }
        items(history.take(10)) { TransactionRow(it) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun LoansTab(loans: List<LoanProduct>?) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Loan marketplace", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
            Text("Credit products ranked by eligibility", color = TextSecondary, fontSize = 14.sp)
        }
        if (loans.isNullOrEmpty()) {
            item { Text("No offers available at this time.", color = TextSecondary, modifier = Modifier.padding(top = 24.dp)) }
        } else {
            items(loans) { LoanCard(it) }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// --- CORE DASHBOARD COMPONENTS ---

@Composable
fun CreditScoreCard(profile: CreditProfileResponse) {
    val riskColor = when (profile.risk) {
        "LOW" -> GrowthGreen
        "MEDIUM" -> Color(0xFFFFC107)
        else -> NetflixRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp, 
                ambientColor = NetflixRed.copy(0.1f), 
                spotColor = NetflixRed.copy(0.1f), 
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Business Credit Score".uppercase(), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp)) {
                    drawCircle(Brush.radialGradient(listOf(NetflixRed.copy(0.15f), Color.Transparent), center = center, radius = size.width))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${profile.score}", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    if (profile.scoreChange != 0) {
                        ScoreChangeIndicator(profile.scoreChange)
                    }
                }
            }
            
            Surface(color = riskColor.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                Text("${profile.risk} RISK PROFILE", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = riskColor, fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(0.05f))
            Spacer(Modifier.height(24.dp))
            Text(profile.summary, textAlign = TextAlign.Center, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun ScoreChangeIndicator(change: Int) {
    val color = if (change > 0) GrowthGreen else NetflixRed
    val sign = if (change > 0) "+" else ""
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
        Icon(if (change > 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text("$sign$change", color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun ScoreBreakdownSection(breakdown: ScoreBreakdown) {
    Column {
        Text("Calculation breakdown", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BreakdownBox("Income", "+${breakdown.income}", Modifier.weight(1f))
            BreakdownBox("Activity", "+${breakdown.activity}", Modifier.weight(1f))
            BreakdownBox("Stability", "+${breakdown.stability}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("Calculated using additive behavioral logic (Base: 300)", color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun BreakdownBox(label: String, points: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardBackground), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = TextSecondary)
            Text(points, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GrowthGreen)
        }
    }
}

@Composable
fun LoanCard(loan: LoanProduct) {
    val context = LocalContext.current
    val borderColor = if (loan.eligible) GrowthGreen.copy(0.4f) else Color.White.copy(0.05f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(20.dp)) {

            // Header Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (loan.tag.isNotEmpty()) {
                        Surface(
                            color = if (loan.eligible) GrowthGreen.copy(0.15f) else NetflixRed.copy(0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                loan.tag.uppercase(),
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = if (loan.eligible) GrowthGreen else NetflixRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(loan.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    Text(loan.provider, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Surface(
                    color = if (loan.eligible) GrowthGreen.copy(0.15f) else Color.White.copy(0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (loan.eligible) "✓ ELIGIBLE" else "LOCKED",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (loan.eligible) GrowthGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (loan.description.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(loan.description, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(0.05f))
            Spacer(Modifier.height(14.dp))

            // Key Stats Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LoanStat("Max Amount", loan.maxAmount, Modifier.weight(1f))
                LoanStat("Interest Rate", loan.interestRate, Modifier.weight(1f))
                LoanStat("Tenure", loan.tenure, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            if (loan.eligible) {
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(loan.link))) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Now →", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Need ${loan.pointsToUnlock} more points to unlock (Min. Score: ${loan.minScore})",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LoanStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White.copy(0.03f), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(label, fontSize = 9.sp, color = TextSecondary)
        Text(value.ifEmpty { "—" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

// --- HELPER WRAPPERS ---

@Composable
fun MetricBox(label: String, value: String, accent: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
fun BusinessMetricsGrid(features: BusinessFeatures?) {
    val f = features ?: BusinessFeatures()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox("Cash Inflow", "₹${f.totalCredit}", TextPrimary, Modifier.weight(1f))
            MetricBox("Business Outflow", "₹${f.totalDebit}", TextSecondary, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox("Net Cash Flow", "₹${f.netBalance}", GrowthGreen, Modifier.weight(1f))
            MetricBox("Activity Level", "${f.transactionCount}", TextPrimary, Modifier.weight(1f))
        }
    }
}

@Composable
fun EvaluationInsightsList(insights: CreditInsights?) {
    val ins = insights ?: CreditInsights()
    Column {
        Text("Credit Highlights", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightRow("Income stability", ins.incomeStrength)
                InsightRow("Spending behavior", ins.spendingBehavior)
                InsightRow("Activity consistency", ins.activityLevel)
            }
        }
    }
}

@Composable
fun InsightRow(label: String, text: String) {
    Row {
        Text("• ", color = NetflixRed, fontWeight = FontWeight.Bold)
        Column {
            Text(label, fontSize = 10.sp, color = TextSecondary)
            Text(text, fontSize = 13.sp, color = TextPrimary)
        }
    }
}

@Composable
fun TransactionRow(item: HistoryItem) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.merchant, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text(item.date, fontSize = 11.sp, color = TextSecondary)
            }
            Text(if (item.type == "credit") "+₹${item.amount}" else "-₹${item.amount}", color = if (item.type == "credit") GrowthGreen else NetflixRed, fontWeight = FontWeight.Bold)
        }
    }
}

// --- VISUALIZATIONS ---

@Composable
fun IncomeExpensePieChart(income: Float, expense: Float) {
    val total = income + expense
    if (total == 0f) return
    val sweep = (income / total) * 360f
    Canvas(Modifier.size(160.dp)) {
        drawArc(NetflixRed, -90f, sweep, false, style = Stroke(30f))
        drawArc(Color(0xFF333333), -90f + sweep, 360f - sweep, false, style = Stroke(30f))
    }
}

@Composable
fun AnalyticsTitle(text: String) {
    Text(text.uppercase(), fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, Modifier.padding(start = 8.dp), fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun LoadingOverlay() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = NetflixRed)
        Spacer(Modifier.height(16.dp))
        Text("Analyzing Transaction Batch...", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun ErrorOverlay(msg: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, contentDescription = null, size = 64.dp, tint = NetflixRed)
        Text(msg, textAlign = TextAlign.Center, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onRetry, Modifier.padding(top = 24.dp), colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)) { Text("Retry Analysis") }
    }
}

@Composable
fun EmptyStateOverlay(onSync: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Financial engine ready", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary)
        Text("Sync your business messages to generate your official Business Credit Score.", textAlign = TextAlign.Center, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = onSync, Modifier.padding(top = 32.dp), colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)) { Text("Start Business Sync", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    androidx.compose.material3.Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(size), tint = tint)
}