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
        
        // --- STEP 1: CLEANUP ---
        // Purge NULL/0 junk before processing
        await pruneJunk();

        // --- STEP 2: PARSE & FILTER ---
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

        console.log(`📡 Sync Received: ${parsedBatch.length} BOI transactions found.`);

        const finalTransactions = parsedBatch.map(item => ({
            ...item.parsed,
            raw_message: item.raw 
        }));

        // --- STEP 3: PERSIST DATA (CRITICAL: Save before scoring) ---
        if (finalTransactions.length > 0) {
            await saveTransactions(finalTransactions);
        }

        // --- STEP 4: FETCH FULL HISTORY (Freshly updated) ---
        const historyData = await getHistory();
        const allTransactions = historyData.transactions;
        const lastTwoScores = historyData.latestScores;

        console.log(`📊 Analysis Engine: Processing history of ${allTransactions.length} records.`);

        const features = calculateFeatures(allTransactions);

        // --- STEP 5: INTELLIGENCE ENGINE (Explainable Scoring) ---
        const scoreResult = calculateScore(features);
        const score = scoreResult.total;
        const breakdown = scoreResult.breakdown;
        
        const risk = classifyRisk(score);
        const insights = generateInsights(features);
        const summary = generateSummary(features, score);

        // Trend Analysis
        let scoreChange = 0;
        if (lastTwoScores.length > 0) {
            scoreChange = score - lastTwoScores[0].score;
        }

        // --- STEP 6: SAVE FINAL PERFORMANCE ---
        await saveScore({ score, risk, features, breakdown });

        // --- STEP 7: RESPONSE (Fully Hydrated) ---
        res.json({
            status: 'success',
            score: score,
            risk: risk,
            scoreChange: scoreChange,
            breakdown: breakdown, // Calculated from fresh history!
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
        let dynamicLoans = [];

        if (latestScore) {
            // Recalculate features and score to ensure breakdown parity
            const features = calculateFeatures(transactions);
            const scoreResult = calculateScore(features);
            dynamicBreakdown = scoreResult.breakdown;
            dynamicLoans = getLoans(latestScore.score);
        }

        res.json({
            transactions: transactions,
            latestScore: latestScore ? {
                ...latestScore,
                breakdown: dynamicBreakdown,
                eligibleLoans: dynamicLoans
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
