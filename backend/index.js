const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

// Import modular components
const { parseStrictTransaction } = require('./parser');
const { calculateFeatures } = require('./features');
const { calculateScore, classifyRisk, generateInsights, generateSummary } = require('./scoring');
const { saveTransactions, saveScore, getHistory, clearAllData, pruneJunk } = require('./database');
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
        
        
        
        // --- STEP 0: SMART PRUNE ---
        // Only wipe NULL/0 junk, NOT the whole history
        await pruneJunk();

        // 1. Parse incoming messages (STRICT VALIDATION PIPELINE)
        const parsedBatch = rawMessages
            .map(msg => ({
                raw: msg.body,
                sender: msg.sender,
                date: msg.date,
                parsed: parseStrictTransaction({
                    body: msg.body,
                    sender: msg.sender,
                    date: msg.date
                })
            }))
            .filter(item => item.parsed !== null);

        // Map to flat transaction objects for DB insertion
        const finalTransactions = parsedBatch.map(item => ({
            ...item.parsed,
            raw_message: item.raw 
        }));

        // 2. Save new transactions with de-duplication
        if (finalTransactions.length > 0) {
            await saveTransactions(finalTransactions);
        }

        // 3. Fetch full history and previous points
        const historyData = await getHistory();
        const allTransactions = historyData.transactions;
        const lastTwoScores = historyData.latestScores;

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
            scoreChange = score - lastTwoScores[0].score;
        }

        // 7. Save new score entry (Breakdown is included but not critical for UI retrieval)
        await saveScore({ score, risk, features, breakdown });

        res.json({
            status: 'success',
            score: score,
            risk: risk,
            scoreChange: scoreChange,
            breakdown: breakdown, // Calculated Fresh
            summary: summary,
            features: features,
            insights: insights,
            eligibleLoans: getLoans(score)
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
        const transactions = historyData.transactions;
        const latestScore = historyData.latestScores.length > 0 ? historyData.latestScores[0] : null;

        let dynamicBreakdown = null;
        if (latestScore) {
            // Re-calculate features and score to get the correct breakdown for the UI
            const features = calculateFeatures(transactions);
            const scoreResult = calculateScore(features);
            dynamicBreakdown = scoreResult.breakdown;
        }

        res.json({
            transactions: transactions,
            latestScore: latestScore ? {
                ...latestScore,
                breakdown: dynamicBreakdown, // Overlay fresh breakdown
                eligibleLoans: getLoans(latestScore.score) // HYDRATE LOANS FOR HISTORY
            } : null,
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
