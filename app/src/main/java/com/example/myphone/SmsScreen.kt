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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(viewModel: SmsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("BizCredit Intelligence", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A237E),
                        titleContentColor = Color.White
                    )
                )
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = Color(0xFF1A237E),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = Color(0xFF4CAF50)
                        )
                    }
                ) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        text = { Text("Analytics") }
                    )
                    Tab(
                        selected = currentTab == 2,
                        onClick = { viewModel.setTab(2) },
                        text = { Text("Loans") }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.syncBusinessData() },
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                text = { Text("Sync Data") },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F3F4))
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
fun OverviewTab(profile: CreditProfileResponse, history: List<HistoryItem>, lastUpdated: String) {
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
                "Last Updated: $lastUpdated",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsTitle("Cash Flow Distribution")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                val (income, expense) = viewModel.getIncomeExpenseData(history)
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IncomeExpensePieChart(income, expense)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        LegendItem("Inflow", Color(0xFF4CAF50))
                        LegendItem("Outflow", Color(0xFFF44336))
                    }
                }
            }
        }

        item {
            AnalyticsTitle("Activity Frequency (Last 7 Days)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                val activityData = viewModel.getActivityData(history)
                Column(modifier = Modifier.padding(16.dp)) {
                    ActivityBarChart(activityData)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Proof of active business transaction volume", fontSize = 11.sp, color = Color.Gray)
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
fun LoansTab(loans: List<LoanProduct>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Loan Marketplace",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B237E)
            )
            Text("Discover credit products suited for your profile", color = Color.Gray, fontSize = 14.sp)
        }

        if (loans.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Offers Available", fontWeight = FontWeight.Bold)
                        Text(
                            "Improve your BizCredit score by syncing more transaction data to unlock higher loan limits.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
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
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Merchant Health Score", color = Color.White.copy(0.7f), fontSize = 14.sp)
            Text("${profile.score}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Surface(color = riskColor, shape = RoundedCornerShape(8.dp)) {
                Text(
                    "${profile.risk} RISK",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = if (profile.risk == "MEDIUM") Color.Black else Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            val eligibilityText = if (profile.score >= 650) "✅ High Loan Eligibility" else "⚠️ Improve activity to qualify"
            Text(eligibilityText, color = Color.White, fontWeight = FontWeight.Bold)
            Text(profile.summary, textAlign = TextAlign.Center, color = Color.White.copy(0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
fun BusinessMetricsGrid(features: BusinessFeatures?) {
    if (features == null) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox("Monthly Inflow", "₹${features.totalCredit}", Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
            MetricBox("Business Outflow", "₹${features.totalDebit}", Color(0xFFFFEBEE), Color(0xFFC62828), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox("Net Cash Flow", "₹${features.netBalance}", Color(0xFFE3F2FD), Color(0xFF1565C0), Modifier.weight(1f))
            MetricBox("Activity Count", "${features.transactionCount}", Color(0xFFFFF3E0), Color(0xFFE65100), Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor.copy(0.7f))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
    }
}

@Composable
fun LoanCard(loan: LoanProduct) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(loan.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("By ${loan.provider}", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                    Text(loan.amount, Modifier.padding(4.dp), color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loan.link))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
            ) {
                Text("Apply Now")
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
    
    Canvas(modifier = Modifier.size(140.dp)) {
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = incomeSweep,
            useCenter = false,
            style = Stroke(width = 30f)
        )
        drawArc(
            color = Color(0xFFF44336),
            startAngle = -90f + incomeSweep,
            sweepAngle = 360f - incomeSweep,
            useCenter = false,
            style = Stroke(width = 30f)
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
                        .width(16.dp)
                        .height(80.dp * (value / maxVal).coerceIn(0.1f, 1f))
                        .background(Color(0xFF1A237E), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Text(label, fontSize = 7.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
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
        CircularProgressIndicator(color = Color(0xFF1A237E))
        Spacer(Modifier.height(16.dp))
        Text("Running Intelligence Engine...", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnalyticsTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, Modifier.padding(start = 4.dp), fontSize = 11.sp)
    }
}

@Composable
fun TransactionRow(item: HistoryItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(item.merchant, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(
            if (item.type == "credit") "+₹${item.amount}" else "-₹${item.amount}",
            color = if (item.type == "credit") Color(0xFF2E7D32) else Color(0xFFC62828),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun EvaluationInsightsList(insights: CreditInsights?) {
    if (insights == null) return
    Column {
        Text("Evaluation Highlights", fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                BulletPoint("Stability", insights.incomeStrength)
                BulletPoint("Behavior", insights.spendingBehavior)
                BulletPoint("Vitality", insights.activityLevel)
            }
        }
    }
}

@Composable
fun BulletPoint(label: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Text(text, fontSize = 13.sp)
        }
    }
}

@Composable
fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onRetry, Modifier.padding(top = 16.dp)) { Text("Retry Analysis") }
    }
}

@Composable
fun EmptyStateOverlay(onSync: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AI Merchant Analysis Ready", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            "Sync your business transactions to generate a real-time credit score and discovery eligible loan offers.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onSync, Modifier.padding(top = 24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) {
            Text("Initiate Business Sync")
        }
    }
}