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
        
        await pruneJunk();

        // 1. Parsing and Early Deduplication (The "900-Score" Fix)
        // We filter by reference_number IMMEDIATELY to prevent double-counting in memory
        const uniqueEntriesMap = new Map();
        
        rawMessages.forEach(msg => {
            const parsed = parseStrictTransaction({ body: msg.body, sender: msg.sender, date: msg.date });
            if (parsed && !uniqueEntriesMap.has(parsed.reference_number)) {
                uniqueEntriesMap.set(parsed.reference_number, {
                    ...parsed,
                    raw_message: msg.body
                });
            }
        });

        const currentTransactions = Array.from(uniqueEntriesMap.values());

        // 2. Persist to DB
        if (currentTransactions.length > 0) {
            await saveTransactions(currentTransactions);
        }

        // 3. Merging with Database History
        const historyData = await getHistory();
        const existingHistory = historyData.transactions || [];
        
        // Final deduplicated list for calculation
        const masterRefMap = new Map();
        [...existingHistory, ...currentTransactions].forEach(tx => {
            masterRefMap.set(tx.reference_number, tx);
        });
        const mergedHistory = Array.from(masterRefMap.values());

        // 4. Calculate Intelligence
        const features = calculateFeatures(mergedHistory);
        const scoreResult = calculateScore(features);
        
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

        let scoreChange = 0;
        if (historyData.latestScores.length > 0) {
            scoreChange = score - historyData.latestScores[0].score;
        }

        // 5. Save performance history
        await saveScore({ score, risk, features, breakdown });

        // --- PRODUCTION LOGS (As requested) ---
        console.log(`\n--- Received ${rawMessages.length} messages ---`);
        console.log(`>>> Analytics Processed: SCORE ${score} | RISK ${risk}`);
        console.log(`>>> Summary: ${summary}`);
        console.log(`✅ JSON_DEBUG_SENT:`, JSON.stringify({ breakdown, score, risk }));

        // 6. Response
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

        if (latestScore) {
            const features = calculateFeatures(transactions);
            const scoreResult = calculateScore(features);
            const b = scoreResult.breakdown;
            
            // FIXED: Variable shadowing prevented
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

app.listen(PORT, () => {
    console.log(`\n🚀 SMS Fintech Intelligence Engine running on http://localhost:${PORT}`);
});
