const { createClient } = require('@supabase/supabase-js');

// Config
const SUPABASE_URL = 'https://iqimfntocrsjgsfvrcbr.supabase.co';
const SUPABASE_KEY = 'sb_publishable_xj3-9XHFo4FvnI04Jx4vXQ_b0STQD6B';

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

/**
 * Saves a batch of parsed transactions to Supabase.
 * Uses UPSERT with 'reference_number' conflict resolution for idempotency.
 * SQL: ALTER TABLE transactions ADD CONSTRAINT unique_ref UNIQUE (reference_number);
 */
async function saveTransactions(messagesWithParsed) {
    const rows = messagesWithParsed
        .filter(item => item.parsed !== null)
        .map(item => ({
            amount: item.parsed.amount,
            type: item.parsed.type,
            merchant: item.parsed.merchant,
            reference_number: item.parsed.referenceNumber, // NEW FIELD
            date: new Date().toISOString(),
            raw_message: item.raw
        }));

    if (rows.length === 0) return;

    // Use UPSERT on reference_number to handle bank retries vs duplicates
    const { error } = await supabase
        .from('transactions')
        .upsert(rows, { onConflict: 'reference_number', ignoreDuplicates: true });

    if (error) console.error('Error saving transactions (UPSERT ref):', error.message);
}

/**
 * Saves a calculated score and breakdown to Supabase.
 */
async function saveScore(scoreData) {
    const { error } = await supabase
        .from('scores')
        .insert([{
            score: scoreData.score,
            risk: scoreData.risk,
            total_credit: scoreData.features.totalCredit,
            total_debit: scoreData.features.totalDebit,
            transaction_count: scoreData.features.transactionCount,
            breakdown: scoreData.breakdown 
        }]);

    if (error) console.error('Error saving score:', error.message);
}

/**
 * Fetches transaction history and latest score entries.
 */
async function getHistory() {
    const { data: transactions, error: tError } = await supabase
        .from('transactions')
        .select('*')
        .order('date', { ascending: false })
        .limit(50);

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

module.exports = {
    saveTransactions,
    saveScore,
    getHistory
};
