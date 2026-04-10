const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = 'https://iqimfntocrsjgsfvrcbr.supabase.co';
const SUPABASE_KEY = 'sb_publishable_xj3-9XHFo4FvnI04Jx4vXQ_b0STQD6B';
const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

async function pruneJunk() {
    const { error } = await supabase.from('transactions').delete().or('amount.is.null,amount.eq.0');
    if (error) console.error("⚠️ Prune Warning:", error.message);
}

async function clearAllData() {
    try {
        await supabase.from('scores').delete().neq('id', '00000000-0000-0000-0000-000000000000');
        await supabase.from('transactions').delete().neq('id', '00000000-0000-0000-0000-000000000000');
        console.log("✅ Cleanup Complete.");
    } catch (e) {
        console.error("❌ Cleanup Error:", e.message);
    }
}

/**
 * Idempotent save — upsert means calling this 100 times with the same data
 * has exactly the same result as calling it once.
 */
async function saveTransactions(transactions) {
    if (!transactions || transactions.length === 0) return;

    const rows = transactions.map(item => ({
        amount:           item.amount,
        type:             item.type,
        merchant:         item.merchant,
        reference_number: item.reference_number,
        date:             item.date,
        raw_message:      item.raw_message
    }));

    const { error } = await supabase
        .from('transactions')
        .upsert(rows, { onConflict: 'reference_number', ignoreDuplicates: true });

    if (error) console.error('Error saving transactions:', error.message);
}

async function saveScore(scoreData) {
    // NOTE: 'breakdown' is NOT a column in the scores table — it is recalculated dynamically.
    // We only persist the scalar values that exist as real columns.
    const { error } = await supabase
        .from('scores')
        .upsert([{
            score:             scoreData.score,
            risk:              scoreData.risk,
            total_credit:      scoreData.features.totalCredit,
            total_debit:       scoreData.features.totalDebit,
            transaction_count: scoreData.features.transactionCount
        }]);
    if (error) console.error('Error saving score:', error.message);
}

/**
 * Returns the canonical DB state — no limits that could distort the score.
 * 10,000 is a safe ceiling for any business SMS history.
 */
async function getHistory() {
    const { data: transactions, error: tError } = await supabase
        .from('transactions')
        .select('*')
        .order('date', { ascending: false })
        .limit(10000);

    const { data: scores, error: sError } = await supabase
        .from('scores')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(2);

    if (tError || sError) {
        console.error('Error fetching history:', tError?.message || sError?.message);
        return { transactions: [], latestScores: [] };
    }

    return {
        transactions: transactions || [],
        latestScores: scores || []
    };
}

module.exports = { saveTransactions, saveScore, getHistory, clearAllData, pruneJunk };
