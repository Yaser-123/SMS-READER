const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

const { parseStrictTransaction } = require('./parser');
const { calculateFeatures } = require('./features');
const { calculateScore, classifyRisk, generateInsights, generateSummary } = require('./scoring');
const { saveTransactions, saveScore, getHistory, clearAllData, pruneJunk } = require('./database');
const { getLoans } = require('./loans');

const app = express();
const PORT = 5000;

app.use(cors());
app.use(bodyParser.json());

// ============================================================
// POST /api/sms  — The Stateless Score Engine
// ============================================================
// FIX: We NEVER merge the incoming batch with DB history for scoring.
//      We SAVE first, then SCORE from the canonical DB state only.
//      This guarantees the same 37 messages always produce the same score.
// ============================================================
app.post('/api/sms', async (req, res) => {
    try {
        const rawMessages = req.body.messages;
        if (!rawMessages || !Array.isArray(rawMessages)) {
            return res.status(400).json({ status: 'error', message: 'Invalid request body' });
        }

        // STEP 1 — Parse & deduplicate the INCOMING batch in memory only
        const uniqueEntriesMap = new Map();
        rawMessages.forEach(msg => {
            const parsed = parseStrictTransaction({ body: msg.body, sender: msg.sender, date: msg.date });
            if (parsed && !uniqueEntriesMap.has(parsed.reference_number)) {
                uniqueEntriesMap.set(parsed.reference_number, { ...parsed, raw_message: msg.body });
            }
        });
        const currentTransactions = Array.from(uniqueEntriesMap.values());

        // STEP 2 — Persist to DB (upsert is idempotent — safe to call repeatedly)
        if (currentTransactions.length > 0) {
            await saveTransactions(currentTransactions);
        }

        // STEP 3 — Fetch the CANONICAL DB state (the single source of truth)
        //   We do NOT merge with currentTransactions here. The DB is always right.
        const historyData = await getHistory();
        const canonicalTransactions = historyData.transactions || [];

        // STEP 4 — Score from canonical data only (deterministic)
        const features = calculateFeatures(canonicalTransactions);
        const scoreResult = calculateScore(features);

        const breakdown = {
            base:      parseInt(scoreResult.breakdown.base)      || 300,
            income:    parseInt(scoreResult.breakdown.income)    || 0,
            activity:  parseInt(scoreResult.breakdown.activity)  || 0,
            stability: parseInt(scoreResult.breakdown.stability) || 0
        };
        const score = parseInt(scoreResult.total) || 300;
        const risk = classifyRisk(score);
        const insights = generateInsights(features);
        const summary = generateSummary(features, score);

        let scoreChange = 0;
        if (historyData.latestScores.length > 0) {
            scoreChange = score - historyData.latestScores[0].score;
        }

        // STEP 5 — Persist the score
        await saveScore({ score, risk, features, breakdown });

        console.log(`\n--- Received ${rawMessages.length} messages ---`);
        console.log(`>>> Analytics Processed: SCORE ${score} | RISK ${risk}`);
        console.log(`>>> Breakdown: Base=${breakdown.base} Income=${breakdown.income} Activity=${breakdown.activity} Stability=${breakdown.stability}`);
        console.log(`>>> DB Canonical Count: ${canonicalTransactions.length} unique transactions`);

        res.json({
            status: 'success',
            score,
            risk,
            scoreChange,
            breakdown,
            summary,
            features,
            insights,
            eligibleLoans: getLoans(score)
        });

    } catch (error) {
        console.error('API Error:', error);
        res.status(500).json({ status: 'error', message: 'Internal Server Error' });
    }
});

// ============================================================
// GET /api/history  — Full intelligence hydration
// ============================================================
app.get('/api/history', async (req, res) => {
    try {
        const historyData = await getHistory();
        const transactions = historyData.transactions || [];
        const latestScore  = historyData.latestScores.length > 0 ? historyData.latestScores[0] : null;

        if (!latestScore) {
            return res.json({ transactions: [], latestScore: null, scoreChange: 0 });
        }

        // Recalculate breakdown from canonical DB state
        const features = calculateFeatures(transactions);
        const scoreResult = calculateScore(features);
        const breakdown = {
            base:      parseInt(scoreResult.breakdown.base)      || 300,
            income:    parseInt(scoreResult.breakdown.income)    || 0,
            activity:  parseInt(scoreResult.breakdown.activity)  || 0,
            stability: parseInt(scoreResult.breakdown.stability) || 0
        };
        const insights = generateInsights(features);
        const summary  = generateSummary(features, latestScore.score);

        res.json({
            transactions,
            latestScore: {
                ...latestScore,
                summary,
                breakdown,
                features,
                insights,
                eligibleLoans: getLoans(latestScore.score)
            },
            scoreChange: historyData.latestScores.length >= 2
                ? (historyData.latestScores[0].score - historyData.latestScores[1].score)
                : 0
        });
    } catch (error) {
        console.error('History Error:', error);
        res.status(500).json({ status: 'error', message: 'Failed to fetch history' });
    }
});

app.listen(PORT, () => {
    console.log(`\n🚀 SMS Fintech Intelligence Engine running on http://localhost:${PORT}`);
});
