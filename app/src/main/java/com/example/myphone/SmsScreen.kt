package com.example.myphone


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SmsScreen(viewModel: SmsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF200122),
            Color(0xFF0B2E3D)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Financial SMS Sync",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            color = Color.White
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Fetched: ${uiState.smsList.size} messages",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { viewModel.syncMessages() },
                enabled = !uiState.isSyncing && uiState.smsList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94057))
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sync Messages")
                }
            }
        }

        if (uiState.syncStatus.isNotEmpty()) {
            Text(
                text = uiState.syncStatus,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.syncStatus.startsWith("Success")) Color.Green else Color.Yellow,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.smsList.isEmpty()) {
            Text(
                text = "No financial messages found with the set keywords.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.smsList) { sms ->
                    SmsItem(sms = sms)
                }
            }
        }
    }
}

@Composable
fun SmsItem(sms: Sms) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B2E3D),
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = sms.sender,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = sms.date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.5f))

            Text(
                text = sms.body,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}