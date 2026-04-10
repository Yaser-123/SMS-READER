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
        
        // 1. Cleanup Junk
        await pruneJunk();

        // 2. Parse Incoming Batch
        const parsedBatch = rawMessages
            .map(msg => ({
                raw: msg.body,
                sender: msg.sender,
                date: msg.date,
                parsed: parseStrictTransaction({ body: msg.body, sender: msg.sender, date: msg.date })
            }))
            .filter(item => item.parsed !== null);

        const currentTransactions = parsedBatch.map(item => ({
            ...item.parsed,
            raw_message: item.raw 
        }));

        // 3. PERSIST (Await background save)
        if (currentTransactions.length > 0) {
            await saveTransactions(currentTransactions);
        }

        // 4. IN-MEMORY MERGER
        const historyData = await getHistory();
        const existingHistory = historyData.transactions || [];
        
        const currentRefs = new Set(currentTransactions.map(t => t.reference_number));
        const mergedHistory = [
            ...currentTransactions,
            ...existingHistory.filter(t => !currentRefs.has(t.reference_number))
        ];

        // 5. Calculate Intelligence (Scoring)
        const features = calculateFeatures(mergedHistory);
        const scoreResult = calculateScore(features);
        
        // STRICT INTEGER CASTING FOR ANDROID GSON
        const b = scoreResult.breakdown;
        const breakdown = {
            base: parseInt(b.base) || 300,
            income: parseInt(b.income) || 0,
            activity: parseInt(b.activity) || 0,
            stability: parseInt(b.stability) || 0
        };

        const score = parseInt(scoreResult.total) || 300;
        const risk = classifyRisk(score);
        const insights = generateInsights(features);
        const summary = generateSummary(features, score);

        // Trend
        let scoreChange = 0;
        if (historyData.latestScores.length > 0) {
            scoreChange = score - historyData.latestScores[0].score;
        }

        // 6. Save performance history
        await saveScore({ score, risk, features, breakdown });

        // --- TRUTH LOGS ---
        console.log(`\n📡 Sync Received: ${currentTransactions.length} messages.`);
        console.log(`📊 Merged History: ${mergedHistory.length} unique tx.`);
        console.log(`🎯 Score Calculated: ${score} (${risk})`);
        console.log(`✅ JSON_DEBUG_SENT:`, JSON.stringify({ breakdown, score, risk }));

        // 7. Response
        res.json({
            status: 'success',
            score: score,
            risk: risk,
            scoreChange: scoreChange,
            breakdown: breakdown, 
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
            const features = calculateFeatures(transactions);
            const scoreResult = calculateScore(features);
            const b = scoreResult.breakdown;
            const dynamicBreakdown = {
                base: parseInt(b.base) || 300,
                income: parseInt(b.income) || 0,
                activity: parseInt(b.activity) || 0,
                stability: parseInt(b.stability) || 0
            };
            
            const insights = generateInsights(features);
            const summary = generateSummary(features, latestScore.score);

            res.json({
                transactions: transactions,
                latestScore: {
                    ...latestScore,
                    summary: summary,
                    breakdown: dynamicBreakdown,
                    features: features,
                    insights: insights,
                    eligibleLoans: getLoans(latestScore.score)
                },
                scoreChange: historyData.latestScores.length >= 2 ? 
                    (historyData.latestScores[0].score - historyData.latestScores[1].score) : 0
            });
        } else {
            res.json({ transactions: [], latestScore: null, scoreChange: 0 });
        }
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch history' });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`\n🚀 SMS Fintech Intelligence Engine running on http://localhost:${PORT}`);
});
