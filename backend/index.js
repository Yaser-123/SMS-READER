const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

// Import modular components
const { parseUPIMessage } = require('./parser');
const { calculateFeatures } = require('./features');
const { calculateScore, classifyRisk, generateInsights, generateSummary } = require('./scoring');
const { saveTransactions, saveScore, getHistory } = require('./database');
const { getEligibleLoans } = require('./loans');

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

        console.log(`\n--- Received ${rawMessages.length} messages ---`);
        
        // 1. Parse incoming messages and FILTER OUT invalid ones (0.0 or non-financial)
        const parsedBatch = rawMessages
            .map(msg => ({
                raw: msg.body,
                sender: msg.sender,
                date: msg.date,
                parsed: parseUPIMessage(msg.body)
            }))
            .filter(item => item.parsed !== null);

        // 2. Save new transactions to Supabase (Async)
        await saveTransactions(parsedBatch);

        // 3. Fetch full history for accurate scoring
        const history = await getHistory();
        const allTransactions = history.transactions;

        // 4. Feature Engineering
        const features = calculateFeatures(allTransactions);

        // 5. Intelligence Engine (Scoring & Insights)
        const score = calculateScore(features);
        const risk = classifyRisk(score);
        const insights = generateInsights(features);
        const summary = generateSummary(features, score);

        // 6. Discovery (Eligible Loans)
        const eligibleLoans = getEligibleLoans(score);

        // 7. Save new score to history (Async)
        const finalResult = { score, risk, features, insights, summary };
        await saveScore(finalResult);

        // 7. Premium API Response
        console.log(`\n>>> Analytics Processed: SCORE ${score} | RISK ${risk}`);
        console.log(`>>> Summary: ${summary}`);

        res.json({
            status: 'success',
            totalMessages: rawMessages.length,
            parsedCount: parsedBatch.filter(p => p.parsed !== null).length,
            score: score,
            risk: risk,
            summary: summary,
            features: features,
            insights: insights,
            topMerchants: features.topMerchants,
            eligibleLoans: eligibleLoans
        });

    } catch (error) {
        console.error('API Error:', error);
        res.status(500).json({ status: 'error', message: 'Internal Server Error' });
    }
});

// History Endpoint: GET /api/history
app.get('/api/history', async (req, res) => {
    try {
        const history = await getHistory();
        res.json(history);
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch history' });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`\n🚀 SMS Fintech Intelligence Engine running on http://localhost:${PORT}`);
    console.log(`API endpoints ready for sync and analytics.`);
});
