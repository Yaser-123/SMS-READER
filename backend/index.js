const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

// Import modular components
const { parseUPIMessage } = require('./parser');
const { calculateFeatures } = require('./features');
const { calculateScore, classifyRisk, generateInsights, generateSummary } = require('./scoring');
const { saveTransactions, saveScore, getHistory } = require('./database');
const { getLoans } = require('./loans');

const app = express();
const PORT = 5000;

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Main Endpoint: POST /api/sms
app.post('/api/sms', async (req, res) => {
    try {
        const rawMessages = req.body.messages;
        if (!rawMessages || !Array.isArray(rawMessages)) {
            return res.status(400).json({ status: 'error', message: 'Invalid request body' });
        }
        
        // 1. Parse incoming messages (Strict Filtering)
        const parsedBatch = rawMessages
            .map(msg => ({
                raw: msg.body,
                sender: msg.sender,
                date: msg.date,
                parsed: parseUPIMessage(msg.body)
            }))
            .filter(item => item.parsed !== null);

        // 2. Save new transactions with de-duplication
        await saveTransactions(parsedBatch);

        // 3. Fetch full history and previous points
        const historyData = await getHistory();
        const allTransactions = historyData.transactions;
        const lastTwoScores = historyData.latestScores;

        // 4. Feature Engineering
        const features = calculateFeatures(allTransactions);

        // 5. Intelligence Engine (Explainable Scoring)
        const scoreResult = calculateScore(features);
        const score = scoreResult.total;
        const breakdown = scoreResult.breakdown;
        
        const risk = classifyRisk(score);
        const insights = generateInsights(features);
        const summary = generateSummary(features, score);

        // 6. Trend Analysis (Score Change)
        let scoreChange = 0;
        if (lastTwoScores.length > 0) {
            // Compare current score with the absolute latest entry in DB
            scoreChange = score - lastTwoScores[0].score;
        }

        // 7. Save new score entry
        await saveScore({ score, risk, features, breakdown });

        res.json({
            status: 'success',
            score: score,
            risk: risk,
            scoreChange: scoreChange,
            breakdown: breakdown,
            summary: summary,
            features: features,
            insights: insights,
            eligibleLoans: getLoans(score) // Returns all loans with eligibility flag
        });

    } catch (error) {
        console.error('API Error:', error);
        res.status(500).json({ status: 'error', message: 'Internal Server Error' });
    }
});

// History Endpoint: GET /api/history
app.get('/api/history', async (req, res) => {
    try {
        const historyData = await getHistory();
        res.json({
            transactions: historyData.transactions,
            latestScore: historyData.latestScores.length > 0 ? historyData.latestScores[0] : null,
            // Calculate change if at least 2 entries exist
            scoreChange: historyData.latestScores.length >= 2 ? 
                (historyData.latestScores[0].score - historyData.latestScores[1].score) : 0
        });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch history' });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`\n🚀 SMS Fintech Intelligence Engine running on http://localhost:${PORT}`);
});
